"""从用户提供的水杉 APK 提取离线资产，并记录来源与摘要。"""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
source = Path(sys.argv[1]).resolve()
repo = Path(sys.argv[2]).resolve()
assets = root / "app/src/resources/assets"
java = root / "app/src/engine/java/app/msime/android"
assets.mkdir(parents=True, exist_ok=True)
java.mkdir(parents=True, exist_ok=True)

def put(relative, data):
    destination = root / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)

files = {}
with zipfile.ZipFile(source) as apk:
    lock = json.loads(apk.read("assets/desktop-dictionary.lock.json"))
    for entry in lock["artifacts"]:
        name = entry["name"]
        if Path(name).name != name or "/" in name or "\\" in name:
            raise ValueError("unsafe dictionary name")
        data = apk.read("assets/dictionary/" + name)
        digest = hashlib.sha256(data).hexdigest()
        if len(data) != entry["size"] or digest != entry["sha256"]:
            raise ValueError("dictionary checksum mismatch: " + name)
        put("app/src/resources/assets/dictionary/" + name, data)
        files[name] = digest
    put("app/src/resources/assets/desktop-dictionary.lock.json", apk.read("assets/desktop-dictionary.lock.json"))
    required = ["prepareHostRaw", "defaultPreferencesRaw", "createRaw", "focusRaw", "setPrivateSessionRaw", "characterRaw", "commandRaw", "selectRaw", "allCandidatesRaw", "viewRaw", "selectAnyCandidateRaw", "destroyRaw"]
    for library in ("libc++_shared.so", "libmsime_android.so", "libmsime_host_api.so"):
        data = apk.read("lib/arm64-v8a/" + library)
        if library == "libmsime_android.so":
            for method in required:
                if ("Java_app_msime_android_NativeClient_" + method).encode() not in data:
                    raise ValueError("JNI symbol missing: " + method)
        put("app/src/engine/jniLibs/arm64-v8a/" + library, data)
        files[library] = hashlib.sha256(data).hexdigest()
    for name in apk.namelist():
        if name.startswith("assets/native-notices/") and not name.endswith("/"):
            parts = Path(name).parts
            if ".." in parts:
                raise ValueError("unsafe notice path")
            put("app/src/resources/" + name, apk.read(name))
    put("app/src/resources/assets/client-LICENSE.txt", apk.read("assets/client-LICENSE.txt"))

for relative in (
    "keyboard/CodingCompletion.java", "keyboard/CodingInput.java", "keyboard/CodingKeyMap.java",
    "keyboard/CodingLayout.java", "keyboard/KeyboardKeyArea.java", "keyboard/KeyboardGapPolicy.java", "core/SelectionEchoTracker.java", "BoundsPolicy.java"
):
    original = repo / "platforms/android/java/app/msime/android" / relative
    # 重新提取资产不得覆盖已修复的本地 Java 辅助代码。
    existing = list((root / "app/src").glob("*/java/app/msime/android/" + original.name))
    if not existing:
        destination = root / ("app/src/engine/java/app/msime/android" if original.stem in ("BoundsPolicy", "SelectionEchoTracker") else "app/src/layout/java/app/msime/android") / original.name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(original, destination)
shutil.copyfile(repo / "LICENSE", root / "LICENSE")
git = ["git", "-c", "safe.directory=" + str(repo)]
commit = subprocess.check_output([*git, "rev-parse", "android-v0.3.1"], cwd=repo, text=True).strip()
host_commit = subprocess.check_output([*git, "rev-parse", "HEAD"], cwd=repo, text=True).strip()
record = {
    "apk_version": "0.3.1",
    "apk_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
    "native_source_repository": "https://github.com/metasequoiaime/msime",
    "native_source_ref": "android-v0.3.1",
    "native_source_commit": commit,
    "host_helpers_base_commit": host_commit,
    "host_helpers_local_additions": ["CodingCompletion", "CodingInput", "CodingKeyMap", "CodingLayout"],
    "dictionary_source_commit": lock["source_commit"],
    "native_binaries_reused_not_recompiled": True,
    "files_sha256": files,
}
put("provenance.json", (json.dumps(record, ensure_ascii=False, indent=2) + "\n").encode())
shutil.copyfile(root / "provenance.json", assets / "provenance.json")
(root / "vendor").mkdir(exist_ok=True)
subprocess.run([*git, "archive", "--format=tar.gz", "--output=" + str(root / "vendor/msime-android-v0.3.1-source.tar.gz"), "android-v0.3.1"], cwd=repo, check=True)
print("Extracted and verified offline dictionaries, three ARM64 libraries, notices and local Java helpers.")

from resource_profile import apply_profile
apply_profile(root)
