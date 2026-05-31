from pathlib import Path

from PIL import Image


def main() -> None:
    media_dir = Path("docs") / "media"
    frames = sorted(media_dir.glob("frame-*.png"))
    if not frames:
        raise SystemExit("No walkthrough frames found. Run capture-assets.ps1 first.")

    images = []
    for frame in frames:
        image = Image.open(frame).convert("RGB")
        # Adaptive palette keeps the dark JavaFX UI crisp while producing a README-friendly GIF.
        images.append(image.convert("P", palette=Image.Palette.ADAPTIVE, colors=256, dither=Image.Dither.NONE))

    output = media_dir / "walkthrough.gif"
    images[0].save(
        output,
        save_all=True,
        append_images=images[1:],
        duration=125,
        loop=0,
        optimize=False,
        disposal=2,
    )

    for frame in frames:
        frame.unlink(missing_ok=True)

    print(f"Created walkthrough GIF: {output}")


if __name__ == "__main__":
    main()
