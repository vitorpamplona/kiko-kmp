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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Plays the cart named on the command line (`.p8.png`, `.p8` or `.p8.rom`), or [DEMO_CART]
 * without one. File > Open (or the Open cart button) picks another. Saves last while the window
 * is open.
 */
fun main(args: Array<String>) =
    application {
        var cart by remember { mutableStateOf(args.firstOrNull()?.let(::File)) }
        val title = cart?.name ?: "kiko demo"
        Window(
            onCloseRequest = ::exitApplication,
            title = title,
            state = rememberWindowState(width = 600.dp, height = 760.dp),
        ) {
            MenuBar {
                Menu("File") {
                    Item("Open cart…", shortcut = KeyShortcut(Key.O, ctrl = true)) {
                        pickCart(window)?.let { cart = it }
                    }
                    Item("Play the demo") { cart = null }
                    Separator()
                    Item("Quit", shortcut = KeyShortcut(Key.Q, ctrl = true), onClick = ::exitApplication)
                }
            }
            val file = cart
            DemoScreen(
                key = file,
                source = { withContext(Dispatchers.IO) { file?.readBytes() } },
                onOpen = { pickCart(window)?.let { cart = it } },
                onExit = ::exitApplication,
            )
        }
    }

private fun pickCart(parent: Frame): File? {
    val dialog = FileDialog(parent, "Open a cart", FileDialog.LOAD)
    dialog.isVisible = true
    val name = dialog.file ?: return null
    return File(dialog.directory, name)
}
