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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Where a [KikoPlayer] keeps what games save with `cartdata(id)`: [VALUES] numbers per id (high
 * scores, progress, settings), as their raw 16:16 bits. The id is the game's own name for its
 * save, so two copies of the same game share it. The calls come from a background coroutine;
 * an implementation that blocks moves to its own I/O dispatcher.
 */
public interface KikoSaveStore {
    /** Every save, by id. Read once, before a game starts. */
    public suspend fun loadAll(): Map<String, IntArray>

    /** Writes [saves], which holds only the ids that changed. */
    public suspend fun save(saves: Map<String, IntArray>)

    public companion object {
        /** The numbers in one save. */
        public const val VALUES: Int = 64
    }
}

/**
 * The saves of one running game, between the game and [store]. A save is written as soon as the
 * game makes it, so a game that saves at a checkpoint keeps it even if the app is killed right
 * after. A game that saves every frame is written at most once per [minInterval], and a save made
 * while a write is under way goes out in the next one. [flush] writes what is left when the
 * player leaves; [close] also ends the writer.
 *
 * [load] and [save] are called on the game's thread; the writes run on [scope], which outlives
 * the player so its last saves still go out after it closes.
 */
internal class SaveBuffer(
    private val store: KikoSaveStore?,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val minInterval: Duration = 1.seconds,
) {
    private var saved: Map<String, IntArray> = emptyMap()
    private val unsaved = MutableStateFlow<Map<String, IntArray>>(emptyMap())
    private val writing = Mutex()
    private var newIds = 0

    // One writer, woken by any number of saves: a save while it waits or writes is not lost, it
    // only joins the next write.
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val writer =
        lazy {
            scope.launch {
                var lastWrite: TimeMark? = null
                while (wake.receiveCatching().isSuccess) {
                    val wait = lastWrite?.let { minInterval - it.elapsedNow() } ?: Duration.ZERO
                    if (wait.isPositive()) delay(wait)
                    lastWrite = TimeSource.Monotonic.markNow()
                    write()
                }
            }
        }

    /** Reads every save once, before the game starts. */
    suspend fun load() {
        saved = store?.loadAll() ?: emptyMap()
    }

    fun get(id: String): IntArray? = saved[id]

    fun save(
        id: String,
        values: IntArray,
    ) {
        // A game uses one save id, a few for a series; a cart that cycles `cartdata` + `run()`
        // through endless ids would otherwise grow the app's store without bound.
        if (id !in saved) {
            if (newIds >= MAX_NEW_IDS) return
            newIds++
        }
        saved = saved + (id to values)
        if (store == null) return
        unsaved.update { it + (id to values) }
        writer.value // started on the first save
        wake.trySend(Unit)
    }

    /** Writes what has not been written yet. Safe from any thread. */
    fun flush(): Job = scope.launch { write() }

    /** Writes what has not been written yet and ends the writer: for a player that is gone. */
    fun close(): Job {
        wake.close()
        return flush()
    }

    private suspend fun write() {
        writing.withLock {
            val batch = unsaved.getAndUpdate { emptyMap() }
            if (batch.isNotEmpty()) store?.save(batch)
        }
    }

    private companion object {
        const val MAX_NEW_IDS = 8
    }
}
