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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

/**
 * A thread of our own with [FantasyConsole.RECOMMENDED_STACK_BYTES] of stack: a secondary thread
 * gets 512 KiB on iOS and 8 MiB on Linux, and a cart recursing past that would not fail with an
 * error here but take the whole process down. The thread runs the tasks [dispatcher] queues until
 * [close].
 */
internal actual fun newGameThread(): GameThread {
    val tasks = Channel<Runnable>(Channel.UNLIMITED)
    val loop: () -> Unit = {
        runBlocking {
            while (true) {
                val task = tasks.receiveCatching().getOrNull() ?: break
                task.run()
            }
        }
    }
    startThread(FantasyConsole.RECOMMENDED_STACK_BYTES, loop)
    return object : GameThread {
        override val dispatcher: CoroutineDispatcher =
            object : CoroutineDispatcher() {
                override fun dispatch(
                    context: CoroutineContext,
                    block: Runnable,
                ) {
                    tasks.trySend(block)
                }
            }

        override fun close() {
            tasks.close()
        }
    }
}

/**
 * Starts a detached thread with a [stackBytes] stack running [body]. One actual per platform
 * family: Linux and Apple declare pthread_t differently, so the shared native code cannot call
 * pthread_create itself.
 */
internal expect fun startThread(
    stackBytes: Long,
    body: () -> Unit,
)

internal actual fun isMemoryLow(): Boolean = false

internal actual fun openPlatformAudioSink(sampleRate: Int): KikoAudioSink? = null
