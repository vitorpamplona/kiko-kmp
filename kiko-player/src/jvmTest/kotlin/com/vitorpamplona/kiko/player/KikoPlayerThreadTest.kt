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
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
