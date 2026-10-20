"""导入与包内原生引擎同版本的日文词典及许可，逐文件核对固定摘要。"""
import hashlib
import json
import tarfile
import urllib.request
import time
from project_paths import ROOT, ASSETS

with tarfile.open(ROOT / "vendor/msime-android-v0.3.1-source.tar.gz") as original:
    lock_bytes = original.extractfile("resources/components/japanese.lock.json").read()
lock = json.loads(lock_bytes)
destination = ASSETS / "dictionary"
destination.mkdir(parents=True, exist_ok=True)
for entry in lock["artifacts"]:
    name = entry["name"]
    if "/" in name or "\\" in name or ".." in name:
        raise ValueError("日文资源文件名不安全")
    target = destination / name
    if target.exists() and target.stat().st_size == entry["size"] and hashlib.sha256(target.read_bytes()).hexdigest() == entry["sha256"]:
        print("已验证", name, flush=True)
        continue
    staging = target.with_suffix(target.suffix + ".part")
    for attempt in range(4):
        size = staging.stat().st_size if staging.exists() else 0
        if size == entry["size"]:
            break
        request = urllib.request.Request(entry["url"], headers={"Range": f"bytes={size}-"} if size else {})
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                resumed = size > 0 and response.status == 206 and response.headers.get("Content-Range", "").startswith(f"bytes {size}-")
                if not resumed:
                    size = 0
                with staging.open("ab" if resumed else "wb") as output:
                    while size < entry["size"]:
                        chunk = response.read(min(262144, entry["size"] - size))
                        if not chunk:
                            raise OSError("下载在固定文件大小之前结束")
                        output.write(chunk)
                        size += len(chunk)
            break
        except (OSError, TimeoutError) as error:
            print("下载重试", name, attempt + 1, type(error).__name__, flush=True)
            if attempt == 3:
                raise
            time.sleep(1)
    with staging.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    if staging.stat().st_size != entry["size"] or digest != entry["sha256"]:
        staging.unlink(missing_ok=True)
        raise ValueError("日文资源校验失败：" + name)
    staging.replace(target)
    print("下载并验证", name, entry["size"], flush=True)
(ASSETS / "japanese-dictionary.lock.json").write_bytes(lock_bytes)
