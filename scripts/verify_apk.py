"""核对最终 APK 的权限、签名、ABI 和离线资产；不宣称设备验收。"""
from pathlib import Path
import hashlib
import json
import os
import re
import subprocess
import struct
import sys
import zipfile
import tarfile
import tempfile
import shutil
import atexit

root = Path(__file__).resolve().parents[1]
apk = Path(sys.argv[1]) if len(sys.argv) > 1 else root / "app/build/outputs/apk/debug/app-debug.apk"
sdk = Path(os.environ["ANDROID_HOME"]) / "build-tools/35.0.0"
# Windows aapt/zipalign cannot consistently open Unicode filenames. Verify an
# identical temporary ASCII-named copy, while reporting the actual delivery.
check_directory = tempfile.TemporaryDirectory(prefix="nope-apk-check-")
atexit.register(check_directory.cleanup)
checked_apk = Path(check_directory.name) / "checked.apk"
shutil.copyfile(apk, checked_apk)

def run(executable, *arguments):
    result = subprocess.run([str(sdk / executable), *(str(checked_apk if argument == apk else argument) for argument in arguments)], check=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, encoding="utf-8")
    return result.stdout.strip()

permissions = run("aapt.exe", "dump", "permissions", apk)
requested = re.findall(r"uses-permission: name='([^']+)'", permissions)
if set(requested) != {"android.permission.VIBRATE"}:
    raise RuntimeError("离线包仅允许按键震动权限：" + str(requested))
badging = run("aapt.exe", "dump", "badging", apk)
version = (root / "VERSION").read_text(encoding="utf-8").strip()
version_code = int(re.search(r"versionCode\s*=\s*(\d+)", (root / "app/build.gradle.kts").read_text(encoding="utf-8")).group(1))
if f"versionName='{version}-offline'" not in badging or f"versionCode='{version_code}'" not in badging:
    raise RuntimeError("APK 版本与当前 VERSION / versionCode 不一致")
for required in ("name='app.msime.offline'", "sdkVersion:'28'", "native-code: 'arm64-v8a'", "provides-component:'ime'"):
    if required not in badging:
        raise RuntimeError("安装包配置不符合交付范围：" + required)
if "application-label:'NOPE 输入法'" not in badging:
    raise RuntimeError("NOPE 应用名称未打包")
record = json.loads((root / "provenance.json").read_text(encoding="utf-8"))
dependencies = {}
with zipfile.ZipFile(apk) as package:
    libraries = sorted(name for name in package.namelist() if name.startswith("lib/"))
    expected = sorted("lib/arm64-v8a/" + name for name in record["files_sha256"] if name.endswith(".so"))
    if libraries != expected:
        raise RuntimeError("原生库数量或 ABI 不匹配")
    allowed = {Path(name).name for name in expected} | {"libc.so", "libm.so", "libdl.so", "liblog.so", "libandroid.so", "libz.so", "libaaudio.so"}
    for library in libraries:
        elf = package.read(library)
        if library.endswith("libmsime_android.so"):
            for method in ("allCandidatesRaw", "viewRaw", "selectAnyCandidateRaw"):
                if ("Java_app_msime_android_NativeClient_" + method).encode() not in elf:
                    raise RuntimeError("缺少展开候选 JNI 接口：" + method)
        if elf[:6] != b"\x7fELF\x02\x01":
            raise RuntimeError("需要 ELF64 小端库")
        offset = struct.unpack_from("<Q", elf, 40)[0]
        width, count = struct.unpack_from("<HH", elf, 58)
        sections = [struct.unpack_from("<IIQQQQIIQQ", elf, offset + i * width) for i in range(count)]
        needed = []
        for section in sections:
            if section[1] != 6:  # SHT_DYNAMIC
                continue
            strings = sections[section[6]]
            table = elf[strings[4]:strings[4] + strings[5]]
            for entry in range(section[4], section[4] + section[5], section[9]):
                tag, value = struct.unpack_from("<qQ", elf, entry)
                if tag == 1:  # DT_NEEDED
                    needed.append(table[value:table.index(b"\0", value)].decode("ascii"))
        if set(needed) - allowed:
            raise RuntimeError("缺少原生依赖：" + str(set(needed) - allowed))
        dependencies[Path(library).name] = needed
    for name, digest in record["files_sha256"].items():
        path = "lib/arm64-v8a/" + name if name.endswith(".so") else "assets/dictionary/" + name
        if name in record.get("omitted_runtime_assets", {}):
            if path in package.namelist():
                raise RuntimeError("未使用资源被重新打包：" + name)
            continue
        if hashlib.sha256(package.read(path)).hexdigest() != digest:
            raise RuntimeError("资产与已验证来源不一致：" + name)
    if json.loads(package.read("assets/provenance.json")) != record:
        raise RuntimeError("来源记录不一致")
    runtime_lock = package.read("assets/runtime-dictionary.lock.json")
    assets = root / "app/src/resources/assets"
    if runtime_lock != (assets / "runtime-dictionary.lock.json").read_bytes():
        raise RuntimeError("运行时资源清单未更新")
    original_lock = json.loads(package.read("assets/desktop-dictionary.lock.json"))
    runtime = json.loads(runtime_lock)
    retained = [entry for entry in original_lock["artifacts"] if entry["name"] not in record["omitted_runtime_assets"]]
    if runtime["source_commit"] != original_lock["source_commit"] or runtime["artifacts"] != retained:
        raise RuntimeError("运行时清单省略了所需词库")
    for entry in runtime["artifacts"]:
        data = package.read("assets/dictionary/" + entry["name"])
        if len(data) != entry["size"] or hashlib.sha256(data).hexdigest() != entry["sha256"]:
            raise RuntimeError("运行时词库清单校验失败：" + entry["name"])
    if "assets/client-LICENSE.txt" not in package.namelist():
        raise RuntimeError("缺少许可")
    emoji = json.loads(package.read("assets/emoji/catalog.json"))
    if len(emoji["groups"]) != 9 or sum(len(g["items"]) for g in emoji["groups"]) != 3944:
        raise RuntimeError("emoji 分类目录缺失")
    for asset in ("emoji/catalog.json", "licenses/UNICODE-LICENSE.txt"):
        if package.read("assets/" + asset) != (root / "app/src/resources/assets" / asset).read_bytes():
            raise RuntimeError("emoji 数据或许可证打包不一致")
    with tarfile.open(root / "vendor/msime-android-v0.3.1-source.tar.gz") as original:
        japanese_lock = original.extractfile("resources/components/japanese.lock.json").read()
        if package.read("assets/japanese-dictionary.lock.json") != japanese_lock:
            raise RuntimeError("日文资源清单不匹配原生版本")
        japanese = json.loads(japanese_lock)
        native_full=json.load(original.extractfile("resources/desktop-dictionary.lock.json"))
        installed={entry["name"]:entry for entry in runtime["artifacts"]+japanese["artifacts"]}
        if installed!={entry["name"]:entry for entry in native_full["artifacts"]}:
            raise RuntimeError("JNI内置full启动资源契约不匹配")
        for entry in japanese["artifacts"]:
            value = package.read("assets/dictionary/" + entry["name"])
            if len(value) != entry["size"] or hashlib.sha256(value).hexdigest() != entry["sha256"]:
                raise RuntimeError("日文词典或许可证摘要不匹配：" + entry["name"])
        for name in ("CJK_Compatibility_Ideographs.txt", "STPhrases.txt", "STPhrases_GeneratedFromRegionalPhrases.txt", "STCharacters.txt", "LICENSE"):
            if package.read("assets/opencc/"+name) != original.extractfile("crates/client-core/data/opencc/"+name).read():
                raise RuntimeError("OpenCC 数据和原版源码不一致："+name)

