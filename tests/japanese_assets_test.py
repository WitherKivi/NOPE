"""校验日文资源与已固定上游源码的版本、大小、摘要及许可一致。"""
from pathlib import Path
import hashlib
import json
import tarfile

root = Path(__file__).resolve().parents[1]
assets = root / "app/src/resources/assets"
with tarfile.open(root / "vendor/msime-android-v0.3.1-source.tar.gz") as original:
    expected = original.extractfile("resources/components/japanese.lock.json").read()
assert (assets / "japanese-dictionary.lock.json").read_bytes() == expected
lock = json.loads(expected)
assert {entry["name"] for entry in lock["artifacts"]} == {
    "msime-japanese.dat", "msime-mozc_dictionary_oss_README.txt", "msime-mozc_LICENSE.txt"}
for entry in lock["artifacts"]:
    path = assets / "dictionary" / entry["name"]
    assert path.stat().st_size == entry["size"]
    with path.open("rb") as stream:
        assert hashlib.file_digest(stream, "sha256").hexdigest() == entry["sha256"]
print("JapaneseAssets: pinned 0.3.1 engine model, Mozc dictionary notice and license PASS")
