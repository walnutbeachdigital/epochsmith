"""Shared helpers for Epochsmith's placeholder texture scripts (standard library only)."""
import struct, zlib

TRANSPARENT = (0, 0, 0, 0)


def write_png(path, px):
    """Writes a 16x16 grid of (r, g, b, a) pixels to a PNG file."""
    raw = b"".join(b"\x00" + bytes(c for p in row for c in p) for row in px)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n"
                + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9))
                + chunk(b"IEND", b""))


def from_map(rows, palette):
    """Turns a character map (one string per row, '.' = transparent) into pixels using a palette dict."""
    rows = rows + ["." * 16] * (16 - len(rows))
    return [[palette.get(ch, TRANSPARENT) for ch in row.ljust(16, ".")] for row in rows]


# The ingot outline every metal ingot shares, so they all look like one set
INGOT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....OOOOOOO....",
    "....OHHHHHHHO...",
    "...OHMMMMMMMDO..",
    "..OHMMMMMMMMDO..",
    "..OMMMMMMMMDO...",
    "..ODDDDDDDDO....",
    "...OOOOOOOO.....",
]
