#!/usr/bin/env python3
"""Draws a skin for each person on the ship.

Charlie, Ben, Izzy and the two staff on floor 1 are people you can see and
talk to, and a person needs a face. These are ordinary 64x64 Minecraft skins,
laid out the way every skin is -- head, body, arms, legs, each with a front, a
back and four sides -- so they hang on the same model the player does. What
tells them apart is colour: the clothes, the hair, the skin tone. Nothing
about the shape of them changes, because a person is a person.

Run from the project root:  python3 tools/make_skins.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pixel  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT = os.path.join(ROOT, "src/main/resources/assets/shiplife/textures/entity/person")

# name: (skin, hair, shirt, sleeve, trousers, shoes)
PEOPLE = {
    "charlie": ((222, 178, 140), (58, 44, 36), (36, 62, 110), (36, 62, 110),
                (38, 40, 52), (28, 28, 32)),
    "ben": ((198, 148, 108), (92, 60, 34), (76, 140, 72), (76, 140, 72),
            (74, 68, 60), (48, 40, 32)),
    "izzy": ((156, 112, 78), (28, 24, 26), (168, 72, 116), (168, 72, 116),
             (52, 56, 78), (36, 34, 40)),
    "staff_one": ((236, 200, 164), (204, 158, 72), (216, 220, 228), (216, 220, 228),
                  (44, 46, 58), (30, 30, 34)),
    "staff_two": ((142, 100, 72), (36, 30, 28), (216, 220, 228), (216, 220, 228),
                  (44, 46, 58), (30, 30, 34)),
    "cook": ((226, 186, 150), (62, 48, 40), (244, 246, 250), (244, 246, 250),
             (208, 210, 216), (54, 50, 46)),
    # Gus on floor 4: chef's whites, an apron, and a cook's forearms.
    "cook": ((214, 168, 126), (48, 40, 34), (238, 238, 234), (238, 238, 234),
             (206, 206, 200), (52, 44, 38)),
}


def box(grid, x, y, w, h, d, side, front, top):
    """One cuboid, laid out the way a skin lays one out.

    Left, front, right and back run along the row; the top and bottom caps sit
    above them. Every skin in the game is this shape, which is why a picture
    drawn here fits the model without any measuring.
    """
    pixel.fill(grid, x, y, d, h, top)                 # top cap
    pixel.fill(grid, x + d, y, w, h, top)             # bottom cap
    pixel.fill(grid, x, y + h, d, d, side)            # placeholder, redrawn below


def part(grid, x, y, w, d, h, side, front, top):
    """Head, body, arm or leg: caps on the top row, then the four sides."""
    pixel.fill(grid, x + d, y, w, d, top)             # top
    pixel.fill(grid, x + d + w, y, w, d, top)         # bottom
    pixel.fill(grid, x, y + d, d, h, side)            # right side
    pixel.fill(grid, x + d, y + d, w, h, front)       # front
    pixel.fill(grid, x + d + w, y + d, d, h, side)    # left side
    pixel.fill(grid, x + d + w + d, y + d, w, h, front)   # back


def face(grid, skin, hair):
    """Eyes and a mouth on the front of the head, and hair over the top."""
    pixel.fill(grid, 8, 0, 16, 8, hair)               # top of the head
    pixel.fill(grid, 8, 8, 8, 2, hair)                # fringe, right side
    pixel.fill(grid, 16, 8, 8, 2, hair)               # fringe, front
    pixel.fill(grid, 24, 8, 8, 2, hair)               # fringe, left
    pixel.fill(grid, 32, 8, 8, 2, hair)               # fringe, back
    for x in (18, 21):
        pixel.fill(grid, x, 12, 2, 1, (255, 255, 255))
        pixel.px(grid, x + 1, 12, (40, 44, 70))
    pixel.fill(grid, 19, 14, 3, 1, pixel.shade(skin, 0.72))


def draw(colours):
    skin, hair, shirt, sleeve, trousers, shoes = colours
    grid = pixel.blank(64, 64)

    # Head, body, arms and legs, in the places a skin keeps them.
    part(grid, 0, 0, 8, 8, 8, skin, skin, hair)                       # head
    part(grid, 16, 16, 8, 4, 12, shirt, shirt, shirt)                 # body
    part(grid, 40, 16, 4, 4, 12, sleeve, sleeve, skin)                # right arm
    part(grid, 32, 48, 4, 4, 12, sleeve, sleeve, skin)                # left arm
    part(grid, 0, 16, 4, 4, 12, trousers, trousers, trousers)         # right leg
    part(grid, 16, 48, 4, 4, 12, trousers, trousers, trousers)        # left leg

    # Hands and shoes, so they are not one colour top to bottom.
    pixel.fill(grid, 44, 26, 4, 2, skin)
    pixel.fill(grid, 36, 58, 4, 2, skin)
    pixel.fill(grid, 4, 26, 4, 2, shoes)
    pixel.fill(grid, 20, 58, 4, 2, shoes)

    face(grid, skin, hair)
    return grid


def main():
    for name, colours in PEOPLE.items():
        pixel.write_png(os.path.join(OUT, "%s.png" % name), draw(colours))
    print("Drew %d people." % len(PEOPLE))


if __name__ == "__main__":
    main()
