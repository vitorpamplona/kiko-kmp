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

import com.vitorpamplona.kiko.demo.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * A game the demo offers in its list: [title], who made it ([author]) and under what [license],
 * where it comes from ([source]), and its cart among the demo's resources ([file]; null for
 * [DEMO_CART]). Every bundled game and its license is listed in demo/CREDITS.md.
 */
class DemoGame(
    val title: String,
    val author: String,
    val license: String,
    val source: String,
    private val file: String?,
) {
    @OptIn(ExperimentalResourceApi::class)
    suspend fun cart(): ByteArray = file?.let { Res.readBytes(it) } ?: DEMO_CART.encodeToByteArray()
}

val DEMO_GAMES: List<DemoGame> =
    listOf(
        DemoGame(
            title = "Kiko demo",
            author = "kiko-kmp",
            license = "MIT",
            source = "github.com/vitorpamplona/kiko-kmp",
            file = null,
        ),
        DemoGame(
            title = "Snowman",
            author = "aquova",
            license = "CC0 1.0",
            source = "github.com/aquova/pico8",
            file = "files/carts/snowman.p8",
        ),
        DemoGame(
            title = "1492",
            author = "A.C. Wright Design",
            license = "MIT",
            source = "github.com/acwright/PICO-8",
            file = "files/carts/1492.p8",
        ),
    )
