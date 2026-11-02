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

import com.vitorpamplona.kiko.player.KikoPlayerHost
import java.io.File

/**
 * Finds the other carts of a game in the [folder] of the one that was opened, so a game in several
 * carts plays from files: `#some_id` is `some_id.p8.png` there, or its latest `some_id-7.p8.png`
 * when the cart names no version; `level2` is `level2.p8.png` or `level2.p8`, and a name with its
 * extension is that file. A cart picks the name, so it never reaches outside [folder].
 */
class FolderCarts(
    private val folder: File,
) : KikoPlayerHost {
    override fun loadCart(name: String): ByteArray? {
        val id = name.removePrefix("#")
        if ('/' in id || '\\' in id) return null
        val exact = listOf(id, "$id$PNG_CART", "$id.p8").map { File(folder, it) }.firstOrNull { it.isFile }
        val file = exact ?: if (name.startsWith("#")) latestVersionOf(id) else null
        return file?.takeIf { it.length() <= MAX_CART_BYTES }?.readBytes()
    }

    private fun latestVersionOf(id: String): File? {
        val prefix = "$id-"
        val versions =
            folder
                .list { _, name -> name.startsWith(prefix) && name.endsWith(PNG_CART) }
                .orEmpty()
                .mapNotNull { name -> name.removeSurrounding(prefix, PNG_CART).toIntOrNull()?.let { it to name } }
        val latest = versions.maxByOrNull { (version, _) -> version } ?: return null
        return File(folder, latest.second).takeIf { it.isFile }
    }

    private companion object {
        const val PNG_CART = ".p8.png"

        /** A cart PNG is tens of KiB; anything far past that is not one. */
        const val MAX_CART_BYTES = 1L shl 20
    }
}
