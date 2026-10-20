"""验证脱机键位、建议、修饰键释放与敏感字段边界，不使用用户输入。"""
from pathlib import Path
import os
import subprocess
import sys
import tempfile
from project_paths import ROOT as root, ASSETS, java_source

home = Path(os.environ["JAVA_HOME"])
java, javac = home / "bin/java.exe", home / "bin/javac.exe"
native = ("CodingCompletion", "CodingInput", "CodingKeyMap", "CodingLayout", "SelectionEchoTracker", "BoundsPolicy")
host = ("FieldPolicy", "EditorTransition", "KeyboardMetrics", "DeleteSwipe", "ClipboardPolicy", "SymbolLayouts",
        "VariantSelection", "ClipboardSuggestion", "HapticStrength", "CandidatePageSwipe", "EditingSwipe",
        "ClipboardText", "KeyPreviewState", "JapaneseInput", "ChineseConversion", "ClipboardRetention", "SkinPalette", "InputTouchPolicy", "KeyboardText", "QuickLanguages", "CandidateRows", "CandidateRequest")
tests = ("CodingKeyboardSmoke", "FieldPolicySmoke", "EditorTransitionSmoke", "KeyboardUpgradeSmoke", "NopeUpgradeSmoke", "JapaneseInputSmoke", "FeedbackUpgradeSmoke", "ClipboardTextSmoke", "QuickLanguagesSmoke", "CandidateExpansionSmoke")
files = [java_source(f"app/msime/android/{name}.java") for name in native]
files += [java_source(f"app/msime/offline/{name}.java") for name in host]
files += [root / f"tests/{name}.java" for name in tests]
(root / "target").mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(dir=root / "target") as directory:
    subprocess.run([str(javac), "--release", "17", "-encoding", "UTF-8", "-d", directory, *map(str, files)], check=True)
    for test in tests:
        qualified = test if test == "CodingKeyboardSmoke" else "app.msime.offline." + test
        arguments = [str(ASSETS / "opencc")] if test == "NopeUpgradeSmoke" else []
        subprocess.run([str(java), "-cp", directory, qualified, *arguments], check=True)
subprocess.run([sys.executable, str(root / "tests/emoji_catalog_test.py")], check=True)

subprocess.run([sys.executable, str(root / "tests/japanese_assets_test.py")], check=True)

subprocess.run([sys.executable, str(root / "tests/resource_profile_test.py")], check=True)

subprocess.run([sys.executable, str(root / "tests/skin_contract_test.py")], check=True)
subprocess.run([sys.executable, str(root / "tests/net_assets_test.py")], check=True)
