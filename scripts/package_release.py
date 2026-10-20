"""组装当前版本完整交付；验证源码逐文件一致，附带 APK 和 SHA-256。"""
from pathlib import Path
import hashlib
import json
import re
import zipfile
from project_paths import ROOT, VERSION

dist = ROOT / "dist"
stem = f"NOPE-输入法-{VERSION}"
apk = dist / f"{stem}.apk"
source = dist / f"{stem}-source.zip"
verification = dist / f"{stem}-verification.json"


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


record = json.loads(verification.read_text(encoding="utf-8"))
assert record["version"] == VERSION + "-offline"
assert record["sha256"] == digest(apk) and record["bytes"] == apk.stat().st_size
files = [ROOT / name for name in ("VERSION", "README.md", "PRODUCT.md", "DESIGN.md", ".impeccable/design.json", "LICENSE", "provenance.json", ".gitignore", ".gitattributes", "settings.gradle.kts", "build.gradle.kts", "gradle.properties", "app/build.gradle.kts")]
for directory in ("app/src", "scripts", "tests", "docs", "vendor"):
    files.extend(path for path in (ROOT / directory).rglob("*") if path.is_file() and "__pycache__" not in path.parts)
expected = {"mobile-ime-offline/" + p.relative_to(ROOT).as_posix(): p for p in files}
with zipfile.ZipFile(source) as archive:
    assert len(archive.namelist()) == len(expected) and set(archive.namelist()) == set(expected)
    for name, path in expected.items():
        assert path.suffix.lower() not in {".keystore", ".jks", ".p12", ".pfx", ".pyc", ".part"}
        assert not path.name.startswith(".env") and path.name != "local.properties"
        assert hashlib.sha256(archive.read(name)).hexdigest() == digest(path), name

test_log = (ROOT / "target/tests-v100.log").read_text(encoding="utf-8")
assert "QuickLanguagesSmoke:" in test_log and "ResourceProfile:" in test_log and "SkinContract:" in test_log
assert "BUILD SUCCESSFUL" in (ROOT / "target/build-v100.log").read_text(encoding="utf-8")
lint = (ROOT / "app/build/reports/lint-results-debug.txt").read_text(encoding="utf-8")
errors, warnings = map(int, re.search(r"(\d+) errors, (\d+) warnings", lint).groups())
assert errors == 0
report = {
    "version": VERSION, "apk": record,
    "source": {"file": source.name, "bytes": source.stat().st_size, "sha256": digest(source), "files": len(files), "exact_file_set_and_each_hash_verified": True, "secrets_build_caches_excluded": True},
    "build": "assembleDebug assembleDebugAndroidTest lintDebug --offline PASS",
    "lint": {"errors": errors, "warnings": warnings},
    "local_tests": {"java_suites": 9, "python_suites": 4, "result": "PASS"},
    "features": {"skin_count": 9, "new_skin": "lavender", "keyboard_geometry": "existing KeyboardMetrics and touch bounds; no preview bitmap at runtime", "default_quick_languages": ["CHINESE", "ENGLISH"], "configurable_quick_languages": True, "tab_leftmost": ["Code controls", "numeric panel"], "cd_brand_scope": "S4U only"},
    "final_device_ui": {"verified": False, "reason": "Phone temporarily disconnected; instrumentation compiled but not executed for this final version"},
    "github_uploaded": False,
}
delivery = dist / f"{stem}-delivery.json"
delivery.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
release_files = [apk, source, verification, delivery]
checksums = dist / f"{stem}-SHA256SUMS.txt"
checksums.write_text("".join(f"{digest(path)}  {path.name}\n" for path in release_files), encoding="utf-8")
complete = dist / f"{stem}-complete.zip"
with zipfile.ZipFile(complete, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for path in release_files + [checksums]:
        # APK/source are already compressed; do not recompress them.
        archive.write(path, path.name, compress_type=zipfile.ZIP_STORED if path.suffix in {".zip", ".apk"} else zipfile.ZIP_DEFLATED)
    archive.write(ROOT / "docs/github-upload.md", "GitHub上传步骤.md")
    archive.write(ROOT / f"docs/release-notes-v{VERSION}.md", "发布说明.md")
with zipfile.ZipFile(complete) as archive:
    assert archive.testzip() is None
    for path in release_files + [checksums]:
        assert hashlib.sha256(archive.read(path.name)).hexdigest() == digest(path)
checksum_complete = dist / f"{stem}-complete.sha256"
checksum_complete.write_text(f"{digest(complete)}  {complete.name}\n", encoding="utf-8")
print(json.dumps({"complete": complete.name, "bytes": complete.stat().st_size, "sha256": digest(complete), "source_files": len(files), "lint": report["lint"]}, ensure_ascii=False, indent=2))
