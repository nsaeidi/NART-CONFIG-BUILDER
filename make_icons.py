from PIL import Image
from pathlib import Path

src = Image.open("icon-source.png").convert("RGBA")
sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

for density, size in sizes.items():
    folder = Path("app/src/main/res") / f"mipmap-{density}"
    folder.mkdir(parents=True, exist_ok=True)
    icon = src.resize((size, size), Image.Resampling.LANCZOS)
    icon.save(folder / "ic_launcher.png")

print("Android icons created successfully.")
