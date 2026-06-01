from pathlib import Path
from textwrap import wrap

from PIL import Image, ImageDraw, ImageFont


CAPTION_HEIGHT = 96
FRAME_DURATION_MS = 125


def caption_for_frame(index: int) -> str:
    if index <= 10:
        return "Overview: all four elevator types are active, and the event log records each request."
    if index <= 16:
        return "Same-direction scenario: a passenger starts at Ground and selects floor 5."
    if index <= 26:
        return "A second passenger on floor 2 requests Up, so the elevator picks them up on the way."
    if index <= 38:
        return "The elevator drops the floor-2 passenger at floor 4 first, then continues to floor 5."
    if index <= 48:
        return "Opposite-direction scenario: another passenger starts at Ground and selects floor 5."
    if index <= 58:
        return "A floor-2 Down request is assigned, but the elevator does not stop while traveling Up."
    return "After the floor-5 drop-off, the elevator returns to floor 2 and then carries the passenger to Ground."


def load_font(name: str, size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    font_path = Path("C:/Windows/Fonts") / name
    if font_path.exists():
        return ImageFont.truetype(str(font_path), size)
    return ImageFont.load_default()


def add_caption(image: Image.Image, caption: str, frame_index: int) -> Image.Image:
    width, height = image.size
    output = Image.new("RGB", (width, height + CAPTION_HEIGHT), (15, 20, 27))
    output.paste(image, (0, 0))

    draw = ImageDraw.Draw(output)
    top = height
    draw.rectangle((0, top, width, height + CAPTION_HEIGHT), fill=(18, 28, 38))
    draw.line((0, top, width, top), fill=(68, 84, 102), width=2)

    title_font = load_font("segoeuib.ttf", 24)
    body_font = load_font("segoeui.ttf", 22)
    draw.text((24, top + 14), "Walkthrough", fill=(245, 248, 251), font=title_font)

    wrapped = "\n".join(wrap(caption, width=92))
    draw.text((24, top + 48), wrapped, fill=(216, 226, 238), font=body_font)
    return output


def main() -> None:
    media_dir = Path("docs") / "media"
    frames = sorted(media_dir.glob("frame-*.png"))
    if not frames:
        raise SystemExit("No walkthrough frames found. Run capture-assets.ps1 first.")

    images = []
    for index, frame in enumerate(frames, start=1):
        image = Image.open(frame).convert("RGB")
        image = add_caption(image, caption_for_frame(index), index)
        # Adaptive palette keeps the dark JavaFX UI crisp while producing a README-friendly GIF.
        images.append(image.convert("P", palette=Image.Palette.ADAPTIVE, colors=256, dither=Image.Dither.NONE))

    output = media_dir / "walkthrough.gif"
    images[0].save(
        output,
        save_all=True,
        append_images=images[1:],
        duration=FRAME_DURATION_MS,
        loop=0,
        optimize=False,
        disposal=2,
    )

    for frame in frames:
        frame.unlink(missing_ok=True)

    print(f"Created walkthrough GIF: {output}")


if __name__ == "__main__":
    main()
