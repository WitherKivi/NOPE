"""Assert the shipped catalog preserves every qualified official sequence and category."""
from pathlib import Path
import hashlib
import json

root = Path(__file__).resolve().parents[1]
source = root / "vendor/unicode-17-emoji-test.txt"
catalog = json.loads((root / "app/src/resources/assets/emoji/catalog.json").read_text(encoding="utf-8"))
expected = []
groups = []
for line in source.read_text(encoding="utf-8").splitlines():
    if line.startswith("# group: "):
        group = {"name": line.removeprefix("# group: "), "text": []}
        groups.append(group)
    elif "; fully-qualified" in line:
        sequence = "".join(chr(int(p, 16)) for p in line.split(";")[0].split())
        expected.append(sequence)
        group["text"].append(sequence)
groups = [g for g in groups if g["text"]]
actual = [item[0] for g in catalog["groups"] for item in g["items"]]
assert actual == expected and len(actual) == 3944 and len(set(actual)) == 3944
assert len(catalog["groups"]) == 9
for actual_group, official in zip(catalog["groups"], groups):
    assert actual_group["name"] == official["name"]
    assert [i[0] for i in actual_group["items"]] == official["text"]
    assert all(i[1].strip() for i in actual_group["items"])
assert catalog["source_sha256"] == hashlib.sha256(source.read_bytes()).hexdigest()
assert any("\u200d" in i for i in actual) and any("\ufe0f" in i for i in actual)
assert "UNICODE LICENSE V3" in (root / "app/src/resources/assets/licenses/UNICODE-LICENSE.txt").read_text(encoding="utf-8")
print("EmojiCatalog: 3,944 sequences, 9 groups, names, ZWJ/VS16 and license PASS")
