# NOPE 输入法

Android 9+、ARM64 的离线输入法，支持中文全拼、日文罗马字、English 和 Coding。包名 `app.msime.offline`，沿用原开发签名，可覆盖旧版并保留本机设置、词库学习和剪贴板。

## 安装

从 GitHub Releases 下载 APK；安装后，打开「NOPE 输入法」，依次点击「启用输入法」「选择输入法」。词库随包提供，无需下载；首次准备期间会暂存输入。

## 1.0.0

默认语言键改为中文 ↔ English；设置「输入语言 → 调整快速切换语言」可选两种或更多键盘循环，长按空格仍能选全部模式。Code 控制行与数字面板的 Tab 均移到最左侧。新增「淡紫像素」皮肤

## 已有功能

0.9.2修正S4U斜度的含义：只有背景机壳带轻微透视，键盘、候选栏和触摸区域保持标准尺寸。快捷栏保留剪贴板、表情和设置，统一放进墨绿LCD区域。NOPE标志保留N在上、ope在下，改为简约方正字形；S4U仍使用唱片。

- 新增 **S4U**：米色复古像素键帽、机械台阶边缘、抖色细节、灰绿 LCD 候选槽与 CD 唱片机图标。中文、日文、English 与 Code 沿用当前布局。只有 S4U 使用唱片；其他皮肤和应用图标保持上方 **N**、下方 **ope**。
- 候选横栏与展开列表统一放大到 **22sp**。默认皮肤浅色功能键副色调改为柔粉，深色分支同步使用低饱和粉灰。
- 剪贴板历史卡片使用两行 **12–15sp** 自适应预览，顶部最近复制提示为 **12–16sp**。预览压平换行、限制到 384 个 Unicode 码点并省略；保存和点击粘贴仍使用完整原文。系统大字体增加卡片最低高度，不再按整段文本宽度无限缩字。
- 修复0.9.0引擎未就绪：恢复五笔词库与句子模型。full原生宿主在使用前校验完整内置清单，不能因界面未开放就删除资源；升级会自动补回，不清学习与剪贴板。
- 所有皮肤统一分层和区域配置：背景、按键下装饰、键帽、文字图标、按键上装饰、交互反馈。设置新增JSON配置导入与恢复；完整标准和可导入例子见[皮肤标准v1](docs/skin-standard-v1.md)。

以下功能继续保留：

- **日本語（ローマ字）**：QWERTY 罗马字转平假名、片假名及汉字候选，词典完全离线。长按空格选择日本語，或点按语言键按默认中 ↔ EN 切换，也可在设置中加入日本語；系统输入法菜单和设置页也提供日文入口。

- 修复快速键入时首字母组字被迟到选区回报取消的多条路径：扩大回声记录、逐次记录分段写入、跳过同步重入和空会话写入；重开键盘保留当前编辑器的回声状态。原始删除或方向键使选区预测失效时，在下一次组字前重新读取选区。引擎未就绪时暂存当前会话输入，最多 256 字符。
- 候选栏右侧下箭头展开全部中文候选，主键区变为可上下滚动的候选列表；点击上箭头返回键盘，选词后自动收起。折叠时仍支持横向滚动及边缘翻页。Code 建议也支持展开，只填入文字，不执行命令。
- 横栏和展开区均使用 **22 sp**，单字、词组和拉丁字符不再各自缩字；长候选在展开区自动换行。系统字体大小仍生效。
- 设置新增「按键文字大小」**80%–140%** 和恢复默认，调整字母、数字及功能按钮。候选字号独立，图标不跟随文字缩放。
- 工程按引擎、布局、皮肤、功能和资源归类；删除旧交付、临时升级脚本、视频处理工具和废弃 iOS 调色板草稿。

## 文件分类

```text
app/src/
  main/       AndroidManifest、输入法服务、设置入口
  engine/     词库安装、JNI 接口与 ARM64 原生库、组字回声、中文转换、字段策略
  layout/     键位、候选横栏与换行布局、触控与手势、图标、文字尺寸、反馈
  skin/       皮肤色板、键帽材质、金属背景、窗口模糊
  features/   剪贴板、分类表情及本机记录
  resources/  Android res 与离线 assets：词库、OpenCC、emoji、许可
scripts/      构建辅助、回归测试、APK 核验、源码打包、资产导入、皮肤示意
tests/       纯 Java / Python 回归测试
docs/        当前设计、数据说明与本次修复说明；history/ 保存历史验收记录
vendor/       固定版本上游完整源码与 Unicode 原始数据，必须随源码保留
dist/        当前版本 APK、源码包与核验报告
```

