# Games in the demo

The demo plays its own small cart (`DemoCart.kt`, part of this repository, MIT) and these games by
other people, bundled unchanged with their licenses. Thank you to their authors.

| Game | Author | License | Source | Files |
|---|---|---|---|---|
| **Snowman**, "a snowman themed puzzle game made while avoiding relatives at Christmas" (its author's words) | [aquova](https://github.com/aquova) ([BBS](https://www.lexaloffle.com/bbs/?uid=27259)) | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | [aquova/pico8](https://github.com/aquova/pico8/tree/f3fe623567c3b2e437050a4b8e1f2491be783b03/snowman), commit `f3fe623` (2022-09-09) | `snowman.p8`, `snowman.LICENSE.txt` |
| **1492** | [A.C. Wright Design](https://github.com/acwright) | [MIT](https://opensource.org/license/mit), Copyright (c) 2024 A.C. Wright Design | [acwright/PICO-8](https://github.com/acwright/PICO-8/tree/6dce6d7324624acaf0b99bab526dd79d56906a78/1492), commit `6dce6d7` (2026-09-17) | `1492.p8`, `1492.LICENSE.txt` |

The files are in `shared/src/commonMain/composeResources/files/carts/`, byte for byte as in the
source repositories. The app shows each game's author and license in its game list.

## Considered, not bundled

Each of these is published under a permissive license, but part of it may belong to someone the
license does not cover. They can be added once their authors confirm.

- **Sixlets** (aquova, CC0): its screen shake comes from a 2016 BBS sample by Krystman
  ([thread](https://www.lexaloffle.com/bbs/?tid=28306)), posted with no license.
- **Sixlets II** (aquova, CC0): its starfield comes from PicoLate's snippet
  ([post](https://www.lexaloffle.com/bbs/?pid=62162)), licensed CC BY-NC-SA, whose terms would
  carry over to the game.
- **Bon Planto** (A.C. Wright Design, MIT): made by the team Infinite Token (sprites and sounds by
  Chris Thompson, levels by Daniel Heupel, programming by Jay Deaton and A.C. Wright, with River);
  the license names only A.C. Wright Design.
- **Dash** (A.C. Wright Design, MIT): credited in the cart "by zander and a.c. wright".

Left out for other reasons: remakes of other companies' games (Piconian, Rally-8, Quick Draw,
Huck Dunt, Wordle, Tetris, Pong), demos and unfinished prototypes.
