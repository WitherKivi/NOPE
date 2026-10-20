"""Import the pinned, official Unicode catalog. No network access at app runtime."""
from pathlib import Path
import hashlib
import json
import re
import urllib.request

root = Path(__file__).resolve().parents[1]
url = "https://www.unicode.org/Public/17.0.0/emoji/emoji-test.txt"
source = root / "vendor/unicode-17-emoji-test.txt"
license_file = root / "app/src/resources/assets/licenses/UNICODE-LICENSE.txt"
if not source.exists():
    source.write_bytes(urllib.request.urlopen(url, timeout=45).read())
if not license_file.exists():
    license_file.parent.mkdir(parents=True, exist_ok=True)
    license_file.write_bytes(urllib.request.urlopen("https://www.unicode.org/license.txt", timeout=45).read())
groups = []
for line in source.read_text(encoding="utf-8").splitlines():
    if line.startswith("# group: "):
        group = {"name": line.removeprefix("# group: "), "items": []}
        groups.append(group)
    elif "; fully-qualified" in line:
        points, _, comment = line.partition(";")
        match = re.search(r"#\s+\S+\s+E[\d.]+\s+(.+)", comment)
        group["items"].append(["".join(chr(int(p, 16)) for p in points.split()), match[1]])
groups = [g for g in groups if g["items"]]
catalog = {"version": "17.0", "source": url, "source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(), "groups": groups}
destination = root / "app/src/resources/assets/emoji/catalog.json"
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(catalog, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
print("Unicode", catalog["version"], "categories", len(groups), "emoji", sum(len(g["items"]) for g in groups))
