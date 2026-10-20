"""只在更新 NET. 字体源时访问上游；正常构建使用已提交字体。"""
from pathlib import Path
import hashlib
import json
import urllib.parse
import urllib.request

root = Path(__file__).resolve().parents[1]
headers = {"User-Agent": "NOPE-font-import"}
provenance = root / "vendor/net-font/provenance.json"
if provenance.exists():
    commit = json.loads(provenance.read_text(encoding="utf-8"))["commit"]
else:
    query = urllib.request.Request("https://api.github.com/repos/google/fonts/commits?path=ofl/nunito&per_page=1", headers=headers)
    commit = json.load(urllib.request.urlopen(query, timeout=30))[0]["sha"]
files = {}
for name, local in (("Nunito[wght].ttf", "vendor/net-font/Nunito-Variable.ttf"), ("OFL.txt", "app/src/resources/assets/licenses/NET-Nunito-OFL.txt")):
    url = f"https://raw.githubusercontent.com/google/fonts/{commit}/ofl/nunito/" + urllib.parse.quote(name)
    data = urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=30).read()
    target = root / local
    target.parent.mkdir(exist_ok=True, parents=True)
    target.write_bytes(data)
    files[local] = {"url": url, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
provenance.write_text(json.dumps({"repository": "https://github.com/google/fonts", "commit": commit, "license": "OFL-1.1", "files": files}, indent=2) + "\n", encoding="utf-8")
print("Nunito source and OFL imported at", commit)
