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

/**
 * What a [KikoPlayer] asks the app, beyond the cart's bytes and its saves. Every method has a
 * default, so an app overrides only what it has. Called from the game's thread.
 */
public interface KikoPlayerHost {
    /** A line the cart printed with `printh`. */
    public fun log(message: String) {}

    /** The local time zone's offset from UTC, for `stat(90..95)`. */
    public fun utcOffsetSeconds(): Int = 0

    /**
     * The bytes of another cart of the same game, by the name the cart used (`load`, `reload`),
     * or null when the app has none: the game then stops with a message.
     */
    public fun loadCart(name: String): ByteArray? = null
}
