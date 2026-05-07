from __future__ import annotations

import argparse
import re
from pathlib import Path


HEADING_RE = re.compile(r"^#{1,6}\s+(.*)$")
MARKDOWN_LINK_RE = re.compile(r"\[([^\]]+)\]\([^)]+\)")
STRIP_MARKDOWN_RE = re.compile(r"[*_`]+")
PAGE_SUFFIX_RE = re.compile(r"page-\d+-\d+$", re.IGNORECASE)


def clean_title(text: str) -> str:
    text = text.strip()
    match = MARKDOWN_LINK_RE.search(text)
    if match:
        text = match.group(1)
    text = STRIP_MARKDOWN_RE.sub("", text)
    text = re.sub(r"<sup>.*?</sup>", "", text)
    text = text.replace("<br>", " ").replace("<br/>", " ").replace("<br />", " ")
    text = re.sub(r"\s+", " ", text).strip()
    return text


def title_from_markdown(file_path: Path) -> str:
    try:
        with file_path.open("r", encoding="utf-8") as handle:
            for raw_line in handle:
                line = raw_line.strip()
                if not line:
                    continue
                heading = HEADING_RE.match(line)
                if heading:
                    return clean_title(heading.group(1)) or fallback_title(file_path.stem)
    except UnicodeDecodeError:
        with file_path.open("r", encoding="cp1252", errors="replace") as handle:
            for raw_line in handle:
                line = raw_line.strip()
                if not line:
                    continue
                heading = HEADING_RE.match(line)
                if heading:
                    return clean_title(heading.group(1)) or fallback_title(file_path.stem)

    return fallback_title(file_path.stem)


def fallback_title(stem: str) -> str:
    stem = PAGE_SUFFIX_RE.sub("", stem)
    stem = stem.replace("_", " ").replace("-", " ")
    stem = re.sub(r"\s+", " ", stem).strip()
    return stem or "Untitled"


def entry_label(path: Path) -> str:
    if path.is_file() and path.suffix.lower() == ".md":
        return title_from_markdown(path)

    candidate = path / f"{path.name}.md"
    if candidate.is_file():
        return title_from_markdown(candidate)

    return fallback_title(path.name)


def relative_markdown_link(target: Path, base_dir: Path) -> str:
    relative_path = target.relative_to(base_dir).as_posix()
    return relative_path


def build_level_lines(directory: Path, base_dir: Path, depth: int = 0) -> list[str]:
    lines: list[str] = []
    indent = "  " * depth

    child_directories = sorted(
        [item for item in directory.iterdir() if item.is_dir() and not item.name.startswith(".")],
        key=lambda item: item.name.lower(),
    )
    
    # Collect all markdown files WITHOUT any filtering based on directory names
    child_markdown_files = sorted(
        [
            item
            for item in directory.iterdir()
            if item.is_file()
            and item.suffix.lower() == ".md"
            and not item.name.lower().endswith("manifest.md")
        ],
        key=lambda item: item.name.lower(),
    )

    # Process directories first
    for child_directory in child_directories:
        manifest_file = child_directory / "manifest.md"
        link_target = manifest_file if manifest_file.is_file() else child_directory / f"{child_directory.name}.md"
        label = entry_label(child_directory)
        relative_link = relative_markdown_link(link_target, base_dir)
        lines.append(f"{indent}- [{label}]({relative_link})")

    # Then process all markdown files (including those with matching directory names)
    for markdown_file in child_markdown_files:
        label = entry_label(markdown_file)
        relative_link = relative_markdown_link(markdown_file, base_dir)
        lines.append(f"{indent}- [{label}]({relative_link})")

    return lines


def build_manifest(directory: Path, output_path: Path) -> None:
    title = entry_label(directory)
    lines = [f"# {title}", ""]
    lines.extend(build_level_lines(directory, directory))
    lines.append("")
    output_path.write_text("\n".join(lines), encoding="utf-8")


def generate_local_manifests(directory: Path, base_dir: Path) -> None:
    for child_directory in sorted(
        [item for item in directory.iterdir() if item.is_dir() and not item.name.startswith(".")],
        key=lambda item: item.name.lower(),
    ):
        generate_local_manifests(child_directory, base_dir)

    lines = [f"# {entry_label(directory)}", ""]
    lines.extend(build_level_lines(directory, base_dir))
    lines.append("")
    (directory / "manifest.md").write_text("\n".join(lines), encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate a Markdown tree manifest for a directory produced by mdsplit."
    )
    parser.add_argument("input_dir", type=Path, help="Root directory to scan")
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        help="Path to the root manifest Markdown file. Defaults to <input_dir>/manifest.md.",
    )
    parser.add_argument(
        "--root-only",
        action="store_true",
        help="Only write the root manifest file.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    input_dir = args.input_dir.resolve()

    if not input_dir.exists() or not input_dir.is_dir():
        raise SystemExit(f"Input directory not found: {input_dir}")

    output_path = args.output.resolve() if args.output else input_dir / "manifest.md"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    print(f"[LOG] Building root manifest: {output_path}", flush=True)
    build_manifest(input_dir, output_path)

    if not args.root_only:
        print(f"[LOG] Generating local manifests for all subdirectories...", flush=True)
        generate_local_manifests(input_dir, input_dir)
        print(f"[LOG] Local manifests complete", flush=True)
    else:
        print(f"[LOG] Skipping local manifests (--root-only flag set)", flush=True)

    print(f"Manifest written to: {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())