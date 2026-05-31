from pathlib import Path

import imageio.v2 as imageio


def main() -> None:
    media_dir = Path("docs") / "media"
    frames = sorted(media_dir.glob("frame-*.png"))
    if not frames:
        raise SystemExit("No walkthrough frames found. Run capture-assets.ps1 first.")

    output = media_dir / "walkthrough.mp4"
    with imageio.get_writer(output, fps=8, codec="libx264", quality=8, macro_block_size=2) as writer:
        for frame in frames:
            writer.append_data(imageio.imread(frame))

    for frame in frames:
        frame.unlink(missing_ok=True)

    print(f"Created silent walkthrough video: {output}")


if __name__ == "__main__":
    main()
