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

import com.vitorpamplona.kiko.cart.Cart
import com.vitorpamplona.kiko.cart.CartFormatException
import com.vitorpamplona.kiko.cart.CartLoader
import com.vitorpamplona.kiko.lua.Fix
import com.vitorpamplona.kiko.runtime.ConsoleHost
import com.vitorpamplona.kiko.runtime.FantasyConsole
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import com.sun.management.ThreadMXBean as AllocationCountingThreadMXBean

/**
 * Carts come from strangers on Nostr. A hostile one may fail to load or stop with an error, but
 * must never exhaust the app's memory or stack, or keep a core busy after it is told to stop.
 */
class HostileCartTest {
    private fun chunk(
        type: String,
        data: ByteArray,
        declaredLength: Int = data.size,
    ): ByteArray {
        val crc = CRC32().apply { update(type.encodeToByteArray() + data) }
        return ByteBuffer
            .allocate(12 + data.size)
            .putInt(declaredLength)
            .put(type.encodeToByteArray())
            .put(data)
            .putInt(crc.value.toInt())
            .array()
    }

    private fun png(vararg chunks: ByteArray) = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10) + chunks.reduce { a, b -> a + b }

    private val ihdr =
        chunk(
            "IHDR",
            ByteBuffer
                .allocate(13)
                .putInt(160)
                .putInt(205)
                .put(8)
                .put(6)
                .put(0)
                .put(0)
                .put(0)
                .array(),
        )

    @Test
    fun aChunkLengthNearIntMaxIsRejectedNotAllocated() {
        val bomb = png(ihdr, chunk("PLTE", ByteArray(3), declaredLength = 0x7ffffff0))
        assertFailsWith<CartFormatException> { CartLoader.load(bomb) }
    }

    @Test
    fun aDecompressionBombStopsAtTheImageSize() {
        // 512 MiB of zeros deflate to about half a megabyte.
        val deflater = Deflater(9).apply { setInput(ByteArray(64 shl 20)) }
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1 shl 16)
        repeat(8) {
            if (it == 7) deflater.finish()
            deflater.setInput(ByteArray(64 shl 20))
            while (!deflater.needsInput() || (it == 7 && !deflater.finished())) {
                val n = deflater.deflate(buf, 0, buf.size, if (it == 7) Deflater.FULL_FLUSH else Deflater.NO_FLUSH)
                if (n == 0 && it != 7) break
                out.write(buf, 0, n)
                if (it == 7 && deflater.finished()) break
            }
        }
        val zlib = out.toByteArray()
        val started = TimeSource.Monotonic.markNow()
        assertFailsWith<CartFormatException> { CartLoader.load(png(ihdr, chunk("IDAT", zlib), chunk("IEND", ByteArray(0)))) }
        assert(started.elapsedNow() < 5.seconds)
    }

    @Test
    fun deeplyNestedCodeIsASyntaxErrorNotAStackOverflow() {
        val code = "x=" + "(".repeat(200_000) + "1" + ")".repeat(200_000)
        val console = FantasyConsole(Cart(ByteArray(Cart.ROM_SIZE), code))
        assertEquals(FantasyConsole.State.ERROR, console.state)
    }

    @Test
    fun anyImageOfACartsSizeIsNotACart() {
        val image = BufferedImage(160, 205, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until 205) for (x in 0 until 160) image.setRGB(x, y, (0xff shl 24) or (x * 1000 + y * 77))
        val plain = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        assertFalse(CartLoader.isCart(plain))
    }

    /** Runs [code] on its own thread, aborts it after a moment, and says whether it stopped. */
    private fun stopsWhenAborted(code: String): Boolean {
        val console = FantasyConsole(Cart(ByteArray(Cart.ROM_SIZE), code))
        val worker =
            Thread(null, {
                while (console.state == FantasyConsole.State.RUNNING) console.step()
            }, "cart", FantasyConsole.RECOMMENDED_STACK_BYTES)
        worker.isDaemon = true
        worker.start()
        Thread.sleep(300)
        console.abort()
        worker.join(5_000)
        return !worker.isAlive && console.state == FantasyConsole.State.STOPPED
    }

    @Test
    fun anEndlessDrawStopsWhenAborted() {
        assert(stopsWhenAborted("function _draw() while true do end end"))
    }

    @Test
    fun recursionWithoutLoopsStopsWhenAborted() {
        assert(stopsWhenAborted("function f(n) if n > 0 then f(n - 1) f(n - 1) end end f(60)"))
    }

    /**
     * Runs [code] on its own thread until it leaves RUNNING, for at most [limit]; aborts it if it
     * has not by then. Returns the console, still running if even the abort did not stop it.
     */
    private fun runOnThread(
        code: String,
        limit: Duration = 5.seconds,
    ): FantasyConsole {
        val console = FantasyConsole(Cart(ByteArray(Cart.ROM_SIZE), code))
        val worker =
            Thread(null, {
                while (console.state == FantasyConsole.State.RUNNING) console.step()
            }, "cart", FantasyConsole.RECOMMENDED_STACK_BYTES)
        worker.isDaemon = true
        worker.start()
        worker.join(limit.inWholeMilliseconds)
        if (worker.isAlive) {
            console.abort()
            worker.join(1_000)
        }
        return console
    }

    @Test
    fun aTableThatIsItsOwnCallHandlerIsAnErrorNotAHang() {
        val started = TimeSource.Monotonic.markNow()
        val console = runOnThread("t=setmetatable({},{}) getmetatable(t).__call=t t()")
        assertEquals(FantasyConsole.State.ERROR, console.state)
        assertTrue(console.error!!.contains("'__call' chain too long"), console.error)
        assertTrue(started.elapsedNow() < 2.seconds, "took ${started.elapsedNow()}")
    }

    @Test
    fun anOperatorWhoseHandlerIsACallLoopIsAnError() {
        val console = runOnThread("t=setmetatable({},{}) getmetatable(t).__call=t getmetatable(t).__add=t x=t+1")
        assertEquals(FantasyConsole.State.ERROR, console.state)
        assertTrue(console.error!!.contains("'__call' chain too long"), console.error)
    }

    @Test
    fun aCallLoopRetriedForeverStopsWhenAborted() {
        // Each attempt fails inside its own coroutine; the cart keeps trying until it is told to stop.
        assertTrue(
            stopsWhenAborted(
                "t=setmetatable({},{}) getmetatable(t).__call=t while true do coresume(cocreate(function() t() end)) end",
            ),
        )
    }

    /** Short of memory once the thread that made it has allocated more than [limit] bytes. */
    private class AllocationLimitHost(
        private val limit: Long,
    ) : ConsoleHost {
        private val threads = ManagementFactory.getThreadMXBean() as AllocationCountingThreadMXBean
        private val start = threads.currentThreadAllocatedBytes

        override fun memoryLow(): Boolean = threads.currentThreadAllocatedBytes - start > limit
    }

    /**
     * Runs [code], whose loop counts in the global `i`, on this thread until the host's memory
     * runs short, and returns how far the loop got before the cart was stopped.
     */
    private fun iterationsBeforeOutOfMemory(code: String): Int {
        val console = FantasyConsole(Cart(ByteArray(Cart.ROM_SIZE), code), AllocationLimitHost(4L shl 20))
        var frames = 0
        while (console.state == FantasyConsole.State.RUNNING && frames++ < 10_000) console.step()
        assertEquals(FantasyConsole.State.ERROR, console.state)
        assertTrue(console.error!!.contains("out of memory"), console.error)
        return Fix.toInt(console.interp.globals.rawget("i") as Int)
    }

    @Test
    fun bigConcatenationsCheckMemoryBeforeTheSampledCheck() {
        // 64 KiB strings: the check every thousand or so loop iterations would come only after
        // 64 MiB, a megabyte string after a gigabyte.
        val i = iterationsBeforeOutOfMemory("""s="x" for k=1,16 do s=s..s end t={} i=0 while true do i+=1 t[i]=s..i end""")
        assertTrue(i < 200, "stopped after $i strings")
    }

    @Test
    fun bigSplitsCheckMemoryBeforeTheSampledCheck() {
        val i = iterationsBeforeOutOfMemory("""s="x" for k=1,10 do s=s..s end t={} i=0 while true do i+=1 t[i]=split(s,1) end""")
        assertTrue(i < 400, "stopped after $i splits")
    }

    @Test
    fun bigPeeksCheckMemoryBeforeTheSampledCheck() {
        // Thrown away at once, so only the charge can stop this before the sampled check does.
        val i = iterationsBeforeOutOfMemory("i=0 while true do i+=1 peek(0,32767) end")
        assertTrue(i < 500, "$i big peeks before the memory check")
    }

    @Test
    fun bigConstructorsCheckMemoryBeforeTheSampledCheck() {
        // Written-out items cost no loop iterations: a thousand of these tables before the
        // sampled check would be most of a gigabyte.
        val items = List(40000) { "1" }.joinToString(",")
        val i = iterationsBeforeOutOfMemory("t={} i=0 while true do i+=1 t[i]={$items} end")
        assertTrue(i < 100, "stopped after $i tables")
    }

    @Test
    fun aCartCanOnlyHoldSoManyOtherCarts() {
        // Every read keeps a parsed cart, and a note can give one cart thousands of names. The last
        // line shows that what was read before the limit still answers. Frames, not one frame: this
        // many reads can cross into the next one on a busy machine.
        val carts = List(1000) { "#c$it" }.associateWith { "__lua__\n__gfx__\n77\n" }
        val code = """for i=0,999 do reload(0,0,1,"#c"..i) end memset(0,0,1) reload(0,0,1,"#c0") printh(peek(0))"""
        val t = TestConsole(code, carts).frames(1000)
        assertEquals(listOf("119"), t.log)
        assertEquals(FantasyConsole.MAX_OTHER_CARTS, t.askedFor.size)
    }

    @Test
    fun aCartTheHostHasNotIsAskedForOnce() {
        val t = TestConsole("""for i=1,100 do reload(0,0,1,"#gone") end""").frames(1000)
        assertEquals(listOf("#gone"), t.askedFor)
    }

    @Test
    fun aNameFarTooLongForACartIsNotPassedOnOrKept() {
        val code = """s="x" for i=1,20 do s=s..s end reload(0,0,1,s)"""
        val t = TestConsole(code).frames(1000)
        assertEquals(FantasyConsole.State.FINISHED, t.console.state)
        assertEquals(emptyList(), t.askedFor)
    }

    @Test
    fun aLoopOfBigAllocationsStillEndsItsFrames() {
        // Each split builds 64K strings: a thousand of them before the sampled frame check would
        // hold one frame for seconds, long enough for a player's watchdog to call the game hung.
        val console = FantasyConsole(Cart(ByteArray(Cart.ROM_SIZE), """s="x" for k=1,16 do s=s..s end i=0 while true do i+=1 x=split(s,1) end"""))
        repeat(3) {
            val started = TimeSource.Monotonic.markNow()
            console.step()
            assertTrue(started.elapsedNow() < 1.seconds, "frame $it took ${started.elapsedNow()}")
        }
        assertEquals(FantasyConsole.State.RUNNING, console.state)
    }

    @Test
    fun aConstructorPastTheLastArrayIndexKeepsALengthACartCanRead() {
        val items = List(33000) { "1" }.joinToString(",")
        // No Lua loop: a long one crosses into the next frame on a busy machine, and this runs one.
        val out = run("local t={$items} printh(#t) printh(t[32767]) printh(count(t))")
        assertEquals(listOf("32767", "1", "32767"), out)
    }

    @Test
    fun keyedNilsBetweenPositionalItemsCostWhatTheConstructorSays() {
        // Each `[k]=nil` empties the array the positional items before it built, so storing each
        // positional item as it comes regrows that array every time: half a second a constructor.
        val items = (1..32000).joinToString(",") { "\"a\",[$it]=nil" }
        val started = TimeSource.Monotonic.markNow()
        val console = runOnThread("for i=1,10 do local t={$items} end")
        assertEquals(FantasyConsole.State.FINISHED, console.state)
        assertTrue(started.elapsedNow() < 2.seconds, "took ${started.elapsedNow()}")
    }

    @Test
    fun theKeyPastTheLastArrayIndexStaysItsOwnKey() {
        // 32768 has no 16:16 number: its key is -32768's, which must not become array slot 32768.
        val items = List(32767) { "1" }.joinToString(",")
        val out = run("local t={[-32768]=\"x\",$items} printh(#t) printh(t[-32768])")
        assertEquals(listOf("32767", "x"), out)
    }

    @Test
    fun insertingIntoAFullArrayKeepsItsLength() {
        val items = (1..32767).joinToString(",")
        val out = run("local t={$items} add(t,0,1) printh(#t) printh(t[1]) printh(t[32767]) printh(t[-32768])")
        assertEquals(listOf("32767", "0", "32766", "32767"), out)
    }

    @Test
    fun hugeSpritesAndMapsCostWhatTheyDraw() {
        val started = TimeSource.Monotonic.markNow()
        val out = run("sspr(0, 0, 8, 8, -16000, -16000, 32767, 32767) map(0, 0, -100000, -100000, 32767, 32767) printh('done')")
        assertEquals(listOf("done"), out)
        assert(started.elapsedNow() < 2.seconds) { "took ${started.elapsedNow()}" }
    }
}
