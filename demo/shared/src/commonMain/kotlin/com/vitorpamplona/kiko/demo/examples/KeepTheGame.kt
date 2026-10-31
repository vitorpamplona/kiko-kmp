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
package com.vitorpamplona.kiko.demo.examples

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.kiko.compose.KikoGame
import com.vitorpamplona.kiko.compose.KikoLabels
import com.vitorpamplona.kiko.player.KikoPlayer
import com.vitorpamplona.kiko.player.KikoSaveStore

/**
 * A game that survives an Android rotation, saves high scores, and speaks the app's language:
 * the app holds the [KikoPlayer] in a ViewModel and passes it to [KikoGame].
 */
class GameViewModel(
    download: suspend () -> ByteArray?,
    saves: KikoSaveStore,
) : ViewModel() {
    val player = KikoPlayer(source = download, saves = saves)

    override fun onCleared() = player.close()
}

@Composable
fun KeepTheGame(
    url: String,
    download: suspend (String) -> ByteArray?,
    saves: KikoSaveStore,
    onBack: () -> Unit,
) {
    val game = viewModel(key = url) { GameViewModel({ download(url) }, saves) }
    KikoGame(
        player = game.player,
        // Your app's own words for what the player says on screen; English where you give none.
        labels = KikoLabels(loading = "Carregando o jogo…", gameEnded = "Fim de jogo."),
        onExit = onBack,
    )
}

/**
 * What games save with `cartdata()`: 64 numbers per game. Keep them where your app keeps its
 * settings (DataStore, a file, a database); this one keeps them only in memory.
 */
class MemorySaves : KikoSaveStore {
    private val saves = HashMap<String, IntArray>()

    override suspend fun loadAll(): Map<String, IntArray> = saves.toMap()

    override suspend fun save(saves: Map<String, IntArray>) = this.saves.putAll(saves)
}
