import json
import sys

if __name__ == "__main__":
    if len(sys.argv) < 2:
        raise AssertionError("Missing inputfile")

    with open(sys.argv[1]) as r:
        content = r.read().strip()

        # Minify json if possible
        try:
            parsed = json.loads(content)
            content = json.dumps(parsed, separators=(',', ':'))
        except ValueError:
            pass
        print(content.replace("\n", "\\\\n"), end="")
