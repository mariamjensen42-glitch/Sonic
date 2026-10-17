#!/usr/bin/env python3
"""Build the font resources used by Sonic (C 方案, ADR-0002).

Sources (downloaded into .tmp/fonts if absent):
  Roboto Flex   https://github.com/google/fonts/ofl/robotoflex  (OFL-1.1)
  Material Symbols Rounded
                https://github.com/google/material-design-icons/variablefont
                (Apache-2.0)

Why static instances instead of a variable font at runtime: Compose 1.13 builds
resource typefaces with explicit FontVariation settings and the Android
Typeface.Builder rejects the subset on API 33 (the family silently falls back to
the system font, which renders the icon codepoints as missing-glyph boxes).
Instancing the axes here keeps rendering deterministic and still satisfies the
size budgets:

  Roboto Flex      regular / medium / bold, Latin subset   target < 150 KB
  Material Symbols rounded fill 0 / fill 1                 target < 500 KB

Run:  python scripts/subset-fonts.py
"""
import os
import subprocess
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, ".tmp", "fonts")
FONT_DIR = os.path.join(ROOT, "presentation", "src", "main", "res", "font")

ROBOTO_FLEX_URL = (
    "https://raw.githubusercontent.com/google/fonts/main/ofl/robotoflex/"
    "RobotoFlex%5BGRAD%2CXOPQ%2CXTRA%2CYOPQ%2CYTAS%2CYTDE%2CYTFI%2CYTLC%2CYTUC%2C"
    "opsz%2Cslnt%2Cwdth%2Cwght%5D.ttf"
)
SYMBOLS_URL = (
    "https://raw.githubusercontent.com/google/material-design-icons/master/"
    "variablefont/MaterialSymbolsRounded%5BFILL%2CGRAD%2Copsz%2Cwght%5D.ttf"
)

# Keep in sync with SymbolIconName in
# presentation/src/main/java/com/cycling/core/ui/components/M3SymbolIcon.kt
SYMBOL_CODEPOINTS = [
    0xE019, 0xE024, 0xE030, 0xE034, 0xE037, 0xE03B, 0xE03D, 0xE040, 0xE043,
    0xE044, 0xE045, 0xE04F, 0xE050, 0xE145, 0xE14D, 0xE162, 0xE164, 0xE176,
    0xE1B8, 0xE1C4, 0xE25D, 0xE26B, 0xE2C7, 0xE313, 0xE315, 0xE316, 0xE405,
    0xE429, 0xE5C4, 0xE5CD, 0xE5CE, 0xE5CF, 0xE5D4, 0xE5D5, 0xE65F, 0xE668,
    0xE6B1, 0xE6B8, 0xE73C, 0xE863, 0xE864, 0xE87E, 0xE8B3, 0xE8B8, 0xE8D5,
    0xE92E, 0xE945, 0xE9B2, 0xE9E4, 0xE9FC, 0xEB80, 0xEC0B, 0xEF7A, 0xF097,
    0xF0BE, 0xF0D3, 0xF159, 0xF8B6,
]

LATIN = "U+0020-007E,U+00A0-00FF,U+0100-017F,U+2000-206F,U+20AC,U+2212"
LAYOUT = "kern,liga,clig,calt,ccmp,locl,mark,mkmk,rlig"


def download(url, dest):
    if os.path.exists(dest) and os.path.getsize(dest) > 100_000:
        return dest
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    print(f"downloading {os.path.basename(dest)} ...")
    urllib.request.urlretrieve(url, dest)
    return dest


def run(*args):
    subprocess.run([sys.executable, "-m", *args], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def instance(src, axes, out):
    run("fontTools.varLib.instancer", src, *axes, "--no-overlap-flag", f"--output={out}")


def subset(src, unicodes, out, layout):
    run("fontTools.subset", src, f"--unicodes={unicodes}",
        f"--layout-features={layout}", "--no-hinting",
        "--name-IDs=1,2,3,4,5,6", "--name-languages=0x409",
        f"--output-file={out}")


def main():
    os.makedirs(FONT_DIR, exist_ok=True)
    flex = download(ROBOTO_FLEX_URL, os.path.join(CACHE, "RobotoFlex.ttf"))
    symbols = download(SYMBOLS_URL, os.path.join(CACHE, "MaterialSymbolsRounded.ttf"))
    tmp = os.path.join(CACHE, "out")
    os.makedirs(tmp, exist_ok=True)

    # Roboto Flex: every unused axis pinned, optical size 24 compromise for both
    # display and body, one static file per weight the type scale actually uses.
    fixed_axes = ["opsz=24", "wdth=100", "slnt=0", "GRAD=0", "XOPQ=96",
                  "YOPQ=79", "XTRA=468", "YTUC=712", "YTLC=514", "YTAS=750",
                  "YTDE=-203", "YTFI=738"]
    for weight, name in ((400, "regular"), (500, "medium"), (700, "bold")):
        instanced = os.path.join(tmp, f"RobotoFlex-{name}.ttf")
        instance(flex, [f"wght={weight}", *fixed_axes], instanced)
        subset(instanced, LATIN, os.path.join(FONT_DIR, f"roboto_flex_{name}.ttf"), LAYOUT)

    # Material Symbols Rounded: FILL is the only axis the UI toggles, so ship
    # one static file per state.
    unicodes = ",".join(f"U+{cp:04X}" for cp in SYMBOL_CODEPOINTS)
    for fill, name in ((0, "material_symbols_rounded"), (1, "material_symbols_rounded_filled")):
        instanced = os.path.join(tmp, f"symbols-{fill}.ttf")
        instance(symbols, [f"FILL={fill}", "GRAD=0", "opsz=24", "wght=400"], instanced)
        subset(instanced, unicodes, os.path.join(FONT_DIR, f"{name}.ttf"), "*")

    total = 0
    for name in sorted(os.listdir(FONT_DIR)):
        if name.endswith(".ttf"):
            size = os.path.getsize(os.path.join(FONT_DIR, name))
            total += size
            print(f"{name}: {size} bytes ({size / 1024:.1f} KB)")
    print(f"total: {total} bytes ({total / 1024:.1f} KB)")


if __name__ == "__main__":
    main()
