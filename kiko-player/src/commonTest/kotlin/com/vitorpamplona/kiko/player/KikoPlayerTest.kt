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
import com.vitorpamplona.kiko.runtime.FantasyConsole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A [KikoPlayer] on virtual time: the game, its clock, its watchdog and its saves all run on the
 * test's scheduler, so a second of play takes no time and frame counts are exact. What needs a
 * real thread (a frame that never returns) is [KikoPlayerThreadTest]'s, on the JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KikoPlayerTest {
    private val log = ArrayList<String>()
    private val players = ArrayList<KikoPlayer>()

    /**
     * [runTest] that closes every player afterwards, failed tests included: a running game
     * always has its next frame scheduled, so the test's final advance-until-idle would never end.
     */
    private fun runPlayerTest(block: suspend TestScope.() -> Unit): TestResult =
        runTest {
            try {
                block()
            } finally {
                players.forEach { it.close() }
            }
        }

    private fun cart(code: String) = "__lua__\n${code.trimIndent()}\n".encodeToByteArray()

    private fun TestScope.player(
        store: KikoSaveStore? = null,
        source: suspend () -> ByteArray?,
    ): KikoPlayer {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return KikoPlayer(
            source = source,
            saves = store,
            host =
                object : KikoPlayerHost {
                    override fun log(message: String) {
                        log.add(message)
                    }
                },
            openAudio = { null },
            gameDispatcher = dispatcher,
            watchdogDispatcher = dispatcher,
            timeSource = testScheduler.timeSource,
            saveBuffer = SaveBuffer(store, CoroutineScope(dispatcher)),
        ).also { players.add(it) }
    }

    private fun TestScope.play(
        player: KikoPlayer,
        time: Duration,
    ) {
        player.resume()
        advanceTimeBy(time)
        runCurrent()
    }

    @Test
    fun aGamePublishesFramesOfWhatItDraws() =
        runPlayerTest {
            val player = player { cart("function _draw() cls(8) end") }
            val pixels = IntArray(FantasyConsole.SCREEN_SIZE * FantasyConsole.SCREEN_SIZE)
            assertEquals(0L, player.copyFrame(pixels))
            play(player, 500.milliseconds)
            assertEquals(KikoPlayer.Status.Playing, player.status.value)
            val number = player.copyFrame(pixels)
            assertEquals(player.frameNumber.value, number)
            assertTrue(number > 2, "frame $number")
            // Color 8 is red.
            assertEquals(0xFFFF004D.toInt(), pixels[0])
        }

    @Test
    fun aGameRunsAtItsOwnFrameRate() =
        runPlayerTest {
            val at30 = player { cart("function _update() end function _draw() end") }
            val at60 = player { cart("function _update60() end function _draw() end") }
            at30.resume()
            at60.resume()
            advanceTimeBy(1.seconds)
            runCurrent()
            assertTrue(at30.frameNumber.value in 29L..32L, "30 fps: ${at30.frameNumber.value}")
            assertTrue(at60.frameNumber.value in 59L..62L, "60 fps: ${at60.frameNumber.value}")
        }

    @Test
    fun bytesThatAreNotACartFail() =
        runPlayerTest {
            val player = player { "hello".encodeToByteArray() }
            play(player, 100.milliseconds)
            assertEquals(KikoPlayer.Status.Failed(KikoPlayer.Reason.NOT_A_CART), player.status.value)
        }

    @Test
    fun noBytesFailWithNoCart() =
        runPlayerTest {
            val nothing = player { null }
            val broken = player { error("offline") }
            play(nothing, 100.milliseconds)
            play(broken, 100.milliseconds)
            assertEquals(KikoPlayer.Status.Failed(KikoPlayer.Reason.NO_CART), nothing.status.value)
            assertEquals(KikoPlayer.Status.Failed(KikoPlayer.Reason.NO_CART), broken.status.value)
        }

    @Test
    fun aGameThatEndsStops() =
        runPlayerTest {
            val player = player { cart("print('bye')") }
            play(player, 100.milliseconds)
            assertEquals(KikoPlayer.Status.Stopped(null), player.status.value)
        }

    @Test
    fun heldButtonsReachEachPlayer() =
        runPlayerTest {
            val player =
                player {
                    cart(
                        """
                        function _update() if (btn(4)) printh("o0") if (btn(5, 1)) printh("x1") end
                        function _draw() end
                        """,
                    )
                }
            play(player, 100.milliseconds)
            player.setButtons(Buttons.O)
            player.setButtons(Buttons.X, playerIndex = 1)
            advanceTimeBy(100.milliseconds)
            runCurrent()
            assertTrue("o0" in log && "x1" in log, "$log")
        }

    @Test
    fun buttonsHeldWhileLoadingCount() =
        runPlayerTest {
            val player =
                player {
                    cart(
                        """
                        if (btn(4)) printh("held from the start")
                        function _draw() end
                        """,
                    )
                }
            player.setButtons(Buttons.O)
            play(player, 100.milliseconds)
            assertTrue("held from the start" in log, "$log")
        }

    @Test
    fun aPausedGameWaitsAndResumesWhereItWas() =
        runPlayerTest {
            val player = player { cart("n = 0 function _update() n += 1 printh(n) end function _draw() end") }
            play(player, 500.milliseconds)
            player.pause()
            val paused = log.size
            advanceTimeBy(1.seconds)
            runCurrent()
            assertEquals(paused, log.size)
            play(player, 100.milliseconds)
            assertEquals((paused + 1).toString(), log[paused])
        }

    @Test
    fun savesGoToTheStore() =
        runPlayerTest {
            val written = ArrayList<Map<String, IntArray>>()
            val store =
                object : KikoSaveStore {
                    override suspend fun loadAll(): Map<String, IntArray> = mapOf("game" to IntArray(KikoSaveStore.VALUES) { 3 shl 16 })

                    override suspend fun save(saves: Map<String, IntArray>) {
                        written.add(saves)
                    }
                }
            val player =
                player(store) {
                    cart(
                        """
                        cartdata("game")
                        printh("loaded " .. dget(0))
                        dset(0, 4)
                        function _draw() end
                        """,
                    )
                }
            play(player, 100.milliseconds)
            assertTrue("loaded 3" in log, "$log")
            assertEquals(4 shl 16, written.first().getValue("game")[0])
        }
}
