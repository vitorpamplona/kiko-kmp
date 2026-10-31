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

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.concurrent.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The game thread on Linux, macOS and iOS has the deep stack the interpreter expects: Lua
 * recursion almost to the interpreter's own limit finishes instead of overflowing the native
 * stack, which on Kotlin/Native kills the whole process.
 */
class NativeStackTest {
    @Test
    fun deepRecursionFitsTheGameThread() =
        runBlocking {
            val printed = AtomicReference<String?>(null)
            val player =
                KikoPlayer(
                    // Not a tail call (that would not grow the stack), and no number past 32767.
                    source = { "__lua__\nfunction f(n) if n==0 then return 'done' end local r=f(n-1) return r end\nprinth(f(45000))\n".encodeToByteArray() },
                    host =
                        object : KikoPlayerHost {
                            override fun log(message: String) {
                                printed.value = message
                            }
                        },
                    openAudio = { null },
                )
            try {
                player.resume()
                withTimeout(60.seconds) { while (printed.value == null) delay(20.milliseconds) }
                assertEquals("done", printed.value)
            } finally {
                player.close()
            }
        }
}
