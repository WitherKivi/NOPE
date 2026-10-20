"""原生full宿主在启动前校验完整内置资源锁；不可按UI入口删词库。"""
import json
from pathlib import Path


def apply_profile(root):
    root = Path(root).resolve()
    assets = root / "app/src/resources/assets"
    original = json.loads((assets / "desktop-dictionary.lock.json").read_text(encoding="utf-8"))
    runtime = {"source_commit": original["source_commit"], "profile": "native-full-v031",
               "artifacts": original["artifacts"]}
    (assets / "runtime-dictionary.lock.json").write_text(json.dumps(runtime, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    record = json.loads((root / "provenance.json").read_text(encoding="utf-8"))
    record["omitted_runtime_assets"] = {}
    text = json.dumps(record, ensure_ascii=False, indent=2) + "\n"
    (root / "provenance.json").write_text(text, encoding="utf-8")
    (assets / "provenance.json").write_text(text, encoding="utf-8")
    return 0


if __name__ == "__main__":
    from project_paths import ROOT
    apply_profile(ROOT)
    print("原生full启动资源清单已同步；不删减原生必需文件")
