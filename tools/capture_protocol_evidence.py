"""Read only P.O.D.S. bounded protocol metadata; never send a headset command."""

import argparse
from pathlib import Path
import re
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parent.parent
COMMAND = (
    'logcat -d -v raw -m 512 -T "$(($(date +%s)-120)).000" '
    "-s PODS-ProtocolEvidence:I '*:S'"
)
RECORD = re.compile(
    r"PODS_TRACE t=\d+ event=(?:begin reason=(?:ownership|head|swipe)|"
    r"(?:rx|tx_queued|tx_failed) kind=(?:"
    r"control id=(?:06|23) size=\d+ data=(?:-|[0-9A-F]{2}(?: [0-9A-F]{2}){0,3})|"
    r"head size=\d+ data=[0-9A-F]{2}(?: [0-9A-F]{2}){0,19}|"
    r"status size=\d+ data=-))"
)


def records(text):
    return [line for line in text.splitlines() if RECORD.fullmatch(line)]


def capture(adb, serial, output, seconds):
    if not 1 <= seconds <= 120:
        raise ValueError("Capture duration must be between 1 and 120 seconds")
    output = Path(output).resolve()
    if output.is_relative_to(ROOT):
        raise ValueError("Protocol evidence must be saved outside the repository")
    output.parent.mkdir(parents=True, exist_ok=True)
    deadline = time.monotonic() + seconds
    seen = set()
    with output.open("x", encoding="ascii") as stream:
        while len(seen) < 512:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                break
            result = subprocess.run(
                [adb, "-s", serial, "shell", COMMAND],
                capture_output=True, text=True, encoding="utf-8", errors="replace",
                check=True, timeout=min(8, remaining),
            )
            for line in records(result.stdout):
                if line not in seen and len(seen) < 512:
                    seen.add(line)
                    stream.write(line + "\n")
            stream.flush()
            time.sleep(min(1, max(0, deadline - time.monotonic())))
    if not seen:
        raise RuntimeError("No matching evidence found; no headset success can be inferred")
    return len(seen)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--seconds", default=30, type=int)
    args = parser.parse_args()
    try:
        count = capture(args.adb, args.serial, args.output, args.seconds)
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as error:
        print(f"Capture failed (any existing output may be partial): {error}", file=sys.stderr)
        return 1
    print(f"Saved {count} unique metadata records. This does not verify headphone behavior.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
