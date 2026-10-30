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

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.vitorpamplona.kiko.player.KikoPlayer
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun bytesThatAreNotACartSaySo() {
        var player: KikoPlayer? = null
        scene =
            ImageComposeScene(width = 300, height = 500, density = Density(1f)) {
                player = rememberKikoPlayer(key = Unit) { "not a cart".encodeToByteArray() }.also { KikoGame(it) }
            }
        renderUntil { player?.status?.value == KikoPlayer.Status.NotACart }
        assertEquals(KikoPlayer.Status.NotACart, player?.status?.value)
    }

    @Test
    fun leavingTheCompositionStopsTheGame() {
        var player: KikoPlayer? = null
        scene =
            ImageComposeScene(width = 300, height = 500, density = Density(1f)) {
                player = rememberKikoPlayer(key = Unit) { cart("function _draw() cls(8) end") }.also { KikoGame(it) }
            }
        renderUntil { (player?.frame?.value?.number ?: 0) > 3 }
        scene.close()
        closed = true
        val last = player?.frame?.value?.number
        Thread.sleep(300)
        assertEquals(last, player?.frame?.value?.number)
    }

    private companion object {
        const val RED = 0xFFFF004D.toInt()
    }
}
