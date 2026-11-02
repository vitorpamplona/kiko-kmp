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
package com.vitorpamplona.kiko.demo

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FolderCartsTest {
    private val root = createTempDirectory("folder-carts").toFile()
    private val folder = File(root, "game").apply { mkdir() }
    private val carts = FolderCarts(folder)

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun write(
        name: String,
        text: String = name,
    ) = File(folder, name).writeText(text)

    private fun loaded(name: String) = carts.loadCart(name)?.decodeToString()

    @Test
    fun anIdOnTheOriginalsSiteIsItsPngBesideTheOpenedCart() {
        write("intoruins_main-7.p8.png")
        assertEquals("intoruins_main-7.p8.png", loaded("#intoruins_main-7"))
    }

    @Test
    fun aFileNameIsTakenAsTheCartWroteIt() {
        write("level2.p8", "the file")
        write("level2.p8.p8", "not this one")
        assertEquals("the file", loaded("level2.p8"))
    }

    @Test
    fun aNameWithoutAnExtensionFindsTheCart() {
        write("level3.p8")
        assertEquals("level3.p8", loaded("level3"))
    }

    @Test
    fun anIdWithoutAVersionIsItsLatestVersion() {
        write("game-2.p8.png", "old")
        write("game-10.p8.png", "new")
        write("game-other.p8.png")
        write("gamey-30.p8.png")
        write("30.p8.png")
        write("30")
        assertEquals("new", loaded("#game"))
    }

    @Test
    fun anIdThatMatchesExactlyBeatsItsVersions() {
        write("game.p8.png", "exact")
        write("game-2.p8.png", "versioned")
        assertEquals("exact", loaded("#game"))
    }

    @Test
    fun onlyAnIdMeansItsLatestVersion() {
        write("game-2.p8.png")
        assertNull(loaded("game"))
    }

    @Test
    fun aCartThatIsNotThereIsNull() {
        assertNull(loaded("#elsewhere"))
    }

    @Test
    fun aFolderIsNotACart() {
        File(folder, "levels.p8").mkdir()
        assertNull(loaded("levels"))
    }

    @Test
    fun aFileTooBigToBeACartIsNeverRead() {
        // A cart names any file in the folder, which may be the user's Downloads.
        File(folder, "holiday.mov").writeBytes(ByteArray((1 shl 20) + 1))
        assertNull(loaded("holiday.mov"))
    }

    @Test
    fun aNameNeverLeavesTheFolder() {
        val secret = File(root, "secret.p8").apply { writeText("secret") }
        File(folder, "sub").mkdir()
        write("sub/inner.p8")
        for (name in listOf("../secret.p8", "../secret", "#../secret", "..\\secret.p8", secret.absolutePath, "sub/inner.p8")) {
            assertNull(loaded(name), name)
        }
    }
}
