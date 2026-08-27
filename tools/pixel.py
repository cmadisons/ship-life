#!/usr/bin/env python3
"""Shared pixel-pushing helpers for the texture scripts.

No image library is installed on this machine, so PNGs are read and written by
hand here (zlib + struct) and every other tool in this folder imports from this
module rather than repeating the codec.

A "grid" throughout is a list of rows, each row a list of (r, g, b, a) tuples.
"""

import os
import struct
import zlib

TRANSPARENT = (0, 0, 0, 0)


# --------------------------------------------------------------- png decoding

def read_png(data):
    """Decode a PNG into (width, height, grid).

    Handles 8-bit RGB and RGBA plus palette images at 1, 2, 4 or 8 bits per
    pixel -- between them that covers everything Mojang ships. Spawn eggs in
    particular are packed at a low bit depth, which is why the unpacking below
    exists.
    """
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a png"
    pos, idat, width, height, channels, color, depth = 8, b"", 0, 0, 4, 6, 8
    palette, trns = [], []

    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        if tag == b"IHDR":
            width, height, depth, color = struct.unpack(">IIBB", body[:10])
            assert depth in (1, 2, 4, 8), "unsupported bit depth %d" % depth
            assert color == 3 or depth == 8, "only palette pngs may be sub-8-bit"
            channels = {2: 3, 3: 1, 6: 4}[color]
        elif tag == b"PLTE":
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif tag == b"tRNS":
            trns = list(body)
        elif tag == b"IDAT":
            idat += body
        elif tag == b"IEND":
            break
        pos += 12 + length

    raw = zlib.decompress(idat)
    stride = (width * channels * depth + 7) // 8
    # Row filters work on bytes, so sub-8-bit images compare one byte back.
    step = max(1, channels * depth // 8)

    rows, prev, at = [], bytearray(stride), 0
    for _ in range(height):
        filt = raw[at]
        line = bytearray(raw[at + 1:at + 1 + stride])
        at += 1 + stride

        # Undo the per-row filter. See the PNG spec's five filter types.
        for i in range(stride):
            a = line[i - step] if i >= step else 0
            b = prev[i]
            c = prev[i - step] if i >= step else 0
            if filt == 1:
                line[i] = (line[i] + a) & 0xFF
            elif filt == 2:
                line[i] = (line[i] + b) & 0xFF
            elif filt == 3:
                line[i] = (line[i] + (a + b) // 2) & 0xFF
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pred) & 0xFF
        prev = line

        row = []
        for x in range(width):
            if color == 3:
                if depth == 8:
                    idx = line[x]
                else:
                    per_byte = 8 // depth
                    shift = 8 - depth * (x % per_byte + 1)
                    idx = (line[x // per_byte] >> shift) & ((1 << depth) - 1)
                r, g, b = palette[idx]
                a = trns[idx] if idx < len(trns) else 255
            else:
                px = line[x * channels:(x + 1) * channels]
                r, g, b, a = px[0], px[1], px[2], px[3] if channels == 4 else 255
            row.append((r, g, b, a))
        rows.append(row)
    return width, height, rows


# --------------------------------------------------------------- png encoding

def write_png(path, grid):
    """Write a grid out as a 32-bit RGBA PNG, creating folders as needed."""
    height, width = len(grid), len(grid[0])
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *px) for px in row) for row in grid)

    def chunk(tag, body):
        blob = tag + body
        return struct.pack(">I", len(body)) + blob + struct.pack(">I", zlib.crc32(blob))

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(b"\x89PNG\r\n\x1a\n"
                 + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
                 + chunk(b"IDAT", zlib.compress(raw, 9))
                 + chunk(b"IEND", b""))


# ------------------------------------------------------------------- painting

def blank(width, height):
    return [[TRANSPARENT for _ in range(width)] for _ in range(height)]


def px(grid, x, y, color):
    """Set one pixel, ignoring anything that lands outside the canvas."""
    if 0 <= y < len(grid) and 0 <= x < len(grid[0]):
        grid[y][x] = color if len(color) == 4 else color + (255,)


def fill(grid, x, y, w, h, color):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            px(grid, xx, yy, color)


def art(grid, x, y, rows, palette):
    """Stamp string-art onto the grid.

    Each character is looked up in `palette`; a character missing from it (by
    convention '.') leaves whatever is underneath untouched, so shapes can be
    layered on top of each other.
    """
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch in palette:
                px(grid, x + dx, y + dy, palette[ch])


def shade(color, factor):
    """Lighten (>1) or darken (<1) a colour, staying inside 0-255."""
    return tuple(max(0, min(255, int(c * factor))) for c in color[:3])


def lerp(c1, c2, t):
    return tuple(int(round(a + (b - a) * t)) for a, b in zip(c1[:3], c2[:3]))


def recolour(grid, dark, mid, light, highlight=None):
    """Remap hue while keeping each pixel's original brightness.

    This is what lets the chests keep every bit of Mojang's shading while
    changing colour -- brightness carries the detail, hue carries the identity.
    """
    out = []
    for row in grid:
        new = []
        for r, g, b, a in row:
            if a == 0:
                new.append(TRANSPARENT)
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            if highlight is not None and lum > 0.88:
                c = highlight
            elif lum < 0.5:
                c = lerp(dark, mid, lum / 0.5)
            else:
                c = lerp(mid, light, (lum - 0.5) / 0.5)
            new.append(tuple(c) + (a,))
        out.append(new)
    return out
