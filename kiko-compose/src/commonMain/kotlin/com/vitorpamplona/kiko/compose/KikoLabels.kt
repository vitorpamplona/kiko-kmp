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

import androidx.compose.runtime.Immutable

/**
 * The words [KikoGame] shows and reads out, in English by default: an app passes its own
 * translations. The library ships no string resources.
 */
@Immutable
public class KikoLabels(
    public val loading: String = "Loading the game…",
    public val noCart: String = "The game could not be loaded.",
    public val notACart: String = "This is not a game cartridge.",
    public val gameEnded: String = "The game ended.",
    public val gameError: String = "The game stopped with an error.",
    public val directionPad: String = "Direction pad",
    public val actionButtons: String = "O button, X button",
) {
    public companion object {
        /** The English words. */
        public val Default: KikoLabels = KikoLabels()
    }
}
