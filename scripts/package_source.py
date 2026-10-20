"""打包可交付的工程与原生版本源码，排除缓存、签名密钥和临时产物。"""
from pathlib import Path
import hashlib
import zipfile

from project_paths import ROOT as root, VERSION
destination = root / f"dist/NOPE-输入法-{VERSION}-source.zip"
files = [root / name for name in ("VERSION", "README.md", "PRODUCT.md", "DESIGN.md", ".impeccable/design.json", "LICENSE", "provenance.json", ".gitignore", ".gitattributes",
                                 "settings.gradle.kts", "build.gradle.kts", "gradle.properties", "app/build.gradle.kts")]
for directory in ("app/src", "scripts", "tests", "docs", "vendor"):
    files.extend(path for path in (root / directory).rglob("*") if path.is_file() and "__pycache__" not in path.parts)

for file in files:
    if file.suffix.lower() in {".keystore", ".jks", ".p12", ".pfx", ".pyc", ".part"} or file.name.startswith(".env") or file.name == "local.properties":
        raise RuntimeError("源码目录含本机配置或签名文件，请先移出：" + str(file.relative_to(root)))
destination.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for file in sorted(files):
        archive.write(file, "mobile-ime-offline/" + file.relative_to(root).as_posix())
with zipfile.ZipFile(destination) as archive:
    if archive.testzip() is not None:
        raise RuntimeError("源码包校验失败")
print(destination.name, destination.stat().st_size, hashlib.sha256(destination.read_bytes()).hexdigest())
