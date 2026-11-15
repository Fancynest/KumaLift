#!/usr/bin/env python3
"""
Upscale exercise GIFs (180x180, 256-color dithered) into crisp sprite-sheet WebP files.

For every GIF it:
  1. decodes all frames (with per-frame delay),
  2. removes GIF dithering noise lightly,
  3. upscales each frame with a neural super-resolution model (FSRCNN x3 by default, fast on CPU),
  4. packs the frames into ONE grid sprite sheet (WebP, white background, no alpha),
  5. writes manifest.json with frame size, grid layout and per-frame delays.

Usage (from repo root):
  pip install opencv-contrib-python numpy pillow
  python tools/upscale_gifs.py --src composeApp/src/androidMain/assets/videos \
                               --out composeApp/src/androidMain/assets/anim \
                               --scale 3 --quality 88
  (use --limit 5 to test quickly, --model edsr for slower but sharper results)

NOTE: The source media is (c) Gym visual. Upscaled files are derivative works; the same license
requirements apply before any public release.
"""
import argparse, json, math, os, sys, urllib.request
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

MODEL_URLS = {
    ("fsrcnn", 2): "https://raw.githubusercontent.com/Saafke/FSRCNN_Tensorflow/master/models/FSRCNN_x2.pb",
    ("fsrcnn", 3): "https://raw.githubusercontent.com/Saafke/FSRCNN_Tensorflow/master/models/FSRCNN_x3.pb",
    ("fsrcnn", 4): "https://raw.githubusercontent.com/Saafke/FSRCNN_Tensorflow/master/models/FSRCNN_x4.pb",
    ("edsr", 2): "https://raw.githubusercontent.com/Saafke/EDSR_Tensorflow/master/models/EDSR_x2.pb",
    ("edsr", 3): "https://raw.githubusercontent.com/Saafke/EDSR_Tensorflow/master/models/EDSR_x3.pb",
    ("edsr", 4): "https://raw.githubusercontent.com/Saafke/EDSR_Tensorflow/master/models/EDSR_x4.pb",
}

def load_model(name: str, scale: int, cache_dir: Path):
    url = MODEL_URLS.get((name, scale))
    if not url:
        sys.exit(f"Unsupported model/scale: {name} x{scale}")
    cache_dir.mkdir(parents=True, exist_ok=True)
    path = cache_dir / Path(url).name
    if not path.exists():
        print(f"Downloading {url}")
        urllib.request.urlretrieve(url, path)
    sr = cv2.dnn_superres.DnnSuperResImpl_create()
    sr.readModel(str(path))
    sr.setModel(name, scale)
    return sr

def read_gif(path: Path):
    im = Image.open(path)
    frames, delays = [], []
    for i in range(im.n_frames):
        im.seek(i)
        frames.append(cv2.cvtColor(np.array(im.convert("RGB")), cv2.COLOR_RGB2BGR))
        d = im.info.get("duration", 0)
        delays.append(int(d) if d and d > 10 else 100)  # same fallback as the app player
    return frames, delays

def process(path: Path, out_dir: Path, sr, scale: int, quality: int, denoise: bool):
    frames, delays = read_gif(path)
    ups = []
    for f in frames:
        if denoise:
            f = cv2.fastNlMeansDenoisingColored(f, None, 3, 3, 5, 15)  # light: only kills dither noise
        up = sr.upsample(f)
        ups.append(np.clip(up, 0, 255).astype(np.uint8))
    h, w = ups[0].shape[:2]
    n = len(ups)
    cols = min(n, 4)
    rows = math.ceil(n / cols)
    sheet = np.full((rows * h, cols * w, 3), 255, np.uint8)
    for i, u in enumerate(ups):
        r, c = divmod(i, cols)
        sheet[r * h:(r + 1) * h, c * w:(c + 1) * w] = u
    out = out_dir / f"{path.stem}.webp"
    Image.fromarray(cv2.cvtColor(sheet, cv2.COLOR_BGR2RGB)).save(out, "WEBP", quality=quality, method=4)
    return {"frameW": w, "frameH": h, "cols": cols, "rows": rows, "count": n, "delays": delays}, out.stat().st_size

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", required=True, type=Path)
    ap.add_argument("--out", required=True, type=Path)
    ap.add_argument("--scale", type=int, default=3, choices=[2, 3, 4])
    ap.add_argument("--model", default="fsrcnn", choices=["fsrcnn", "edsr"])
    ap.add_argument("--quality", type=int, default=88)
    ap.add_argument("--limit", type=int, default=0, help="process only the first N GIFs (testing)")
    ap.add_argument("--no-denoise", action="store_true")
    ap.add_argument("--resume", action="store_true", help="skip GIFs that already have an output")
    args = ap.parse_args()

    gifs = sorted(args.src.glob("*.gif"))
    if args.limit:
        gifs = gifs[: args.limit]
    args.out.mkdir(parents=True, exist_ok=True)
    manifest_path = args.out / "manifest.json"
    manifest = json.loads(manifest_path.read_text()) if (args.resume and manifest_path.exists()) else {}
    sr = load_model(args.model, args.scale, args.out.parent / ".sr_models")

    total = 0
    for i, g in enumerate(gifs, 1):
        if args.resume and g.stem in manifest and (args.out / f"{g.stem}.webp").exists():
            continue
        meta, size = process(g, args.out, sr, args.scale, args.quality, not args.no_denoise)
        manifest[g.stem] = meta
        total += size
        if i % 25 == 0 or i == len(gifs):
            manifest_path.write_text(json.dumps(manifest, separators=(",", ":")))
            print(f"[{i}/{len(gifs)}] {g.stem} -> {size/1024:.0f} KB (running total {total/1048576:.1f} MB)")
    manifest_path.write_text(json.dumps(manifest, separators=(",", ":")))
    print(f"Done. {len(gifs)} files, {total/1048576:.1f} MB written to {args.out}")

if __name__ == "__main__":
    main()
