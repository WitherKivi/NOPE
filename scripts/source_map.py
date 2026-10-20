"""生成真实工作树和逐文件作用说明，HTML 可离线搜索，不包含缓存或私有数据。"""
from pathlib import Path
import html
import json
import re
from urllib.parse import quote

root=Path(__file__).resolve().parents[1]
roles={
"OfflineImeService":"输入法主服务：组字、引擎会话、键盘构建、候选、语言切换、手势及各功能面板的入口。",
"SetupActivity":"应用设置与试打入口：启用输入法、语言、高度、文字、反馈、皮肤和本地数据设置。",
"NativeClient":"匹配原生符号的 JNI 桥接，调用固定版本的本地引擎；不实现词典算法。",
"EngineInstall":"安装并校验完整词库/模型、准备原生宿主和引擎会话配置。",
"BoundsPolicy":"纯 Java 数值上下界，供跨平台策略和几何计算使用。",
"SelectionEchoTracker":"跟踪预期选区回报，区分迟到回声和真实光标移动，防止快速键入时首字母组字丢失。",
"ChineseConversion":"用随包 OpenCC 字符和词组表转换中文候选与上屏文字。",
"EditorTransition":"处理输入框切换、焦点变化与会话迁移，避免把旧输入带进新编辑器。",
"FieldPolicy":"识别密码、私密、普通和终端字段，决定直输、学习和剪贴板边界。",
"JapaneseInput":"日文罗马字组字、假名读音、空格转换和回车确认的状态策略。",
"ClipboardPolicy":"筛选可记录的普通纯文本，排除敏感字段、敏感标记和非文本剪贴板。",
"ClipboardRetention":"剪贴板保留时间解析、过期判断和置顶豁免。",
"ClipboardSettings":"剪贴板保留时长选择及当前设置摘要。",
"ClipboardStore":"应用私有剪贴板数据库：条目、置顶、去重、删除、容量及过期维护。",
"ClipboardSuggestion":"管理最近一次复制的临时提示，与历史排序及置顶相互独立。",
"ClipboardText":"限制预览排版开销、处理换行和 Unicode；保存与粘贴保持完整原文。",
"EmojiCatalog":"读取 Unicode 表情分类，按当前设备字体过滤不支持的序列。",
"EmojiCell":"独立绘制表情单元，避免回收按钮的省略和滚动状态影响显示。",
"CodingCompletion":"本机静态代码/命令片段补全；只补缺失后缀，不执行命令。",
"CodingInput":"成对发送按下/抬起与修饰键事件，异常路径也释放按键。",
"CodingKeyMap":"把半角字符、Ctrl、Alt、Shift 映射为 Android 按键代码与元状态。",
"CodingLayout":"Code 字母与标点布局数据，分层提供完整可达的 ASCII 字符。",
"KeyboardGapPolicy":"将键间空隙中的落点分配给相邻键，避免触点落在边距时丢键。",
"KeyboardKeyArea":"键盘行/列触摸容器，负责键缝命中、指针坐标平移和取消派发。",
"ArrowButton":"方向控制按钮，使用原生文本绘制方向符号。",
"CandidateFlow":"旧版全量候选的自动换行容器；迁移为可回收候选行前的实现。",
"CandidateList":"原生 ListView 候选列表，仅创建可见行的候选按钮，长词仍可折行。",
"CandidateRows":"按实际词宽计算候选行与索引，不创建任何 Android View。",
"CandidateRequest":"延迟候选查询票据，防止收起、编辑或换输入框后使用旧结果。",
"CandidatePageSwipe":"候选横栏边缘翻页手势判定。",
"CandidateStrip":"可横向滚动候选栏，滑到边缘后转交引擎翻页。",
"DeleteSwipe":"删除长按/上滑清空的可取消手势策略。",
"EditingSwipe":"字母下滑的撤回、全选、剪切、复制、粘贴策略。",
"HapticStrength":"震动百分比到振幅/时长的映射。",
"HintKey":"显示数字或标点提示的字母键；NET. 的已绘制数字避免重复显示。",
"InputTouchPolicy":"区分轻触抖动和主动的滑动手势。",
"KeyboardIcon":"工具、品牌和功能键图标；NET. 可使用独立 PNG。",
"KeyboardMetrics":"普通/Code 键盘高度、行高和辅助控制区尺寸。",
"KeyboardText":"按键文字缩放上下界与固定候选字号。",
"KeyButton":"按键文字测量、缩放、按压反馈、预览与触摸基础控件。",
"KeyFeedback":"执行用户设置的按键震动，处理硬件与系统限制。",
"KeyPreview":"显示不拦截触摸的字母气泡，并处理屏幕边缘位置。",
"KeyPreviewState":"气泡的按键所有权与代数票据，防止旧松手隐藏新气泡。",
"QuickLanguages":"默认中英互切及自定义快速语言循环。",
"SymbolLayouts":"符号页、数字上滑提示和长按变体数据。",
"VariantPanel":"跟随手指选择的长按/上滑变体弹窗。",
"VariantSelection":"变体弹窗定位、命中和滑回取消的几何计算。",
"BrandMark":"默认 N 在上、ope 在下的方正标志路径。",
"GlassWindow":"在可安全限定到键盘窗口时使用系统模糊，并提供回退。",
"IosTheme":"整合皮肤配置、色板、候选颜色和材质资源。",
"MetalBackground":"原生绘制 mac 拟物的拉丝金属背景。",
"NetAssets":"缓存 NET. 雾面纹理、透明图标和圆润字体，避免逐键重复解码。",
"PixelGlassDrawable":"绘制 NET. 雾面背景、圆角玻璃键帽及首排 1–0。",
"PixelGlyphs":"项目自制的像素字形数据与绘制，主要用于 S4U 气泡及字体源。",
"RetroDrawable":"绘制 S4U 米色机壳、厚键帽和墨绿 LCD 槽。",
"RetroRecordDrawable":"S4U 独立全彩唱片图标，按按钮可用区域居中，不参与文字排版和染色。",
"RetroTypeface":"加载并缓存 S4U 随包像素 TTF。",
"SkinConfig":"校验并读取皮肤 JSON：色板、角色、图层、装饰和背景透视。",
"SkinFiles":"把用户选择的 JSON 配置导入应用私有目录，支持恢复默认。",
"SkinKeyDrawable":"按角色和状态选择键帽材质，绘制阴影、轮廓和按下状态。",
"SkinPalette":"九种皮肤的名称、基础颜色、前景对比、圆角及深度参数。",
"SkinStack":"共用图层栈：背景、下装饰、键帽/文字、上装饰和反馈；透视仅作用于背景。",
"NopeDeviceSmoke":"需手机执行的 instrumentation：真实 JNI、中日文、字体、材质图标及键盘几何检查。",
"SettingsMaterial":"设置页的明暗雾面背景、细纹理及圆角功能按钮材质。",
"BrandSymbols":"各皮肤专属的云朵、命令键及咬痕梨子原生图标。",
"build_net_assets":"从固定 Nunito 源生成圆润字体子集与 NET. 透明工具图标。",
"build_s4u_font":"从项目自制像素字形生成 S4U TTF。",
"extract_msime":"从指定上游 APK 提取固定原生库、词库与许可。",
"import_emoji":"导入固定 Unicode 表情目录与许可。",
"import_japanese":"按固定上游清单导入离线日文模型和声明。",
"import_net_font":"更新时下载固定版本 Nunito 字体源与 OFL 许可；运行时不联网。",
"package_release":"核对当前 APK/源码并组合完整交付包与 SHA-256。",
"package_source":"打包源码、资源、脚本和许可，排除密钥、缓存与临时文件。",
"project_paths":"统一定位工程根目录、版本号和多个 Java 源码目录。",
"render_net":"用实际 NET. 资源生成示意图，不是 Android 截图。",
"render_skins":"生成皮肤色板及标准键位的静态对比示意。",
"resource_profile":"维护原生 full 启动资源契约，避免误删模型或词库。",
"test_local":"编译并运行纯 Java 回归，再运行 Python 资源检查。",
"verify_apk":"核验最终 APK 的版本、权限、原生依赖、资源、签名和对齐。",
"source_map":"生成此源码树与逐文件用途导览。",
"render_settings":"用实际设置颜色与精简文案生成明暗雾面示意，非 Android 截图。",
"CodingKeyboardSmoke":"验证补全后缀、隐私、过期建议、ASCII 映射与终端按键派发。",
"FieldPolicySmoke":"验证私密/密码字段及学习策略。",
"EditorTransitionSmoke":"验证输入框切换边界。",
"KeyboardUpgradeSmoke":"验证高度、可取消滑动、变体、ASCII 与剪贴板隐私。",
"NopeUpgradeSmoke":"验证首字母回声、触点抖动、编辑手势、繁简及皮肤对比。",
"JapaneseInputSmoke":"验证日文组字、转换、确认和会话重置。",
"FeedbackUpgradeSmoke":"验证气泡所有权、快速连按、边缘位置与字号策略。",
"ClipboardTextSmoke":"验证长文本、换行、Unicode 和可读缩放。",
"QuickLanguagesSmoke":"验证默认中英、自定义循环与无效配置回退。",
"CandidateExpansionSmoke":"验证候选行打包、大列表索引与过期请求取消。",
"SkinTokens":"导出皮肤参数，供示意图脚本使用。",
"emoji_catalog_test":"检查表情条目、分类、序列与许可证。",
"japanese_assets_test":"检查日文模型固定摘要及声明。",
"resource_profile_test":"检查完整启动资源契约与原始资源摘要。",
"skin_contract_test":"检查九种皮肤的图层、区域、透视与像素字体轮廓。",
"net_assets_test":"检查 NET. 图片/字体摘要、字形覆盖和 OFL 来源。",
}
special={"VERSION":"交付版本号来源。","README.md":"保留的功能介绍。","LICENSE":"项目主许可证。","provenance.json":"原始引擎库与资源来源及摘要。","build.gradle.kts":"根目录 Android 构建插件配置。","app/build.gradle.kts":"Android 版本、ABI、源码根目录、打包、签名入口和 Lint 配置。","settings.gradle.kts":"工程模块与依赖仓库配置。","gradle.properties":"Gradle/Android 构建参数。",".gitignore":"排除缓存、安装包、密钥和本机配置。",".gitattributes":"文本换行与二进制文件属性。"}
def purpose(p):
    rel=p.relative_to(root).as_posix()
    if rel in special:return special[rel]
    if p.stem in roles:return roles[p.stem]
    if p.suffix==".java":
        match=re.search(r"/\*\*(.*?)\*/",p.read_text(encoding="utf-8"),re.S)
        if match:return re.sub(r"\s+"," ",match.group(1)).strip(" *")[:160]
    if rel.startswith("vendor/"):
        if p.name.endswith(".tar.gz"):return "固定版本引擎完整 Rust/C++ 上游源码归档；未展开进 Android 宿主目录。"
        if p.suffix==".ttf":return "NET. 圆润字体的原始上游字体源，用于生成随包子集。"
        if p.name=="provenance.json":return "字体上游提交、下载地址、许可与摘要。"
        return "固定版本上游原始数据。"
    if "/skins/" in rel:return "皮肤参数与装饰配置。" if p.stem!="schema-v1" else "皮肤 JSON 格式约束。"
    if "/dictionary/" in rel:return "固定离线词库、语言模型或其来源/许可声明。"
    if "/skin/net/" in rel:return "NET. 的纹理、字体、透明图标或资源清单。"
    if "/native-notices/" in rel or "/licenses/" in rel:return "随包第三方许可或来源声明，不能按普通说明文档删除。"
    if "/jniLibs/" in rel:return "ARM64 预编译原生引擎或运行库，由 JNI 使用。"
    if "/opencc/" in rel:return "中文繁简转换数据及其许可/声明。"
    if p.name=="AndroidManifest.xml":return "注册设置 Activity、输入法服务及权限。"
    if p.name=="method.xml":return "系统输入法入口、设置页和语言 subtype。"
    if p.name=="data_extraction_rules.xml":return "限制本机数据备份/迁移。"
    if p.name=="ic_keyboard.xml":return "应用图标矢量资源。"
    if p.name=="styles.xml":return "系统明暗/Android 版本对应的主题配置。"
    if p.name=="strings.xml":return "界面格式化文字资源。"
    if p.name=="ids.xml":return "试打和无障碍操作等资源 ID。"
    if p.name.endswith("lock.json"):return "固定版本资源的清单、尺寸和摘要。"
    return "资源或工程元数据；完整相对路径见文件列。"

