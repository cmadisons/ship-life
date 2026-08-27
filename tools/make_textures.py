#!/usr/bin/env python3
"""Draws every texture Ship Life adds, and writes the JSON that goes with them.

Six things you handle or press: the sponge and towel you do the dishes with,
the dish itself, the weed whacker and the lawn mower, and the elevator button.
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
}

ITEMS = {
    "sponge": SPONGE,
    "towel": TOWEL,
    "weed_whacker": WHACKER,
    "lawn_mower": MOWER,
}

BLOCKS = {
    "dish": DISH,
    "elevator_button": BUTTON,
}


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
        names["block.shiplife.%s" % name] = title(name)

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
