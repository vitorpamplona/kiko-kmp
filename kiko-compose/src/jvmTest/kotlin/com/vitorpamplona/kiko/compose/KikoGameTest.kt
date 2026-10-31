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
package com.vitorpamplona.kiko.compose

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import com.vitorpamplona.kiko.player.KikoPlayer
import com.vitorpamplona.kiko.player.KikoPlayerHost
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [KikoGame] end to end in an off-screen scene: the one-call overload loads a cart, the player
 * runs it on its own thread, and its frames reach the screen. Density 1, so dp == px.
 */
class KikoGameTest {
    private lateinit var scene: ImageComposeScene
    private var timeMs = 0L
    private var closed = false

    @AfterTest
    fun close() {
        if (::scene.isInitialized && !closed) scene.close()
    }

    private fun render(): Image {
        timeMs += 16
        return scene.render(timeMs * 1_000_000)
    }

    /** Renders until [done] holds for the scene's image, for up to 10 s of real time. */
    private fun renderUntil(done: (Image) -> Boolean): Image {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (true) {
            val image = render()
            if (done(image) || System.nanoTime() > deadline) return image
            Thread.sleep(20)
        }
    }

    private fun Image.argbAt(
        x: Int,
        y: Int,
    ): Int {
        val pixels = Bitmap.makeFromImage(this)
        return pixels.getColor(x, y)
    }

    private fun cart(code: String) = "__lua__\n$code\n".encodeToByteArray()

    @Test
    fun aGameDrawsOnTheScreen() {
        scene =
            ImageComposeScene(width = 512, height = 512, density = Density(1f)) {
                KikoGame(source = { cart("function _draw() cls(8) end") }, showTouchControls = false)
            }
        // 512 is four times 128: the screen fills the scene, each game pixel a 4x4 block.
        val image = renderUntil { it.argbAt(256, 256) == RED }
        assertEquals(RED, image.argbAt(256, 256))
        assertEquals(RED, image.argbAt(2, 2))
    }

    @Test
    fun bytesThatAreNotACartFail() {
        var player: KikoPlayer? = null
        scene =
            ImageComposeScene(width = 300, height = 500, density = Density(1f)) {
                player = rememberKikoPlayer(key = Unit) { "not a cart".encodeToByteArray() }.also { KikoGame(it) }
            }
        renderUntil { player?.status?.value == KikoPlayer.Status.Failed(KikoPlayer.Reason.NOT_A_CART) }
        assertEquals(KikoPlayer.Status.Failed(KikoPlayer.Reason.NOT_A_CART), player?.status?.value)
    }

    @Test
    fun leavingTheCompositionStopsTheGame() {
        var player: KikoPlayer? = null
        scene =
            ImageComposeScene(width = 300, height = 500, density = Density(1f)) {
                player = rememberKikoPlayer(key = Unit) { cart("function _draw() cls(8) end") }.also { KikoGame(it) }
            }
        renderUntil { (player?.frameNumber?.value ?: 0) > 3 }
        scene.close()
        closed = true
        val last = player?.frameNumber?.value
        Thread.sleep(300)
        assertEquals(last, player?.frameNumber?.value)
    }

    // Building a KeyEvent by hand is compose-ui's internal API; fine for a test of the same version.
    @OptIn(InternalComposeUiApi::class)
    @Test
    fun theBuildingBlocksPlayInALayoutOfTheirOwn() {
        val log = CopyOnWriteArrayList<String>()
        var player: KikoPlayer? = null
        scene =
            ImageComposeScene(width = 512, height = 512, density = Density(1f)) {
                val host =
                    remember {
                        object : KikoPlayerHost {
                            override fun log(message: String) {
                                log.add(message)
                            }
                        }
                    }
                val game =
                    rememberKikoPlayer(key = Unit, host = host) {
                        cart("function _update() if (btn(4)) printh('o') end function _draw() cls(8) end")
                    }
                player = game
                DisposableEffect(game) {
                    game.resume()
                    onDispose { game.pause() }
                }
                val input = rememberKikoInput(game)
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                Box(
                    Modifier
                        .fillMaxSize()
                        .focusRequester(focus)
                        .kikoKeys(input, onBack = {})
                        .focusable(),
                ) {
                    KikoScreen(game, Modifier.fillMaxSize())
                    KikoStatus(game, Modifier.fillMaxSize())
                }
            }
        val image = renderUntil { it.argbAt(256, 256) == RED }
        assertEquals(RED, image.argbAt(256, 256))
        scene.sendKeyEvent(KeyEvent(Key.Z, KeyEventType.KeyDown))
        renderUntil { "o" in log }
        assertTrue("o" in log, "$log")
    }

