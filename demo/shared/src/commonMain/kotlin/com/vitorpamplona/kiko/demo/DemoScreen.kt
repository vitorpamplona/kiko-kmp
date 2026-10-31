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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitorpamplona.kiko.compose.KikoGame

/**
 * The demo on every platform: [KikoGame] playing one of [DEMO_GAMES], picked from a list that
 * credits each game's author and license, or a cart the user opened. [key] and [source] are that
 * opened cart (null when there is none); a new [key] plays it. [onOpen], where the platform can
 * pick a file, adds "Open a cart…" to the list.
 */
@Composable
fun DemoScreen(
    key: Any?,
    source: suspend () -> ByteArray?,
    onOpen: (() -> Unit)?,
    onExit: (() -> Unit)? = null,
) {
    // The game playing now: one of DEMO_GAMES, or null for the user's own cart.
    var game by remember { mutableStateOf<DemoGame?>(DEMO_GAMES.first()) }
    var listOpen by remember { mutableStateOf(false) }
    LaunchedEffect(key) { if (key != null) game = null }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val playing = game
        KikoGame(
            key = playing ?: key,
            source = { playing?.cart() ?: source() ?: DEMO_CART.encodeToByteArray() },
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            onExit = onExit,
        )

        Row(
            Modifier.fillMaxWidth().safeDrawingPadding().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // Credit where it is due, on screen while the game plays.
            BasicText(
                text = playing?.let { "${it.title} by ${it.author} · ${it.license}" } ?: "",
                style = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp),
                modifier = Modifier.weight(1f).padding(top = 8.dp, end = 8.dp),
            )
            Chip("Games") { listOpen = true }
        }

        if (listOpen) {
            GameList(
                current = game,
                onPick = {
                    game = it
                    listOpen = false
                },
                onOpen =
                    onOpen?.let { open ->
                        {
                            listOpen = false
                            open()
                        }
                    },
                onClose = { listOpen = false },
            )
        }
    }
}

@Composable
private fun GameList(
    current: DemoGame?,
    onPick: (DemoGame) -> Unit,
    onOpen: (() -> Unit)?,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .safeDrawingPadding()
                .widthIn(max = 420.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicText("Games", style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold))
            for (game in DEMO_GAMES) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = if (game == current) 0.22f else 0.1f), RoundedCornerShape(12.dp))
                        .clickable { onPick(game) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    BasicText(game.title, style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    BasicText(
                        "by ${game.author} · ${game.license}\n${game.source}",
                        style = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp),
                    )
                }
            }
            if (onOpen != null) Chip("Open a cart…", onOpen)
            BasicText(
                "Thanks to the authors who share their games. Credits and licenses: demo/CREDITS.md",
                style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Chip(
    text: String,
    onClick: () -> Unit,
) {
    BasicText(
        text = text,
        style = TextStyle(color = Color.White, fontSize = 14.sp),
        modifier =
            Modifier
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
