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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.vitorpamplona.kiko.player.KikoPlayer
import com.vitorpamplona.kiko.player.KikoSaveStore
import com.vitorpamplona.kiko.runtime.ConsoleHost
import kotlin.math.min

/**
 * Plays the cart [source] supplies, the simplest way: loads it, shows it, takes the controls,
 * plays its sound, and pauses while the app is in the background. [source] returns the cart's
 * bytes (`.p8.png`, `.p8` or `.p8.rom`), or null when there are none; the game reloads when
 * [key] changes. See the [KikoPlayer] overload for an app that keeps the game itself.
 */
@Composable
public fun KikoGame(
    source: suspend () -> ByteArray?,
    modifier: Modifier = Modifier,
    key: Any? = Unit,
    saves: KikoSaveStore? = null,
    labels: KikoLabels = KikoLabels(),
    showTouchControls: Boolean = true,
    onExit: (() -> Unit)? = null,
) {
    val player = rememberKikoPlayer(key = key, saves = saves, source = source)
    KikoGame(player, modifier, labels, showTouchControls, onExit)
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
    host: ConsoleHost? = null,
    source: suspend () -> ByteArray?,
): KikoPlayer {
    val currentSource by rememberUpdatedState(source)
    val player =
        remember(key) {
            KikoPlayer(source = { currentSource() }, saves = saves, host = host ?: object : ConsoleHost {})
        }
    DisposableEffect(player) { onDispose { player.close() } }
    return player
}

/**
 * The game [player] runs: its screen as large as fits, an on-screen direction pad and O/X
 * buttons when [showTouchControls], and the keyboard or a gamepad (arrows, Z/C/N for O, X/V/M for
 * X, Enter or P to pause). The screen sits above the controls on a tall area and between them on
 * a wide one. The player runs while the app is in front and pauses when it is not.
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
    labels: KikoLabels = KikoLabels(),
    showTouchControls: Boolean = true,
    onExit: (() -> Unit)? = null,
) {
    // Runs only while the app is in front: Home or a minimized window pauses the game and its
    // sound, and writes its saves before the system may kill the app.
    LifecycleStartEffect(player) {
        player.resume()
        onStopOrDispose { player.pause() }
    }
    val status by player.status.collectAsState()
    val frame = player.frame.collectAsState()

    // Plain holders, sent on every change: an effect keyed on them would merge a quick tap's
    // press and release into one recomposition and the game would never see the press.
    val held = remember(player) { HeldButtons() }
    val send = { player.setButtons(held.mask) }
    // A tick for each change the thumb makes: a touch screen gives it nothing else to feel.
    val haptics = LocalHapticFeedback.current
    val tick = { haptics.performHapticFeedback(HapticFeedbackType.VirtualKey) }
    // Buttons tick when one goes down; the pad on every new direction, diagonals included, but not on letting go.
    val tickIfPressed = { before: Int, now: Int -> if (now and before.inv() != 0) tick() }
    val tickIfTurned = { before: Int, now: Int -> if (now != before && now != 0) tick() }
    val currentOnExit by rememberUpdatedState(onExit)
    val back = {
        if (!player.openPauseMenu()) {
            val exit = currentOnExit
            if (exit != null) exit() else player.tapPause()
        }
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        modifier
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.Escape || event.key == Key.Back) {
                    if (event.type == KeyEventType.KeyUp) back()
                    return@onKeyEvent true
                }
                // Games that read the keyboard get every key typed, buttons included, as on the original.
                if (event.type == KeyEventType.KeyDown) typedText(event)?.let(player::type)
                val bit = KEY_BITS[event.key] ?: return@onKeyEvent false
                held.keys =
                    when (event.type) {
                        KeyEventType.KeyDown -> held.keys or bit
                        KeyEventType.KeyUp -> held.keys and bit.inv()
                        else -> held.keys
                    }
                send()
                true
            },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > maxHeight
            val control = min(maxWidth.value, maxHeight.value).coerceIn(120f, 200f).dp * if (wide) 0.9f else 0.8f
            val pad: @Composable () -> Unit = {
                DirectionPad(
                    onChange = {
                        tickIfTurned(held.pad, it)
                        held.pad = it
                        send()
                    },
                    description = labels.directionPad,
                    modifier = Modifier.size(control),
                )
            }
            val actions: @Composable () -> Unit = {
                ActionButtons(
                    size = control,
                    onChange = {
                        tickIfPressed(held.actions, it)
                        held.actions = it
                        send()
                    },
                    description = labels.actionButtons,
                )
            }
            if (!showTouchControls) {
                GameScreen(frame, player, Modifier.fillMaxSize())
            } else if (wide) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    pad()
                    GameScreen(frame, player, Modifier.weight(1f).aspectRatio(1f, matchHeightConstraintsFirst = true))
                    actions()
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(48.dp))
                    GameScreen(frame, player, Modifier.fillMaxWidth().aspectRatio(1f))
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        pad()
                        actions()
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        StatusOverlay(status, labels)
    }
}

/** What each control holds down, as button bits. */
private class HeldButtons {
    var pad = 0
    var actions = 0
    var keys = 0
    val mask get() = pad or actions or keys
}
