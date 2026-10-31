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

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.LifecycleStartEffect
import com.vitorpamplona.kiko.player.KikoPlayer
import com.vitorpamplona.kiko.player.KikoPlayerHost
import com.vitorpamplona.kiko.player.KikoSaveStore
import kotlin.math.min

/**
 * Plays the cart [source] supplies, the simplest way: loads it, shows it, takes the controls,
 * plays its sound, and pauses while the app is in the background. [source] returns the cart's
 * bytes (`.p8.png`, `.p8` or `.p8.rom`), or null when there are none; the game reloads when
 * [key] changes. See the [KikoPlayer] overload for an app that keeps the game itself, and
 * [KikoScreen], [KikoDirectionPad], [KikoActionButtons], [kikoKeys] and [KikoStatus] to lay a
 * player out another way.
 */
@Composable
public fun KikoGame(
    source: suspend () -> ByteArray?,
    modifier: Modifier = Modifier,
    key: Any? = Unit,
    saves: KikoSaveStore? = null,
    labels: KikoLabels = KikoLabels.Default,
    showTouchControls: Boolean = true,
    pauseInBackground: Boolean = true,
    requestFocus: Boolean = true,
    onExit: (() -> Unit)? = null,
) {
    val player = rememberKikoPlayer(key = key, saves = saves, source = source)
    KikoGame(player, modifier, labels, showTouchControls, pauseInBackground, requestFocus, onExit)
}

/**
 * A [KikoPlayer] that lives while this composition does, and closes with it. It reloads when
 * [key] changes. An app that must keep the game across an Android configuration change holds a
 * [KikoPlayer] in a ViewModel instead.
 */
@Composable
public fun rememberKikoPlayer(
    key: Any?,
    saves: KikoSaveStore? = null,
    host: KikoPlayerHost? = null,
    source: suspend () -> ByteArray?,
): KikoPlayer {
    val currentSource by rememberUpdatedState(source)
    val player =
        remember(key) {
            KikoPlayer(source = { currentSource() }, saves = saves, host = host ?: object : KikoPlayerHost {})
        }
    DisposableEffect(player) { onDispose { player.close() } }
    return player
}

/**
 * The game [player] runs: its screen as large as fits, an on-screen direction pad and O/X
 * buttons when [showTouchControls], and the keyboard or a gamepad ([kikoKeys]). The screen sits
 * above the controls on a tall area and between them on a wide one.
 *
 * The game runs while this is shown. With [pauseInBackground] it also pauses while the app is in
 * the background (Home, a minimized window), which writes its saves before the system may kill
 * the app. With [requestFocus] it takes the keyboard focus when it appears or [player] changes,
 * and again when the game is touched or clicked.
 *
 * Escape (and a key-event Back) opens the game's pause menu; pressed again it calls [onExit], or
 * closes the menu when there is none. The system Back gesture is the app's to route: call
 * [KikoPlayer.openPauseMenu] from its back handler the same way. Apply window insets with
 * [modifier]; the game draws on black.
 */
@Composable
public fun KikoGame(
    player: KikoPlayer,
    modifier: Modifier = Modifier,
    labels: KikoLabels = KikoLabels.Default,
    showTouchControls: Boolean = true,
    pauseInBackground: Boolean = true,
    requestFocus: Boolean = true,
    onExit: (() -> Unit)? = null,
) {
    if (pauseInBackground) {
        LifecycleStartEffect(player) {
            player.resume()
            onStopOrDispose { player.pause() }
        }
    } else {
        DisposableEffect(player) {
            player.resume()
            onDispose { player.pause() }
        }
    }

    val input = rememberKikoInput(player)
    val currentOnExit by rememberUpdatedState(onExit)
    val back = {
        if (!player.openPauseMenu()) {
            val exit = currentOnExit
            if (exit != null) exit() else player.tapPause()
        }
    }

    val focus = remember { FocusRequester() }
    // A new game takes the keyboard, and a touch or click on the game takes it back from
    // whatever had it since (a menu the app showed over the game, a text field beside it).
    if (requestFocus) LaunchedEffect(player) { focus.requestFocus() }

    Box(
        modifier
            .background(Color.Black)
            .focusRequester(focus)
            .pointerInput(focus) {
                // Requiring nothing unconsumed: the pad and buttons consume their touches first.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    focus.requestFocus()
                }
            }.kikoKeys(input, onBack = back)
            .focusable(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight
            // Each control is a square; on a tall area both sit side by side under the screen, so
            // a narrow one shrinks them to fit its width.
            val preferred = min(maxWidth.value, maxHeight.value).coerceIn(120f, 200f).dp * if (wide) 0.9f else 0.8f
            val control = if (wide) preferred else min(preferred, (maxWidth - SIDE_GAP * 2 - 8.dp) / 2)
            when {
                !showTouchControls -> KikoScreen(player, Modifier.fillMaxSize())
                wide ->
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        KikoDirectionPad(input, Modifier.size(control), labels.directionPad)
                        KikoScreen(player, Modifier.weight(1f).aspectRatio(1f, matchHeightConstraintsFirst = true))
                        KikoActionButtons(input, Modifier.size(control), labels.actionButtons)
                    }
                else -> {
                    // The screen takes what the controls leave, so they keep their square size.
                    val screen = min(maxWidth, maxHeight - TOP_GAP - control - BOTTOM_GAP).coerceAtLeast(0.dp)
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(TOP_GAP))
                        KikoScreen(player, Modifier.size(screen))
                        Spacer(Modifier.weight(1f))
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = SIDE_GAP),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            KikoDirectionPad(input, Modifier.size(control), labels.directionPad)
                            KikoActionButtons(input, Modifier.size(control), labels.actionButtons)
                        }
                        Spacer(Modifier.height(BOTTOM_GAP))
                    }
                }
            }
        }

        KikoStatus(player, Modifier.fillMaxSize(), labels)
    }
}

private val TOP_GAP = 48.dp
private val BOTTOM_GAP = 24.dp
private val SIDE_GAP = 24.dp
