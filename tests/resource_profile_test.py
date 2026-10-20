"""对照JNI同版源码内置full锁；UI未开放不代表bootstrap资源可删除。"""
from pathlib import Path
import hashlib,json,tarfile,zipfile
root=Path(__file__).resolve().parents[1]
assets=root/"app/src/resources/assets"
original=json.loads((assets/"desktop-dictionary.lock.json").read_text(encoding="utf8"))
runtime=json.loads((assets/"runtime-dictionary.lock.json").read_text(encoding="utf8"))
japanese=json.loads((assets/"japanese-dictionary.lock.json").read_text(encoding="utf8"))
with tarfile.open(root/"vendor/msime-android-v0.3.1-source.tar.gz") as t:
    native=json.load(t.extractfile("resources/desktop-dictionary.lock.json"))
    # This is the contract reached before engine::merge_split_wubi or model preferences.
    host=t.extractfile("crates/host-api/src/lib.rs").read().decode()
    assert "verify_resources_once(&resources, specification, &state_root, on_demand)?;" in host
expected={a["name"]:a for a in native["artifacts"]}
installed={a["name"]:a for a in runtime["artifacts"]+japanese["artifacts"]}
assert installed==expected,"packaged staging differs from JNI's compiled full contract"
assert runtime["artifacts"]==original["artifacts"]
for name,a in expected.items():
    p=assets/"dictionary"/name
    assert p.stat().st_size==a["size"]
    with p.open("rb") as f:assert hashlib.file_digest(f,"sha256").hexdigest()==a["sha256"]
record=json.loads((root/"provenance.json").read_text(encoding="utf8"))
assert not record["omitted_runtime_assets"]
# 0.9.0 omitted these two and must fail the same contract before session creation.
old=set(expected)-{"msime-wubi.db","sentence-model.safetensors"}
assert old!=set(expected) and set(expected)-old=={"msime-wubi.db","sentence-model.safetensors"}
print("ResourceProfile: JNI full bootstrap contract, restored files and all original digests PASS")