paths=[p for p in (root/"app/src").rglob("*") if p.is_file()]
paths += [p for folder in ("scripts","tests","vendor") for p in (root/folder).rglob("*") if p.is_file() and "__pycache__" not in p.parts]
paths += [root/p for p in special if (root/p).is_file()]
paths=sorted(set(paths))
service=(root/"app/src/main/java/app/msime/offline/OfflineImeService.java").read_text(encoding="utf-8")
records=[]
for p in paths:
    rel=p.relative_to(root).as_posix();state=""
    if p.stem in {"CandidateRows","CandidateList","CandidateRequest"} and "CandidateList expandedCandidates" not in service:state="新增，尚待接入"
    records.append({"path":rel,"purpose":purpose(p),"bytes":p.stat().st_size,"state":state})
tree={}
for item in records:
    current=tree
    for part in item["path"].split("/"):current=current.setdefault(part,{})
notes={"vendor":"上游完整源码、字体源和原始数据","docs":"示意图、源码导览与历史核验 JSON","dist":"安装包、源码包和摘要（Git忽略）",".gradle":"本机构建缓存（Git忽略）","build":"根构建产物（Git忽略）","app/build":"Android 构建产物（Git忽略）","target":"本机测试与工具缓存（Git忽略）"}
for path in notes:
    current=tree
    for part in path.split("/"):current=current.setdefault(part,{})
