/*
 * Copyright (c) 2025 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.kiko.player

import com.vitorpamplona.kiko.cart.CartLoader
import com.vitorpamplona.kiko.cart.P8scii
import com.vitorpamplona.kiko.runtime.Buttons
import com.vitorpamplona.kiko.runtime.ConsoleHost
import com.vitorpamplona.kiko.runtime.FantasyConsole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Loads and plays one cart in real time. The game runs on a thread of its own at its frame
 * rate, publishing each [frame] and playing its sound; the app sends the held buttons with
 * [setButtons], and the pointer and typed text for games that read them, from any thread. Input
 * reaches the game on its thread, between frames.
 *
 * Nothing runs until [resume]. Call [pause] when the player leaves the screen or the app goes to
 * the background, and [close] when it is gone for good. A paused player keeps its game, so a
 * player held across a screen rotation picks up where it was.
 *
 * [source] supplies the cart's bytes (`.p8.png`, `.p8` or `.p8.rom`), or null when there are none:
 * the player never touches the network or the file system itself. What the game saves
 * (`cartdata`) goes to [saves], or stays in memory while this player lives when there is none.
 * [host] answers the rest of what a cart may ask (`printh`, the time zone, other carts); its
 * saves and memory answers are the player's. [openAudio] opens the sound output at a sample rate,
 * or returns null to play silently.
 */
