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

import com.vitorpamplona.kiko.runtime.FantasyConsole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A [KikoPlayer] as apps make it, on its own thread and the real clock: what virtual time cannot
 * show, a frame that never returns.
 */
class KikoPlayerThreadTest {
    private val players = ArrayList<KikoPlayer>()
    private val log = CopyOnWriteArrayList<String>()

    @AfterTest
    fun closeAll() = players.forEach { it.close() }

    private fun player(code: String) =
        KikoPlayer(
            source = { "__lua__\n$code\n".encodeToByteArray() },
            host =
                object : KikoPlayerHost {
                    override fun log(message: String) {
                        log.add(message)
                    }
                },
            openAudio = { null },
        ).also { players.add(it) }

    @Test
    fun aGamePlaysOnItsOwnThread() =
        runBlocking {
            val player = player("function _draw() cls(8) end")
            player.resume()
            withTimeout(10.seconds) { player.frameNumber.first { it > 2 } }
            val pixels = IntArray(FantasyConsole.SCREEN_SIZE * FantasyConsole.SCREEN_SIZE)
            player.copyFrame(pixels)
            assertEquals(0xFFFF004D.toInt(), pixels[0])
        }

    @Test
    fun theWatchdogStopsAFrameThatNeverEnds() =
        runBlocking {
            val player = player("function _draw() while true do end end")
            player.maxFrame = 1.seconds
            player.resume()
            val status = withTimeout(10.seconds) { player.status.first { it is KikoPlayer.Status.Stopped } }
            assertIs<KikoPlayer.Status.Stopped>(status)
            assertEquals("the game stopped responding", status.error)
        }

    @Test
    fun pausingDuringASlowFrameDoesNotEndTheGame() =
        runBlocking {
            // A frame of _draw that takes a couple of seconds (about 2 s on a laptop; the console
            // never cuts a frame inside _draw), well under the watchdog's limit. The app goes to
            // the background in the middle of it.
            val player =
                player(
                    """
                    n=0
                    function _draw() n+=1 printh("frame "..n) if n==1 then for i=1,30000 do for j=1,2000 do end end end end
                    """.trimIndent(),
                )
            player.resume()
            withTimeout(10.seconds) { while ("frame 1" !in log) delay(10.milliseconds) }
            delay(1200.milliseconds)
            player.pause()
            player.resume()
            withTimeout(30.seconds) { while ("frame 3" !in log) delay(20.milliseconds) }
            assertEquals(KikoPlayer.Status.Playing, player.status.value)
        }

    /** A weak reference to the game [player] runs now: what its cart built stays reachable through it. */
    private fun weakGame(player: KikoPlayer) = WeakReference(player.console!!)

    private fun collected(ref: WeakReference<*>): Boolean {
        repeat(50) {
            System.gc()
            if (ref.get() == null) return true
            Thread.sleep(100)
        }
        return false
    }

    // A table of 50,000 tables, which nothing outside the game holds.
    private val bigTable = "big={} for i=1,50000 do big[i]={i} end "

    @Test
    fun aGameThatStoppedLetsGoOfItsMemoryButKeepsItsLastFrame() =
        runBlocking<Unit> {
            val loads = AtomicInteger()
            val code = bigTable + "f=0 function _update() f+=1 if f==30 then stop() end end function _draw() cls(8) end"
            val player =
                KikoPlayer(
                    source = {
                        loads.incrementAndGet()
                        "__lua__\n$code\n".encodeToByteArray()
                    },
                    openAudio = { null },
                ).also { players.add(it) }
            player.resume()
            withTimeout(10.seconds) { player.frameNumber.first { it > 0 } }
            val game = weakGame(player)
            withTimeout(10.seconds) { player.status.first { it is KikoPlayer.Status.Stopped } }
            assertTrue(collected(game), "the stopped game is still reachable")
            val pixels = IntArray(FantasyConsole.SCREEN_SIZE * FantasyConsole.SCREEN_SIZE)
            assertTrue(player.copyFrame(pixels) > 0)
            assertEquals(0xFFFF004D.toInt(), pixels[0])
            // Resuming a game that ended neither restarts nor reloads it.
            player.resume()
            delay(200.milliseconds)
            assertEquals(1, loads.get())
            assertIs<KikoPlayer.Status.Stopped>(player.status.value)
        }

    @Test
    fun closeLetsGoOfTheGameButKeepsItsLastFrame() =
        runBlocking {
            val player = player(bigTable + "function _draw() cls(8) end")
            player.resume()
            withTimeout(10.seconds) { player.frameNumber.first { it > 2 } }
            val game = weakGame(player)
            player.close()
            assertTrue(collected(game), "the closed game is still reachable")
            val pixels = IntArray(FantasyConsole.SCREEN_SIZE * FantasyConsole.SCREEN_SIZE)
            assertTrue(player.copyFrame(pixels) > 0)
            assertEquals(0xFFFF004D.toInt(), pixels[0])
        }

    @Test
    fun closeStopsAGameStuckInAFrame() =
        runBlocking {
            val player = player("function _draw() printh('in') while true do end end")
            player.resume()
            withTimeout(10.seconds) { while ("in" !in log) delay(10.milliseconds) }
            player.close()
            // The console was told to stop: the frame ends and the status leaves Playing.
            withTimeout(10.seconds) { player.status.first { it is KikoPlayer.Status.Stopped } }
            Unit
        }
}