def tree_lines(node,prefix="",parent=""):
    result=[];names=sorted(node)
    for i,name in enumerate(names):
        relative=parent+name;last=i==len(names)-1;folder=bool(node[name]) or relative in notes
        result.append(prefix+("└── " if last else "├── ")+name+("/" if folder else "")+("  "+notes[relative] if relative in notes else ""))
        if node[name]:result+=tree_lines(node[name],prefix+("    " if last else "│   "),relative+"/")
    return result
tree_text="mobile-ime-offline/\n"+"\n".join(tree_lines(tree))
rows="".join(f'<tr><td><a href="../{quote(x["path"])}">{html.escape(x["path"])}</a><small>{html.escape(x["state"])}</small></td><td>{html.escape(x["purpose"])}</td><td>{x["bytes"]:,}</td></tr>' for x in records)
page='''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>NOPE 源码导览</title><style>
body{max-width:1200px;margin:40px auto;padding:0 20px;background:#fafafa;color:#24232a;font:16px/1.65 system-ui,"Microsoft YaHei",sans-serif}h1{font-size:30px}p{max-width:900px}input{width:min(96%,800px);padding:13px;border:1px solid #aaa;border-radius:10px;font:inherit}table{width:100%;border-collapse:collapse;margin-top:20px}th,td{text-align:left;padding:12px 10px;border-bottom:1px solid #ddd;vertical-align:top}td:first-child{width:48%;font:13px/1.6 monospace;overflow-wrap:anywhere}td:last-child{white-space:nowrap;color:#666}a{color:#4c438e}small{display:block;color:#a35817}pre{overflow:auto;padding:20px;background:#efedf3;font:13px/1.6 monospace}summary{cursor:pointer;padding:12px 0}tr[hidden]{display:none}@media(prefers-color-scheme:dark){body{background:#202024;color:#eeedf3}pre{background:#292831}a{color:#b9adee}td,th{border-color:#444}input{background:#303038;color:white}td:last-child{color:#bbb}}
</style><h1>NOPE 当前源码树与文件作用</h1><p>按实际工作树生成。源码在 main / engine / layout / skin / features，资源在 resources。Java 包名保持兼容 JNI；目录分类与包名是两件事。上游引擎的 Rust/C++ 源码以固定 tar.gz 保存在 vendor，本页逐项列出当前 Android 宿主源码，不展开归档内部文件。</p><p>下表覆盖 <b>COUNT</b> 个源码、脚本、测试、资源和工程文件。新增但尚未接入的候选优化文件会标记状态。过滤框可按文件名、路径或功能搜索。</p><input id="q" placeholder="搜索，例如 候选、皮肤、剪贴板、NativeClient"><details><summary>展开完整 tree</summary><pre>TREE</pre></details><table><thead><tr><th>文件</th><th>作用</th><th>字节</th></tr></thead><tbody>ROWS</tbody></table><script>document.getElementById('q').addEventListener('input',e=>{let q=e.target.value.toLowerCase();document.querySelectorAll('tbody tr').forEach(r=>r.hidden=!r.textContent.toLowerCase().includes(q))})</script></html>'''
page=page.replace('COUNT',str(len(records))).replace('TREE',html.escape(tree_text)).replace('ROWS',rows)
out=root/"docs/source-map.html";out.parent.mkdir(exist_ok=True);out.write_text(page,encoding="utf-8")
(root/"docs/source-tree.txt").write_text(tree_text+'\n',encoding="utf-8")
print(out,len(records),"files",sum(p.suffix==".java" and "/tests/" not in p.as_posix() for p in paths),"Android Java")
