> 历史说明：此文件记录 0.4.0 的设计过程。0.6.0 当前行为以 README.md、PRODUCT.md、nope-skin-brief.md、DESIGN.md 为准。

# 离线键面设计证据 · 0.4.0

2026-10-09。依据为用户的 Gboard 参考截图与本项目源代码，不是安装后的截图或手感验收。

| 事实 | 源码依据 | 边界 |
| --- | --- | --- |
| Android 9+ / ARM64 | app/build.gradle.kts | 未遍历设备兼容性 |
| 本地词库、学习，无联网权限 | EngineInstall / Manifest / 离线偏好 | 资产与权限已核验；真实中文效果未验证 |
| 顶部工具 / 候选、长按空格、Shell 弹窗 | OfflineImeService | 无实机浮窗位置证据 |
| 两页符号、独立数字、变体 | SymbolLayouts / VariantSelection / VariantPanel | 几何与 ASCII 可达性通过测试，实际触控未验证 |
| 取消与输入会话保护 | OfflineImeService.variantGesture / deleteGesture | 未做 Android 多指自动化 |
| 持久剪贴板、置顶、私密过滤 | ClipboardStore / ClipboardPolicy / captureClipboard | 过滤规则测试通过，跨应用系统事件仍待验证 |

固定黑色配色已移除。GlassTheme 在 Android 12+ 读取系统色，夜间使用 neutral2_700 / neutral2_600 和浅色文字；日间使用 neutral2_50 / neutral1_0 和深色文字。旧系统回退蓝灰色。GlassWindow 仅在可安全限制范围且系统允许时启用窗口模糊，其他情况用不透明渐变，不依靠模糊保证文字对比度。参考 [Android 窗口模糊官方指南](https://source.android.com/docs/core/display/window-blurs)；它不是所有 Android IME 都能获得的背景效果。

工具和返回图标在原生 Button 的左 compound drawable 中居中显示，24 dp，无可见文字；无障碍描述保留。空格留白；Shift / Del / Enter / 123 保留文字。没有依赖空文字按钮的 onDraw 重绘图标。实际设备渲染尚未验收。字号短标签 23 sp、长标签 13 sp；数字 30 sp、提示 10 sp、emoji 27 sp。键距每侧 2 dp，原生触摸间隙路由保留。Code 闲置隐藏顶栏，候选出现时展开 48 dp；控制与底部导航保持原位。

emoji 数据来源 [Unicode 17 官方 emoji-test.txt](https://www.unicode.org/Public/17.0.0/emoji/emoji-test.txt)，九类 3,944 项，源文件 SHA256 写入目录。自动检查逐条比对原始序列和分类、许可证及名称；设备实际显示按离线系统字体的 hasGlyph 筛选。GridView 复用屏内按钮，不同时创建数千个视图。

Apple Design 用于即时反馈、触发位置一致和可撤回手势；Emil / animate 用于高频输入不加定时动画；review-animations 的代码审查见 symbols-and-motion.md。屏幕导航 inset 已处理，深浅设置主题已提供，但没有真实渲染证据。

0.4.0 检查覆盖：普通编辑手势与逗号/句号映射、全量 emoji 目录与许可，以及此前的剪贴板提示按复制时间隐藏、Code 独立高度预算、震动零值 / 幅度 / 时长、候选边缘翻页 / 竖滑 / 取消规则。仅验证纯逻辑；真实马达、系统事件、候选运行、文字显示和布局仍待手机验证。VIBRATE 为可调反馈新增的唯一权限，无联网权限。
