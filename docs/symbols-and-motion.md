> 历史说明：此文件记录 0.4.0 的设计过程。0.6.0 当前行为以 README.md、PRODUCT.md、nope-skin-brief.md、DESIGN.md 为准。

# 符号与动效核验 · 0.4.0

## 公开参考

实际查阅的是 [AOSP LatinIME MoreKeysDetector.java](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/main/java/src/com/android/inputmethod/keyboard/MoreKeysDetector.java)，2026-10-09，页面 blob 5a9d4755fbdf750d6c2cbf7f5c26cf8fb5e1c830。其思路是允许拖动选择候选，并对候选边缘给予滑动容差。未获得完整 Gboard App 源码，没有把 LatinIME 宣称为 Gboard；本项目的键位来自用户截图，几何和浮窗代码自行实现，未复制该文件源码。

独立数字面板将截图右侧的缩进形状明确标为 Tab，便于终端使用。Code 普通点按使用 ASCII；变体与扩展符号只在用户明确操作时插入。

## Emil / Apple / animate / review-animations 核验

| Before | After | Why |
| --- | --- | --- |
| 图标下面有中文副标题 | 24 dp 纯图标，原生 Button 的 compound drawable 居中；Shift / Del / Enter 保留标签 | 满足简洁要求，保留无障碍名称，图标不依赖空文字 onDraw |
| Code 控制及箭头挤在同一行 | 顶部五个控制键，底部六个导航键，默认额外 120 dp | 留给手指清楚的独立目标，默认竖屏保持主键高度 |
| 最近复制提示返回和粘贴后重新出现 | 复制时间对应的隐藏标记持久保存，数据库历史独立保留 | 一次复制只在需要时提示，新的复制恢复提示 |
| 候选「更多」按钮 | 系统横向滚动与边缘翻页，手势可滑回取消 | 浏览连续，不额外占用候选栏；无计时翻页动画 |
| 系统固定强度反馈 | KeyButton 统一按下反馈，可关闭 / 0–100% | 反馈遵守用户设置，支持幅度控制或时长降级 |
| 键盘弹跳 / 缩放会打断高频操作 | 即时按下状态；浮窗动画为 0；保持系统滚动 | 高频输入不引入定时装饰动画 |
| 普通编辑需要展开菜单 | Z / A / X / C / V 下滑后松手执行；滑回、横移、多指或输入会话变化取消 | 不在按下时执行剪切，手势过程可反悔；即时提示，不加动画 |
| 20 个 emoji 直接创建全部按钮 | 九类目录，系统 GridView 复用可见格子；选中后保持面板 | 不同时创建数千个控件，连续输入保持空间位置 |
| Code 顶栏常驻 | 闲置隐藏，仅输入、建议或错误时即时展开 | 不占终端空间，不给高频候选栏增加高度动画 |
| 固定近黑色键面 | 系统取色的渐变玻璃材质，安全范围内使用系统窗口模糊 | 保留文字清晰度；无需截屏或对键盘内容应用模糊 |

**Interruptibility & timing**：编辑手势与候选页在松手时执行，手指反向可取消；输入会话与 generation 校验拒绝过期操作。剪贴板后台回报不重建活跃候选栏。Code 候选栏即时展开或隐藏，保留系统窗口自身的输入法过渡，应用不增加定时高度动画。

**Accessibility**：纯图标和留白空格保留描述；编辑手势另有 TalkBack 快捷动作。候选栏保留前后滚动动作；没有新计时运动或 hover 依赖。按键震动有独立开关与零强度；不会因系统震动调用失败阻断输入。

判定：Approve（代码层面的动效审查）。输入及浮窗不加定时动画；反馈目的为即时按下确认，没有新增动画库。系统滚动属于手指直接浏览，未另加弹跳、缩放或视觉过渡。真实渲染、震动效果及触控手感尚未做真机验收。

源码定位：`OfflineImeService.java:375`（可取消编辑与符号手势）、`:478`（即时按下状态）、`:560`（分类与复用 emoji 网格）、`:602`（编辑操作与粘贴）、`:945`（Code 顶栏可见性与候选）；`KeyboardIcon.java:16`（原生 compound drawable）、`GlassWindow.java:52`（模糊边界与系统状态）。

## 震动依据

使用 [Android Vibrator](https://developer.android.com/reference/android/os/Vibrator#hasAmplitudeControl()) 查询幅度控制能力；[VibrationEffect.createOneShot](https://developer.android.com/reference/android/os/VibrationEffect#createOneShot(long,%20int)) 支持 1–255 幅度或设备默认幅度。应用仅增加 VIBRATE 权限。百分比是本应用调节范围，不表示所有手机相同的物理振幅。

## 真机待验收

工具纯图标与留白空格、小屏 / 大字体 / 横屏、逗号/句号上滑、中英切换、五个编辑手势与撤回宿主支持、Code 候选展开与底部四方向、emoji 分类及系统字体、动态配色与模糊范围、候选首尾页及返回页、滑回取消、TalkBack、震动、剪贴板提示隐藏、Termux 组合键。源码和安装包校验不能替代这些结果。