    // Building a KeyEvent by hand is compose-ui's internal API; fine for a test of the same version.
    @OptIn(InternalComposeUiApi::class)
    @Test
    fun aClickOnTheGameTakesTheKeyboardBack() {
        val log = CopyOnWriteArrayList<String>()
        var player: KikoPlayer? = null
        val other = FocusRequester()
        scene =
            ImageComposeScene(width = 512, height = 512, density = Density(1f)) {
                val host =
                    remember {
                        object : KikoPlayerHost {
                            override fun log(message: String) {
                                log.add(message)
                            }
                        }
                    }
                val game =
                    rememberKikoPlayer(key = Unit, host = host) {
                        cart("function _update() if (btn(4)) printh('o') end function _draw() cls(8) end")
                    }
                player = game
                Box(Modifier.fillMaxSize()) {
                    KikoGame(game, Modifier.fillMaxSize(), showTouchControls = false)
                    // Something else that can hold the keyboard, as a menu over the game would.
                    Box(Modifier.focusRequester(other).focusable())
                }
            }
        renderUntil { player?.status?.value == KikoPlayer.Status.Playing }
        other.requestFocus()
        render()
        scene.sendKeyEvent(KeyEvent(Key.Z, KeyEventType.KeyDown))
        scene.sendKeyEvent(KeyEvent(Key.Z, KeyEventType.KeyUp))
        // Several game frames, any of which would have printed had the key reached the game.
        repeat(10) {
            render()
            Thread.sleep(30)
        }
        assertTrue("o" !in log, "the game had the keyboard while something else held it: $log")

        scene.sendPointerEvent(PointerEventType.Press, Offset(256f, 256f))
        scene.sendPointerEvent(PointerEventType.Release, Offset(256f, 256f))
        render()
        scene.sendKeyEvent(KeyEvent(Key.Z, KeyEventType.KeyDown))
        renderUntil { "o" in log }
        assertTrue("o" in log, "$log")
    }

    @Test
    fun theScreenFollowsTheGame() {
        scene =
            ImageComposeScene(width = 512, height = 512, density = Density(1f)) {
                KikoGame(
                    source = { cart("t=0 function _update() t+=1 end function _draw() cls(t<20 and 8 or 12) end") },
                    showTouchControls = false,
                )
            }
        assertEquals(RED, renderUntil { it.argbAt(256, 256) == RED }.argbAt(256, 256))
        // Color 12 is blue: a later frame reaches the same screen.
        assertEquals(BLUE, renderUntil { it.argbAt(256, 256) == BLUE }.argbAt(256, 256))
    }

    @Test
    fun theTouchControlsFollowANewGame() {
        val log = CopyOnWriteArrayList<String>()
        val host =
            object : KikoPlayerHost {
                override fun log(message: String) {
                    log.add(message)
                }
            }
        var game by mutableStateOf("first")
        var player: KikoPlayer? = null
        scene =
            ImageComposeScene(width = 400, height = 700, density = Density(1f)) {
                val current = game
                player =
                    rememberKikoPlayer(key = current, host = host) {
                        cart("function _update() if (btn(4)) printh('$current o') end function _draw() end")
                    }.also { KikoGame(it, requestFocus = false) }
            }
        renderUntil { player?.status?.value == KikoPlayer.Status.Playing }
        val first = player
        game = "second"
        renderUntil { player !== first && player?.status?.value == KikoPlayer.Status.Playing }
        // 400x700, a phone: the O/X area is 160 dp square, 24 dp in from the right and above the
        // bottom (x 216..376, y 516..676), and O sits low on its left, its radius 0.225 of 160.
        val o = Offset(216f + 36f, 516f + 160f - 36f)
        scene.sendPointerEvent(PointerEventType.Press, o, type = PointerType.Touch)
        renderUntil { "second o" in log }
        scene.sendPointerEvent(PointerEventType.Release, o, type = PointerType.Touch)
        assertTrue("second o" in log, "$log")
    }

    // Building a KeyEvent by hand is compose-ui's internal API; fine for a test of the same version.
    @OptIn(InternalComposeUiApi::class)
    @Test
    fun aKeyHeldWhenTheGameLosesTheKeyboardIsLetGo() {
        val log = CopyOnWriteArrayList<String>()
        var player: KikoPlayer? = null
        val other = FocusRequester()
        scene =
            ImageComposeScene(width = 512, height = 512, density = Density(1f)) {
                val host =
                    remember {
                        object : KikoPlayerHost {
                            override fun log(message: String) {
                                log.add(message)
                            }
                        }
                    }
                val game =
                    rememberKikoPlayer(key = Unit, host = host) {
                        cart("function _update() printh(btn(1) and 'right' or 'still') end function _draw() end")
                    }
                player = game
                Box(Modifier.fillMaxSize()) {
                    KikoGame(game, Modifier.fillMaxSize(), showTouchControls = false)
                    Box(Modifier.focusRequester(other).focusable())
                }
            }
        renderUntil { player?.status?.value == KikoPlayer.Status.Playing }
        scene.sendKeyEvent(KeyEvent(Key.DirectionRight, KeyEventType.KeyDown))
        renderUntil { log.lastOrNull() == "right" }
        // Something else takes the keyboard while the key is down; its KeyUp goes there.
        other.requestFocus()
        render()
        log.clear()
        renderUntil { log.size > 5 && log.last() == "still" }
        assertEquals("still", log.last(), "${log.takeLast(5)}")
    }

    private companion object {
        const val RED = 0xFFFF004D.toInt()
        const val BLUE = 0xFF29ADFF.toInt()
    }
}
