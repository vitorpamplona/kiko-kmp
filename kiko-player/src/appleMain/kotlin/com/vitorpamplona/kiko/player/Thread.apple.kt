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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import platform.posix.PTHREAD_CREATE_DETACHED
import platform.posix.pthread_attr_destroy
import platform.posix.pthread_attr_init
import platform.posix.pthread_attr_setdetachstate
import platform.posix.pthread_attr_setstacksize
import platform.posix.pthread_attr_t
import platform.posix.pthread_create
import platform.posix.pthread_tVar

@OptIn(ExperimentalForeignApi::class)
internal actual fun startThread(
    stackBytes: Long,
    body: () -> Unit,
) {
    memScoped {
        val attributes = alloc<pthread_attr_t>()
        pthread_attr_init(attributes.ptr)
        pthread_attr_setstacksize(attributes.ptr, stackBytes.convert())
        pthread_attr_setdetachstate(attributes.ptr, PTHREAD_CREATE_DETACHED)
        val thread = alloc<pthread_tVar>()
        val started =
            pthread_create(
                thread.ptr,
                attributes.ptr,
                staticCFunction { argument ->
                    val ref = argument!!.asStableRef<() -> Unit>()
                    try {
                        ref.get()()
                    } finally {
                        ref.dispose()
                    }
                    null
                },
                StableRef.create(body).asCPointer(),
            )
        pthread_attr_destroy(attributes.ptr)
        check(started == 0) { "could not start a thread: $started" }
    }
}
