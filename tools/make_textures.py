#!/usr/bin/env python3
"""Draws every texture Ship Life adds, and writes the JSON that goes with them.

That includes the loot tables. A block with no loot table drops nothing when
you break it, which is how the dishes were vanishing off the counter rather
than going into your pocket.

Everything you handle or press: the sponge and towel you do the dishes with,
the dish itself, the weed whacker, the lawn mower, the plunger, one of Ben's
bombs, and the elevator button.
They started out as vanilla items wearing a name, which reads fine in a list
and badly in your hand -- a towel should not be a block of wool.

Everything here is 16x16 and drawn as string art, one character a pixel, so a
change is a change to the picture rather than to a paint program.

Run from the project root:  python3 tools/make_textures.py
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pixel  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/shiplife")

# ------------------------------------------------------------------ palettes

YELLOW_DK = (196, 150, 26)
YELLOW = (240, 200, 60)
YELLOW_LT = (255, 232, 130)
WHITE = (240, 242, 248)
WHITE_DK = (188, 196, 212)
BLUE = (86, 148, 220)
BLUE_DK = (52, 100, 164)
GREY = (150, 156, 168)
GREY_DK = (96, 102, 116)
GREY_LT = (198, 204, 216)
GREEN = (92, 170, 78)
GREEN_DK = (56, 118, 50)
RED = (198, 66, 62)
RED_DK = (140, 40, 40)
WOOD = (156, 110, 62)
WOOD_DK = (110, 74, 40)
STEEL = (176, 182, 196)
STEEL_DK = (110, 116, 132)
BLACK = (34, 36, 44)
LIME = (140, 230, 110)
LEAF_CLOTH = (74, 126, 62)

# ---------------------------------------------------------------- the pictures

SPONGE = [
    "................",
    "................",
    "..oooooooooooo..",
    ".oyyhyyyhyyyhyo.",
    ".oyhyyyhyyyhyyo.",
    ".oyyyhyyyhyyyho.",
    ".ohyyyyhyyyhyyo.",
    ".oyyhyyyyhyyyyo.",
    ".oyyyyhyyyhyyho.",
    ".ohyyyhyyyyhyyo.",
    ".oyyyhyyyhyyyyo.",
    ".oyhyyyyhyyyhyo.",
    ".oyyyhyyyhyyyyo.",
    "..oooooooooooo..",
    "................",
    "................",
]

TOWEL = [
    "................",
    "................",
    "..wwwwwwwwwwww..",
    "..wwwwwwwwwwww..",
    "..wbbbbbbbbbbw..",
    "..wwwwwwwwwwww..",
    "..wwwwwwwwwwww..",
    "..wbbbbbbbbbbw..",
    "..wwwwwwwwwwww..",
    "..dwwwwwwwwwwd..",
    "...dwwwwwwwwd...",
    "....dwwwwwwd....",
    ".....dwwwwd.....",
    "......dwwd......",
    ".......dd.......",
    "................",
]

DISH = [
    "................",
    "................",
    "................",
    "....wwwwwwww....",
    "..wwwwwwwwwwww..",
    ".wwwbbbbbbbbww..",
    ".wwbwwwwwwwwbw..",
    ".wwbwwwwwwwwbw..",
    ".wwbwwwwwwwwbw..",
    "..wwbbbbbbbbww..",
    "..wwwwwwwwwwww..",
    "...dwwwwwwwwd...",
    "....dddddddd....",
    "................",
    "................",
    "................",
]

WHACKER = [
    "..........ss....",
    ".........ss.....",
    "........ss......",
    ".......ss.......",
    "......ss........",
    ".....ss.........",
    "....ss..........",
    "...ss...........",
    "..sk............",
    ".skk............",
    "gkkg............",
    "ggllgg..........",
    ".gllllg.........",
    "..gllg..........",
    "...gg...........",
    "................",
]

MOWER = [
    "................",
    "..........tt....",
    ".........tt.....",
    "........tt......",
    ".......tt.......",
    "......tt........",
    "..rrrrrr........",
    ".rrrrrrrr.......",
    ".rggggggr.......",
    ".rrrrrrrr.......",
    "..ssssss........",
    ".kkkkkkkk.......",
    ".kbbkkkbbk......",
    ".kbbkkkbbk......",
    "..kkkkkkk.......",
    "................",
]

PLUNGER = [
    "................",
    "..........tt....",
    ".........tt.....",
    "........tt......",
    ".......tt.......",
    "......tt........",
    ".....tt.........",
    "....tt..........",
    "...tt...........",
    "..rrrrrr........",
    ".rrxxxxrr.......",
    ".rxxxxxxr.......",
    ".rxxxxxxr.......",
    "..rxxxxr........",
    "...rrrr.........",
    "................",
]

BOMB = [
    "................",
    "..........ee....",
    ".........e.e....",
    "..........ee....",
    ".........t......",
    "....ccc.t.......",
    "...cnnnce.......",
    "..cnncnnnc......",
    "..neknnncnn.....",
    "..nknnecnnn.....",
    "..cnnnnnncn.....",
    "..cnennnnce.....",
    "...cnnnncn......",
    "....ccecc.......",
    "................",
    "................",
]


ARMOUR = [
    "................",
    "..cc........cc..",
    ".ceecccccccceec.",
    ".cevvvvvvvvvvec.",
    "cccvevvvvvevvccc",
    "cevvvvvevvvvvvec",
    "ceevvvvvvvvevvec",
    "cccvvevvvvvvvccc",
    ".cevvvvvevvvvec.",
    ".cvvevvvvvvevc..",
    ".cvvvvvvevvvvc..",
    ".ceevvvvvvveec..",
    "..cvvvvvvvvvc...",
    "..cceccccceecc..",
    "...cc.....cc....",
    "................",
]

BOOTS = [
    "................",
    "................",
    "................",
    "................",
    "..cvvc....cvvc..",
    ".cveevc..cveevc.",
    ".cvvvvc..cvvvvc.",
    ".cvevvc..cvvevc.",
    ".cvvvvc..cvvvvc.",
    "ccvvvvcc.cvvvvcc",
    "cvvvvvvccvvvvvvc",
    "cevvvvvccevvvvvc",
    "cccccccc.ccccccc",
    "................",
    "................",
    "................",
]

HELMET = [
    "................",
    "................",
    "....cccccccc....",
    "..ccveevvevcc...",
    ".ccvvvvvvvvvcc..",
    ".cvvevvvvvevvc..",
    ".cvvvvvvvvvvvc..",
    ".cvevvvvvvvvec..",
    ".cvvvvvvvvvvvc..",
    ".cvvc.cccc.cvc..",
    ".cvvc......cvc..",
    ".cevc......cec..",
    ".ccc........cc..",
    "................",
    "................",
    "................",
]

LEGGINGS = [
    "................",
    "................",
    "..cccccccccccc..",
    ".cveevvvvvveevc.",
    ".cvvvvvvvvvvvvc.",
    ".cvvevvvvvvevvc.",
    ".cvvvvvvvvvvvvc.",
    ".cvvvvcccvvvvvc.",
    ".cvvvc...cvvvvc.",
    ".cevvc...cvvevc.",
    ".cvvvc...cvvvvc.",
    ".cvvvc...cvvvvc.",
    ".ccecc...ccecc..",
    "................",
    "................",
    "................",
]

BUTTON = [
    "................",
    "................",
    "...gggggggggg...",
    "..gsssssssssg...",
    "..gskkkkkkksg...",
    "..gskllllkksg...",
    "..gsklaaalkbg...",
    "..gskla.alkbg...",
    "..gskla.alkbg...",
    "..gsklaaalkbg...",
    "..gskllllkksg...",
    "..gskkkkkkksg...",
    "..gsssssssssg...",
    "...gggggggggg...",
    "................",
    "................",
]

PALETTE = {
    "y": YELLOW, "h": YELLOW_DK, "o": YELLOW_LT,
    "w": WHITE, "d": WHITE_DK, "b": BLUE,
    "g": GREY, "s": GREY_LT, "k": GREY_DK,
    "l": STEEL, "t": WOOD, "r": RED, "a": LIME,
    "x": RED_DK, "n": BLACK, "e": LIME, "c": GREEN, "v": GREEN_DK,
}

ITEMS = {
    "sponge": SPONGE,
    "towel": TOWEL,
    "weed_whacker": WHACKER,
    "lawn_mower": MOWER,
    "plunger": PLUNGER,
    "bomb": BOMB,
    "ben_armour": ARMOUR,
    "ben_boots": BOOTS,
    "ben_helmet": HELMET,
    "ben_leggings": LEGGINGS,
}

BLOCKS = {
    "dish": DISH,
    "elevator_button": BUTTON,
}


def leafy(width, height):
    """The armour as it looks on you: green cloth with leaves growing over it.

    A worn armour layer is 64x32 and laid out like a skin, and only the parts
    the model uses are ever seen -- so rather than pick those out by hand the
    whole sheet is covered and the model takes what it needs. The leaves are
    scattered by a fixed sum of the coordinates rather than at random, so the
    picture comes out the same every time this is run.
    """
    grid = pixel.blank(width, height)
    for y in range(height):
        for x in range(width):
            speck = (x * 73 + y * 151 + (x * y) % 17) % 23
            if speck < 3:
                colour = LIME
            elif speck < 8:
                colour = GREEN
            elif speck < 12:
                colour = GREEN_DK
            else:
                colour = LEAF_CLOTH
            pixel.px(grid, x, y, colour)
    # Vines running down it, so it reads as growing rather than as noise.
    for y in range(height):
        for x in ((y * 5) % width, (y * 11 + 7) % width):
            pixel.px(grid, x, y, GREEN_DK)
            if y % 3 == 0:
                pixel.px(grid, x + 1, y, LIME)
    return grid


def draw(rows):
    grid = pixel.blank(16, 16)
    pixel.art(grid, 0, 0, rows, PALETTE)
    return grid


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def main():
    names = {}

    for name, rows in ITEMS.items():
        pixel.write_png(os.path.join(ASSETS, "textures/item/%s.png" % name), draw(rows))
        write(os.path.join(ASSETS, "models/item/%s.json" % name), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "shiplife:item/%s" % name},
        })
        write(os.path.join(ASSETS, "items/%s.json" % name), {
            "model": {"type": "minecraft:model", "model": "shiplife:item/%s" % name},
        })
        names["item.shiplife.%s" % name] = title(name)

    for name, rows in BLOCKS.items():
        pixel.write_png(os.path.join(ASSETS, "textures/block/%s.png" % name), draw(rows))
        write(os.path.join(ASSETS, "blockstates/%s.json" % name), {
            "variants": {"": {"model": "shiplife:block/%s" % name}},
        })
        write(os.path.join(ASSETS, "models/block/%s.json" % name), {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": "shiplife:block/%s" % name},
        })
        write(os.path.join(ASSETS, "models/item/%s.json" % name), {
            "parent": "shiplife:block/%s" % name,
        })
        write(os.path.join(ASSETS, "items/%s.json" % name), {
            "model": {"type": "minecraft:model", "model": "shiplife:block/%s" % name},
        })
        # Break it and you get it: without a loot table a block drops nothing.
        write(os.path.join(ROOT, "src/main/resources/data/shiplife/loot_table/blocks/%s.json" % name), {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": "shiplife:%s" % name}],
            }],
        })
        names["block.shiplife.%s" % name] = title(name)

    # Ben's armour as it looks worn, plus the file that points the game at it.
    pixel.write_png(
        os.path.join(ASSETS, "textures/entity/equipment/humanoid/ben_armour.png"),
        leafy(64, 32))
    pixel.write_png(
        os.path.join(ASSETS, "textures/entity/equipment/humanoid_leggings/ben_armour.png"),
        leafy(64, 32))
    write(os.path.join(ASSETS, "equipment/ben_armour.json"), {
        "layers": {
            "humanoid": [{"texture": "shiplife:ben_armour"}],
            "humanoid_leggings": [{"texture": "shiplife:ben_armour"}],
        },
    })

    lang = os.path.join(ASSETS, "lang/en_us.json")
    existing = {}
    if os.path.exists(lang):
        with open(lang) as handle:
            existing = json.load(handle)
    existing.update(names)
    write(lang, dict(sorted(existing.items())))

    print("Drew %d items and %d blocks." % (len(ITEMS), len(BLOCKS)))


def title(name):
    return " ".join(word.capitalize() for word in name.split("_"))


if __name__ == "__main__":
    main()