public class KikoPlayer(
    private val source: suspend () -> ByteArray?,
    saves: KikoSaveStore? = null,
    private val host: ConsoleHost = object : ConsoleHost {},
    private val openAudio: (sampleRate: Int) -> KikoAudioSink? = ::openPlatformAudioSink,
) : AutoCloseable {
    public sealed interface Status {
        public data object Loading : Status

        /** [source] gave nothing, or the bytes are not a cart that loads. */
        public data object NotACart : Status

        public data object Playing : Status

        /** The game ended on its own, or stopped on an [error] (already drawn on its screen). */
        public data class Stopped(
            val error: String?,
        ) : Status
    }

    /**
     * One finished frame: [FantasyConsole.SCREEN_SIZE] squared ARGB [pixels], numbered so each
     * is a new value. The array is reused two frames later: copy it to keep it.
     */
    public class Frame(
        public val pixels: IntArray,
        public val number: Long,
    )

    private val _status = MutableStateFlow<Status>(Status.Loading)
    public val status: StateFlow<Status> = _status.asStateFlow()

    private val _frame = MutableStateFlow<Frame?>(null)
    public val frame: StateFlow<Frame?> = _frame.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob())

    @Volatile
    private var console: FantasyConsole? = null

    // Three buffers: the game draws into one while the screen may still be reading the last two.
    private val buffers = Array(3) { IntArray(FantasyConsole.SCREEN_SIZE * FantasyConsole.SCREEN_SIZE) }
    private var nextBuffer = 0

    private val gameThread = lazy { newGameThread() }
    private var loop: Job? = null

    @Volatile
    private var closed = false

    // When the frame running now started, for the watchdog: a cart can loop for ever in `_draw`
    // or in a menu callback, where the console never ends the frame on its own.
    @Volatile
    private var stepStarted: TimeMark? = null

    @Volatile
    private var hung = false

    @Volatile
    private var menuOpen = false

    @Volatile
    private var buttons = 0

    private val saveBuffer = SaveBuffer(saves)

    /** How long one frame may run before the watchdog stops the game. */
    internal var maxFrame: Duration = 10.seconds

    private val consoleHost =
        object : ConsoleHost {
            override fun log(message: String) = host.log(message)

            override fun loadCartData(id: String): IntArray? = saveBuffer.get(id)

            override fun saveCartData(
                id: String,
                values: IntArray,
            ) = saveBuffer.save(id, values)

            override fun utcOffsetSeconds(): Int = host.utcOffsetSeconds()

            override fun loadCart(name: String): ByteArray? = host.loadCart(name)

            override fun memoryLow(): Boolean = isMemoryLow() || host.memoryLow()
        }

    /** The buttons held now, as [Buttons] bits. */
    public fun setButtons(mask: Int) {
        buttons = mask
        onGameThread { it.setButtons(0, mask) }
    }

    /** A tap of the pause button, which opens the game's pause menu (or closes it). */
    public fun tapPause() {
        val held = buttons
        onGameThread {
            it.setButtons(0, held or Buttons.PAUSE)
            it.setButtons(0, held)
        }
    }

    /**
     * What Back should do while a game plays: open its pause menu, so a stray swipe never ends a
     * game. Returns false when there is nothing to pause, or the menu is open already: the app
     * should then close the player.
     */
    public fun openPauseMenu(): Boolean {
        if (_status.value != Status.Playing || menuOpen) return false
        // Until the next frame says so: a quick second Back must close, not toggle the menu shut.
        menuOpen = true
        tapPause()
        return true
    }

    /** The pointer over the screen, in game pixels (0..127), and its buttons (1 left, 2 right, 4 middle). */
    public fun setPointer(
        x: Int,
        y: Int,
        pointerButtons: Int,
    ): Unit = onGameThread { it.setPointer(x, y, pointerButtons) }

    /** Mouse wheel steps, up positive. */
    public fun scroll(steps: Int): Unit = onGameThread { it.scroll(steps) }

    /** Typed text, for games that read the keyboard: Enter is `"\r"` and Backspace `"\b"`. */
    public fun type(text: String): Unit = onGameThread { it.type(P8scii.fromUnicode(text)) }

    private fun onGameThread(action: (FantasyConsole) -> Unit) {
        val game = console ?: return
        if (closed) return
        scope.launch(gameThread.value.dispatcher) { action(game) }
    }

    /** Loads the cart the first time, then runs it until [pause] or [close]. Call from one thread. */
    public fun resume() {
        if (closed || loop?.isActive == true) return
        loop =
            scope.launch(gameThread.value.dispatcher) {
                val game = console ?: load() ?: return@launch
                play(game)
            }
    }

    /** Stops the game between frames, keeping it to [resume] later, and writes its saves. */
    public fun pause() {
        // Cancelling cannot interrupt a frame that never ends; stop such a game instead of
        // leaving its thread spinning behind a closed player.
        if (isStuck(PAUSE_STUCK)) stopHungGame()
        loop?.cancel()
        loop = null
        saveBuffer.flush()
    }

    /** Stops the game for good, writes its saves and frees its thread. */
    override fun close() {
        if (closed) return
        closed = true
        console?.abort()
        loop?.cancel()
        loop = null
        // The last saves are written outside [scope], which is going away.
        saveBuffer.flush()
        scope.cancel()
        if (gameThread.isInitialized()) gameThread.value.close()
    }

    private fun isStuck(after: Duration) = (stepStarted?.elapsedNow() ?: Duration.ZERO) > after

    private fun stopHungGame() {
        hung = true
        console?.abort()
    }

    private suspend fun load(): FantasyConsole? {
        val bytes =
            try {
                source()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        if (bytes == null) {
            _status.value = Status.NotACart
            return null
        }
        saveBuffer.load()
        return try {
            FantasyConsole(CartLoader.load(bytes), consoleHost).also {
                // Input sent while loading had no game to reach yet: what is held still counts.
                it.setButtons(0, buttons)
                console = it
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // A hostile file must never take the app down: anything that fails to load, even
            // with an Error, is simply not a cart.
            host.log("Could not load the cart: $e")
            _status.value = Status.NotACart
            null
        }
    }

    private suspend fun play(game: FantasyConsole) {
        if (game.state != FantasyConsole.State.RUNNING) {
            publishFrame(game)
            _status.value = Status.Stopped(if (hung) NOT_RESPONDING else game.error)
            return
        }
        _status.value = Status.Playing
        val sound = openAudio(FantasyConsole.SAMPLE_RATE)
        // On the web there is one thread: the watchdog only runs between frames there.
        val watchdog =
            scope.launch(Dispatchers.Default) {
                while (isActive) {
                    delay(WATCHDOG_INTERVAL)
                    if (isStuck(maxFrame)) stopHungGame()
                }
            }
        try {
            var deadline = TimeSource.Monotonic.markNow()
            while (currentCoroutineContext().isActive) {
                stepStarted = TimeSource.Monotonic.markNow()
                try {
                    game.step()
                } finally {
                    stepStarted = null
                }
                menuOpen = game.isPaused
                publishFrame(game)
                if (game.state != FantasyConsole.State.RUNNING) {
                    _status.value = Status.Stopped(if (hung) NOT_RESPONDING else game.error)
                    break
                }
                sound?.write(game.audioBuffer, game.audioSamples)
                deadline += 1.seconds / game.fps
                val wait = -deadline.elapsedNow()
                if (wait.isPositive()) {
                    delay(wait)
                } else {
                    // Behind: still let the input queued for this thread in before the next frame.
                    yield()
                    // Far behind (the app was busy): drop the backlog instead of racing to catch up.
                    if (wait < -MAX_LAG) deadline = TimeSource.Monotonic.markNow()
                }
            }
        } finally {
            watchdog.cancel()
            sound?.close()
        }
    }

    private fun publishFrame(game: FantasyConsole) {
        val buffer = buffers[nextBuffer]
        nextBuffer = (nextBuffer + 1) % buffers.size
        game.render(buffer)
        _frame.value = Frame(buffer, game.frameCount)
    }

    private companion object {
        val MAX_LAG = 250.milliseconds
        val PAUSE_STUCK = 1.seconds
        val WATCHDOG_INTERVAL = 1.seconds
        const val NOT_RESPONDING = "the game stopped responding"
    }
}
