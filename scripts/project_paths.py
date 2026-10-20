"""按功能定位源码，统一维护资产与交付版本路径。"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/resources/assets"
VERSION = (ROOT / "VERSION").read_text(encoding="utf-8").strip()


def java_source(relative):
    matches = list((ROOT / "app/src").glob("*/java/" + relative))
    if len(matches) != 1:
        raise ValueError(f"Java 源码定位结果不唯一：{relative}: {matches}")
    return matches[0]
