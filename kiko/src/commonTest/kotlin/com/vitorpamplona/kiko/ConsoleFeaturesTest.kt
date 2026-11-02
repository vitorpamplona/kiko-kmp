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
package com.vitorpamplona.kiko

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsoleFeaturesTest {
    @Test
    fun printModesChangeTheWidth() {
        val out =
            run(
                """
                cls()
                printh(print("ab", 0, 0))
                printh(print("\^wab", 0, 10))
                printh(print("\^pab", 0, 20))
                printh(print("a\^-wb", 0, 30))
                printh(print("\*3x", 0, 40))
                """.trimIndent(),
            )
        assertEquals(listOf("8", "16", "16", "8", "12"), out)
    }

    @Test
    fun printControlCodesDraw() {
        val out =
            run(
                """
                cls()
                print("\#8 ", 10, 10, 7)
                printh(pget(10, 10) .. pget(9, 9))
                print("\fca", 20, 20)
                printh(pget(20, 20))
                print("\^ia", 30, 30, 7)
                printh(pget(33, 30) .. pget(30, 30))
                print("\^o8ffa", 40, 40, 7)
                printh(pget(39, 40) .. pget(40, 40))
                """.trimIndent(),
            )
        // Solid background fills the cell and its padding; invert fills around the glyph; outlines go outside it.
        assertEquals(listOf("88", "12", "70", "87"), out)
    }

    @Test
    fun customFontAtX5600() {
        val out =
            run(
                """
                cls()
                poke(0x5600, 6, 6, 8)        -- width, wide width, height
                poke(0x5600 + ord("a") * 8, 0b111111)
                printh(print("\014aa", 0, 0, 7))
                printh(pget(5, 0) .. pget(6, 0))
                """.trimIndent(),
            )
        assertEquals(listOf("12", "77"), out)
    }

    @Test
    fun busyWaitWithoutFlipStillSeesTimePass() {
        // The cart's own loop never calls flip(): frames are cut for it once each runs out of time.
        val t = TestConsole("while time() < 0.1 do end printh('done')")
        repeat(20) { t.console.step() }
        assertEquals(listOf("done"), t.log)
    }

    @Test
    fun pauseMenuRunsMenuItemCallbacks() {
        val t =
            TestConsole(
                """
                menuitem(1, "say hi", function(b) printh("hi " .. b) end)
                function _update() end
                function _draw() end
                """.trimIndent(),
            )
        t.frames(1)
        t.console.setButtons(0, 0x40) // pause
        t.frames(1)
        t.console.setButtons(0, 0)
        t.console.setButtons(0, 0x08) // down to the menu item
        t.frames(1)
        t.console.setButtons(0, 0)
        t.console.setButtons(0, 0x20) // X
        t.frames(1)
        assertEquals(listOf("hi 32"), t.log)
    }

    @Test
    fun devkitMouseAndKeyboard() {
        val t =
            TestConsole(
                """
                poke(0x5f2d, 1)
                function _update()
                  if (stat(30)) printh("key " .. stat(31))
                  printh(stat(32) .. "," .. stat(33) .. "," .. stat(34))
                end
                """.trimIndent(),
            )
        // Typing before the cart turns the keyboard on is dropped.
        t.console.type("x")
        t.frames(1)
        t.console.setPointer(10, 20, 1)
        t.console.type("q")
        t.frames(1)
        assertEquals(listOf("0,0,0", "key q", "10,20,1"), t.log)
    }

    @Test
    fun colorsCarryFillPatternsAndInvert() {
        val out =
            run(
                """
                cls()
                poke(0x5f34, 3)
                -- fill bit 0x1000, invert bit 0x0800, color 8, pattern 0 (solid)
                circfill(64, 64, 4, 0x1808.0000)
                printh(pget(64, 64) .. pget(0, 0))
                """.trimIndent(),
            )
        assertEquals(listOf("08"), out)
    }

    @Test
    fun outOfBoundsReadsCanReturnAValue() {
        val out =
            run(
                """
                printh(mget(-1, 0) .. sget(200, 0))
                poke(0x5f36, 0x10)
                poke(0x5f5a, 9)
                poke(0x5f59, 3)
                printh(mget(-1, 0) .. sget(200, 0))
                """.trimIndent(),
            )
        assertEquals(listOf("00", "93"), out)
    }

    @Test
    fun mapCanMoveToUpperMemory() {
        val out =
            run(
                """
                poke(0x5f56, 0x80)    -- map at 0x8000
                poke(0x5f57, 0)       -- 256 wide
                mset(200, 3, 42)
                printh(peek(0x8000 + 3 * 256 + 200))
                printh(mget(200, 3))
                """.trimIndent(),
            )
        assertEquals(listOf("42", "42"), out)
    }

    @Test
    fun runRestartsWithAParameter() {
        val out =
            run(
                """
                printh("p=" .. stat(6))
                if (stat(6) == "") run("again")
                """.trimIndent(),
                frames = 3,
            )
        assertEquals(listOf("p=", "p=again"), out)
    }

    @Test
    fun gamesSpanSeveralCarts() {
        val carts =
            mapOf(
                "#part2" to "pico-8 cartridge\n__lua__\nprinth(\"part2 \" .. stat(6) .. \" \" .. stat(100)) extcmd(\"breadcrumb\")\n",
                "data.p8" to "pico-8 cartridge\n__lua__\n__gfx__\n77\n",
            )
        val out =
            run(
                "reload(0, 0, 1, \"data.p8\") printh(peek(0)) printh(\"param \" .. stat(6)) if (stat(6) == \"\") load(\"#part2\", \"back to part 1\", \"from1\")",
                frames = 4,
                carts = carts,
            )
        assertEquals(listOf("119", "param ", "part2 from1 back to part 1", "119", "param "), out.take(5))
    }

    private fun other(code: String) = mapOf("#other" to "pico-8 cartridge\n__lua__\n$code\n")

    @Test
    fun loadKeepsTheMemoryCartsPassDataIn() {
        val out =
            run(
                "poke(0x4300,1) poke(0x5dff,2) poke(0x8000,3) poke(0xffff,4) load(\"#other\")",
                frames = 3,
                carts = other("printh(peek(0x4300)..\",\"..peek(0x5dff)..\",\"..peek(0x8000)..\",\"..peek(0xffff))"),
            )
        assertEquals(listOf("1,2,3,4"), out)
    }

    @Test
    fun loadStillResetsTheRest() {
        val out =
            run(
                "poke(0x42ff,1) poke(0x5e00,2) poke(0x7fff,3) color(3) load(\"#other\")",
                frames = 3,
                carts = other("printh(peek(0x42ff)..\",\"..peek(0x5e00)..\",\"..peek(0x7fff)..\",\"..peek(0x5f25))"),
            )
        assertEquals(listOf("0,0,0,6"), out)
    }

    @Test
    fun goingBackToTheCartBeforeKeepsThatMemoryToo() {
        val out =
            run(
                "if peek(0x8000)==0 then load(\"#other\",\"back\") else printh(peek(0x8000)) end",
                frames = 4,
                carts = other("poke(0x8000,7) extcmd(\"breadcrumb\")"),
            )
        assertEquals(listOf("7"), out)
    }

    @Test
    fun aGamesCartsShareTheirSaves() {
        val out =
            run(
                "cartdata(\"game\") dset(0,7) load(\"#other\")",
                frames = 3,
                carts = other("cartdata(\"game\") printh(dget(0))"),
            )
        assertEquals(listOf("7"), out)
    }

    @Test
    fun aMenuCallbackThatLoadsACartKeepsWhatItSaved() {
        val t =
            TestConsole(
                """
                cartdata("game")
                menuitem(1, "next", function() dset(0,7) load("#other") end)
                function _update() end
                function _draw() end
                """.trimIndent(),
                other("function _draw() end"),
            )
        t.frames(1)
        t.console.setButtons(0, 0x40) // pause
        t.frames(1)
        t.console.setButtons(0, 0)
        t.console.setButtons(0, 0x08) // down to the menu item
        t.frames(1)
        t.console.setButtons(0, 0)
        t.console.setButtons(0, 0x20) // X
        t.frames(1)
        assertEquals(7 shl 16, t.saved["game"]?.get(0))
    }

    @Test
    fun peekReadsMoreThan8192Bytes() {
        // Into Ruins reads its 10,019 byte table back this way; a shorter list loses the end of it.
        val out = run("poke(0x8000+10018,65) t={peek(0x8000,10019)} printh(#t..\",\"..t[10019])")
        assertEquals(listOf("10019,65"), out)
    }

    @Test
    fun splittingNothingGivesNothingToWalk() {
        // Into Ruins passes a missing attack pattern here on every plain attack.
        val out = run("for n in all(split(nil,\"|\")) do printh(\"part \"..n) end printh(\"done\")")
        assertEquals(listOf("done"), out)
    }

    @Test
    fun aMissingCartIsAClearError() {
        val out = run("load(\"#elsewhere\")")
        assertEquals(listOf("ERROR: this game continues in another cartridge (#elsewhere), which can't be loaded here"), out)
    }

    @Test
    fun nextKeepsWalkingWhenTheLoopClearsWhatItVisits() {
        val out = run("t={a=1,b=2,c=3,d=4} t[1]=5 for k in next,t do t[k]=nil end n=0 for k in pairs(t) do n+=1 end printh(n)")
        assertEquals(listOf("0"), out)
    }

    @Test
    fun aStringThatDoublesForeverRunsOutOfMemory() {
        // A few frames: building megabyte strings is slow on some targets (macOS), and once the
        // allocator has charged its budget a frame that ran over ends there, so the cart may reach
        // the limit only in the next one.
        val out = run("s='x' while true do s=s..s end", frames = 10)
        assertEquals(1, out.size)
        assertTrue(out[0].contains("out of memory"), out[0])
    }

    @Test
    fun cartDataIdsAreCappedAtTheOriginalsLength() {
        val t = TestConsole("cartdata('" + "a".repeat(300) + "') dset(0, 7)").frames(1)
        assertEquals(listOf("a".repeat(64)), t.saved.keys.toList())
    }
}