with zipfile.ZipFile(apk) as package:
    for directory in ("skins","skin"):
        for asset in (root/"app/src/resources/assets"/directory).rglob("*"):
            if asset.is_file() and package.read("assets/"+asset.relative_to(root/"app/src/resources/assets").as_posix())!=asset.read_bytes():
                raise RuntimeError("皮肤配置或字体未正确打包："+asset.name)

signature = run("apksigner.bat", "verify", "--verbose", "--print-certs", apk)
certificate = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-f]+)", signature).group(1)
previous_report = root / "docs/history/delivery-verification-v060.json"
previous = json.loads(previous_report.read_text(encoding="utf-8"))
if certificate != previous["certificate_sha256"]:
    raise RuntimeError("开发签名与0.6.0不一致；请通过 NOPE_DEBUG_KEYSTORE 指定原签名文件")
alignment = run("zipalign.exe", "-c", "-P", "16", "4", apk)
subtypes = run("aapt.exe", "dump", "xmltree", apk, "res/xml/method.xml")
if 'ja_JP' not in subtypes:
    raise RuntimeError("缺少 Android 日文键盘入口")
report = {
    "apk": apk.name,
    "bytes": apk.stat().st_size,
    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
    "package": "app.msime.offline", "version": re.search(r"versionName='([^']+)'", badging).group(1), "version_code": version_code,
    "min_android_api": 28, "abi": ["arm64-v8a"],
    "requested_permissions": requested, "verified_original_assets": len(record["files_sha256"])-len(record.get("omitted_runtime_assets", {})),
    "omitted_runtime_assets": list(record.get("omitted_runtime_assets", {})),
    "runtime_dictionary_profile_verified": True,
    "native_full_bootstrap_contract_verified": True,
    "skin_v1_configs_and_pixel_font_verified": True,
    "emoji_version": emoji["version"], "emoji_categories": 9, "emoji_catalog_entries": 3944,
    "label": "NOPE 输入法", "opencc_original_files_verified": 5,
    "native_dependencies": dependencies,
    "signature_verified": True, "zip_alignment_16k_verified": True,
    "expanded_candidate_jni_symbols_verified": True,
    "japanese_original_assets_verified": len(japanese["artifacts"]), "japanese_system_subtype_verified": True,
    "certificate_sha256": certificate, "certificate_matches_version": "0.6.0",
    "device_runtime_verified": False,
}
(root / "dist").mkdir(exist_ok=True)
(root / "dist" / (apk.stem + "-verification.json")).write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, ensure_ascii=False, indent=2))
print(signature)
