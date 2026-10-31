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

/**
 * A cart written for this demo, in `.p8` text, so the window plays something without a file:
 * real carts stay out of the repository. Arrows move the ball, Z (O) changes its color and X
 * drops a ring.
 */
val DEMO_CART: String =
    """
    __lua__
    x,y,c=64,64,8
    rings={}
    function _update60()
     if (btn(0)) x-=1
     if (btn(1)) x+=1
     if (btn(2)) y-=1
     if (btn(3)) y+=1
     x=mid(4,x,123) y=mid(20,y,108)
     if (btnp(4)) c=c%15+1
     if (btnp(5)) add(rings,{x=x,y=y,r=4})
     for r in all(rings) do
      r.r+=0.5
      if (r.r>24) del(rings,r)
     end
    end
    function _draw()
     cls(1)
     for i=0,15 do rectfill(i*8,0,i*8+7,5,i) end
     print("kiko demo",46,9,7)
     for r in all(rings) do circ(r.x,r.y,r.r,c) end
     circfill(x,y,4+sin(t())*1.5,c)
     print("arrows: move",40,114,6)
     print("z: color   x: ring",28,121,6)
    end
    """.trimIndent()
