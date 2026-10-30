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

import com.vitorpamplona.kiko.runtime.Buttons
import com.vitorpamplona.kiko.runtime.ConsoleHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** A [KikoPlayer] on its own thread, at real speed, with carts written in `.p8` text. */
class KikoPlayerTest {
    private val players = ArrayList<KikoPlayer>()
    private val log = CopyOnWriteArrayList<String>()

    private val host =
        object : ConsoleHost {
            override fun log(message: String) {
                log.add(message)
            }
        }

    @AfterTest
    fun closeAll() = players.forEach { it.close() }

    private fun player(
        code: String?,
        saves: KikoSaveStore? = null,
    ) = KikoPlayer(
        source = { code?.let { "__lua__\n${it.trimIndent()}\n".encodeToByteArray() } },
        saves = saves,
        host = host,
        openAudio = { null },
    ).also { players.add(it) }

    private fun awaitLog(line: String) =
        runBlocking {
            withTimeout(10.seconds) {
                while (line !in log) delay(10.milliseconds)
            }
        }

    @Test
    fun aGamePublishesFramesOfWhatItDraws() =
        runBlocking {
            val player = player("function _draw() cls(8) end")
            player.resume()
            withTimeout(10.seconds) { player.status.first { it == KikoPlayer.Status.Playing } }
            val frame = withTimeout(10.seconds) { player.frame.first { it != null && it.number > 2 } }!!
            // Color 8 is red.
            assertEquals(0xFFFF004D.toInt(), frame.pixels[0])
        }

    @Test
    fun bytesThatAreNotACartSayNotACart() =
        runBlocking {
            val player = KikoPlayer(source = { "hello".encodeToByteArray() }, openAudio = { null }).also { players.add(it) }
            player.resume()
            withTimeout(10.seconds) { player.status.first { it == KikoPlayer.Status.NotACart } }
            Unit
        }

    @Test
    fun noBytesSayNotACart() =
        runBlocking {
            val player = player(null)
            player.resume()
            withTimeout(10.seconds) { player.status.first { it == KikoPlayer.Status.NotACart } }
            Unit
        }

    @Test
    fun aGameThatEndsStops() =
        runBlocking {
            val player = player("print('bye')")
            player.resume()
            val status = withTimeout(10.seconds) { player.status.first { it is KikoPlayer.Status.Stopped } }
            assertEquals(KikoPlayer.Status.Stopped(null), status)
        }

    @Test
    fun heldButtonsReachTheGame() {
        val player =
            player(
                """
                function _update() if (btn(4)) printh("o") end
                function _draw() end
                """,
            )
        player.resume()
        player.setButtons(Buttons.O)
        awaitLog("o")
    }

    @Test
    fun aPausedGameResumesWhereItWas() =
        runBlocking {
            val player = player("n = 0 function _update() n += 1 end function _draw() cls() print(n) end")
            player.resume()
            val before = withTimeout(10.seconds) { player.frame.first { it != null && it.number > 5 } }!!.number
            player.pause()
            player.resume()
            val after = withTimeout(10.seconds) { player.frame.first { it != null && it.number > before } }!!.number
            assertTrue(after > before, "frame $after after $before")
        }

    @Test
    fun savesGoToTheStore() {
        val written = CopyOnWriteArrayList<Map<String, IntArray>>()
        val store =
            object : KikoSaveStore {
                override suspend fun loadAll(): Map<String, IntArray> = mapOf("game" to IntArray(KikoSaveStore.VALUES) { 3 shl 16 })

                override suspend fun save(saves: Map<String, IntArray>) {
                    written.add(saves)
                }
            }
        val player =
            player(
                """
                cartdata("game")
                printh("loaded " .. dget(0))
                dset(0, 4)
                function _draw() end
                """,
                store,
            )
        player.resume()
        awaitLog("loaded 3")
        runBlocking { withTimeout(10.seconds) { while (written.isEmpty()) delay(10.milliseconds) } }
        assertEquals(4 shl 16, written.first().getValue("game")[0])
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
            awaitLog("in")
            player.close()
            // Nothing to wait on: a stuck thread would show as a CPU left busy, so check the console
            // was told to stop by watching the status leave Playing.
            withTimeout(10.seconds) { player.status.first { it is KikoPlayer.Status.Stopped } }
            Unit
        }
}