多个 Java 源码根目录由 `app/build.gradle.kts` 显式注册。Java 包名不随分类改变，以保持现有 JNI 符号及应用数据兼容。`scripts/project_paths.py` 统一定位分类后的文件。

## 键位与设置

长按空格切换中文 / 日本語 / English / Code。中 / 日 / EN 快速切换语言；首排字母上滑输入数字，部分字母和符号长按或上滑选变体，滑回可取消。句号键上滑输入逗号。Del 长按连续删除，上滑松手清空普通字段；Code 发送 Ctrl+U。

普通字母键下滑 Z/A/X/C/V 为撤回、全选、剪切、复制、粘贴，具体编辑由当前应用处理。Code 提供 Esc、Tab、Ctrl、Alt、Home/End 与方向键；Termux 默认启用 Code。命令与代码建议为本机静态片段，真正的路径和远端补全使用终端 Tab。

普通键盘高度 220–420 dp，Code 独立 320–520 dp，横屏自动限制；按键震动可关闭或调 0–100%。皮肤保留默认明暗、浮云、简约黑白、mac 拟物、简约线条、简约 color 明暗与 iOS，新增 S4U 与淡紫像素。emoji 使用 Unicode 17 的九类 3,944 项目录，按系统字体过滤不可显示的序列。

## 隐私、来源和许可

只申请 `VIBRATE` 权限，没有联网、录音、联系人或存储权限，没有账号、云候选或上传。密码字段英文直输；私密字段不学习、不展示剪贴板。剪贴板保存在应用私有数据库，可设置保留时间、置顶或手动删除；关闭记录保留已有历史。清理的是输入法历史，不改写系统剪贴板。详见 [服务器与数据说明](docs/server-data.md)。

原生引擎和词库来自用户提供的水杉 Android 0.3.1 APK，原始九项词库/模型、三项 ARM64 库以 `provenance.json` 记录；本版恢复完整启动资源清单，全部原始摘要保持一致，原生库未重新编译。新增日文资源按同版上游 `japanese-dictionary.lock.json` 固定清单核验：66,547,565 字节的 Mozc 模型及两份许可/声明，下载只在构建准备阶段运行 `python scripts/import_japanese.py`，应用运行时不联网。匹配版本完整源码保存在 `vendor/msime-android-v0.3.1-source.tar.gz`，保留 GPL-3.0、第三方声明及 Unicode/OpenCC 许可。重新提取资产时保留本地 Java 修复。

## 构建和验证

使用 JDK 21、Gradle 8.14.3、Android SDK platform 36 / build-tools 35.0.0；Java 源语言级别 17，AGP 8.13.2，无第三方 Android UI 依赖。设置 `JAVA_HOME`、`ANDROID_HOME` 并将 Gradle 放入 PATH 后运行：

```powershell
gradle :app:assembleDebug :app:lintDebug --no-daemon
python scripts/test_local.py
Copy-Item app/build/outputs/apk/debug/app-debug.apk dist/NOPE-输入法-1.0.0.apk
python scripts/verify_apk.py dist/NOPE-输入法-1.0.0.apk
python scripts/package_source.py
python scripts/package_release.py
```

`VERSION` 是交付版本来源，Android versionCode 单独递增。覆盖现有安装前必须设置 `NOPE_DEBUG_KEYSTORE` 指向原来的开发签名文件（本机在 `D:/nope/msime-coding/target/tooling/android-user-home/debug.keystore`），避免不同终端的默认目录生成新证书。密钥不进入源码包。

缓存仅在 `.gradle/`、`app/build/`、`build/`、`target/` 生成，均排除于源码交付。

1.0.0 完成九组 Java 与四组 Python 回归、编译、Lint 和 APK/源码核验。新增回归覆盖气泡所有权、快速连按、屏幕边缘位置、私密字段、候选字号、原生完整资源契约、字体与皮肤分层配置及长剪贴板的 Unicode 安全预览。实际气泡位置、触控动画、字体缩放、中文与日文引擎运行、候选滚动和 Termux 兼容性仍需安装后验证。本次实现和验证范围见 [1.0.0 说明](docs/nope-v100-review.md)，历史背景透视修复见 [0.9.2 说明](docs/nope-v092-review.md)。
