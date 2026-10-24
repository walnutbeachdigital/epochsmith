"""Rebuilds every Epochsmith texture. Run from the project root (Gradle must have set up the game once):
    python3 tools/make_all_textures.py

  make_textures_early.py   Primitive to Industrial blocks and items
  make_textures_mid.py     Electrical and Information blocks and items
  make_textures_future.py  Future blocks and items
  recolor_vanilla.py       ores, ingots, coke, and all armor and vanilla-style tools
"""
import runpy
import sys
from pathlib import Path

HERE = Path(__file__).parent
sys.path.insert(0, str(HERE))
for script in ("make_textures_early.py", "make_textures_mid.py", "make_textures_future.py", "recolor_vanilla.py"):
    runpy.run_path(str(HERE / script), run_name="__main__")
