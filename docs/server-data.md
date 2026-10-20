# 哪些数据需要服务器，为什么

**这一版不上传任何应用数据。基础打字、中日文候选、本地选词记忆、中文拼写纠错、Coding 键位和静态建议，都不需要服务器。** 词库和模型文件是开发者打包给手机的资源，不是需要从手机上传的个人数据。

下表根据水杉上游源码 `87baa4ba78d2d780d8b43d6931e9bdef9032f778` 的客户端接口归纳，是为后续开发划分范围；本版实际复用的原生二进制来自 Android 0.3.1。不同上游版本功能可能不同。接口字段表示客户端可能发送什么，不能据此证明服务器实际留存、用途执行情况或所有第三方政策。

| 延后功能 | 会发送的数据 | 用途 / 什么时候才需要 | 本版处理 |
|---|---|---|---|
| 登录与个人资料 | 登录凭证/令牌、匿名账号的随机身份与凭证；用户编辑的昵称、头像 | 登录、鉴权、账户资料、绑定云服务 | 无账号入口 |
| 设置同步 | 可同步的输入法设置、文档版本号；包括适用的皮肤设置 | 多设备恢复相同配置 | 仅本机设置 |
| 个人词库同步 | 用户词条及编码/拼音、权重、学习增量、版本/来源信息 | 换机或多设备保留选词习惯 | 本机学习，无快照上传 |
| 常用语同步 | 短语文本、ID、分组和排列位置 | 多设备共享常用短语 | 暂不实现 |
| 云剪贴板 | 剪贴板文本及设备标记、时间、置顶等元数据 | 手机/电脑之间复制粘贴 | 读取并持久保存本地纯文本剪贴板；无云功能 |
| 云候选 | 当前需要转换的输入串；AI 候选还可能包含请求上下文 | 扩充本地词库之外的候选 | 显式关闭，无网络传输 |
| AI 润色、回复与翻译 | 用户交给功能的文字、系统提示/模型参数；翻译发送待翻译候选及语言 | 调用远端模型/翻译服务 | 暂不实现 |
| 云语音识别 | 音频及识别语言/服务参数，识别后文字可按用户选择再润色 | 远端语音识别 | 无录音权限或语音功能 |
| 语音样本贡献 | 明确同意后上传 WAV、转写文字、语言、识别器、时长和应用版本 | 收集语音识别样本，区别于仅为识别发送音频 | 暂不实现 |
| 使用统计与崩溃报告 | 活跃/会话/崩溃事件、平台、版本、随机安装标识、错误摘要与堆栈等 | 判断使用情况、定位稳定性问题 | 显式关闭，无统计宿主 |
| 开发者诊断 / MCP 云访问 | 脱敏配置快照、崩溃摘要/堆栈、白名单事件种类/时间/耗时 | 用户授权后辅助排障和远程诊断 | 暂不实现 |
| 反馈、社区投稿与 AI 皮肤 | 用户填写的反馈、选附诊断、截图；发布的皮肤/短语等内容；皮肤生成描述/图像 | 用户反馈、分享作品、在线生成资源 | 暂不实现 |

资源和版本检查主要是**下载**。请求仍会让服务器看到请求资源/版本/平台以及网络地址等连接信息，但不意味着需要上传全部输入内容。离线版随包带词库，连这类请求也没有。

## 几个需要分清的点

- **“记忆”不等于上传输入记录。** 本地学习可以记录词条、频次和候选习惯；跨设备同步才需要上传个人词库快照。本版不保存 Coding 命令历史。
- **纠错可以本地完成。** 上游全拼默认启用邻键和相邻字母换位纠错；本版沿用本地引擎偏好和纠错候选，不会为了纠错上传命令。工程中未单独加入新 AI 纠错模型。
- **上下文访问与上下文上传是两件事。** 输入法与目标应用交互以写入组字和处理光标，不代表网络发送。以后增加 AI 功能时应明确列出将发出的内容、服务接收方和触发动作。
- **不能把原云剪贴板称为端到端加密。** 上游 `CloudClipboardApi` 注释明确写经 HTTPS 传输、服务端明文保存。未来若自行实现，应先确定是否采用客户端端到端加密。
- **统计不等于完整键盘记录。** 已检查的 `TelemetryEvent` 没有完整键入流字段；上游默认 usage reporting 为开启。本版显式关闭并且没有上传代码。不能把源码字段列表当成对原服务全部行为的保证。
- **诊断有自己的边界。** 上游 `DiagnosticsApi` 按白名单只重建事件时间、种类、耗时，说明请求不含文本、拼音、候选或按键字面值；仍有配置和错误信息，因此应由用户主动开启，并提供删除和期限控制。

## 后续服务器开发范围

目前必须上传的数据：**没有**。

如以后需要换机/跨设备记忆，优先做可选的设置、个人词库和短语同步。若要智能代码补全，单独做明确触发的 AI 请求：只发送用户选定的代码/命令片段，不扫描整个终端会话，不默认收集命令历史。路径、命令和变量保持原样，建议由用户确认插入。账号、云剪贴板、语音、遥测、诊断可分别排期，不应成为基本打字的依赖。

未来每一项上传能力都应定义独立开关、触发动作、准确字段、接收服务、保存期限、删除方式和失败降级；这些是未来产品设计建议，本版没有用虚构后端替代实现。

## 源码依据

以下链接固定到这次检查的上游提交，便于后续按同一版本核对。

- [同步接口 SyncApi](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/platforms/android/java/app/msime/android/account/SyncApi.java)、[同步字段策略 SyncMergePolicy](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/platforms/android/java/app/msime/android/account/SyncMergePolicy.java)
- [云剪贴板 CloudClipboardApi](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/platforms/android/java/app/msime/android/account/CloudClipboardApi.java)
- [云候选请求 providers.rs](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/crates/input-runtime/src/providers.rs)、[查询结构 types.rs](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/crates/input-runtime/src/types.rs)
- [AI 润色传输](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/platforms/android/java/app/msime/android/voice/AiPolishHttpTransport.java)、[候选翻译](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/platforms/android/java/app/msime/android/voice/BackendTranslationClient.java)
- [语音贡献 VoiceContributionApi](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/platforms/android/java/app/msime/android/voice/VoiceContributionApi.java)
- [遥测字段 telemetry.rs](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/crates/client-core/src/telemetry.rs)、[遥测宿主 Telemetry](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/platforms/android/java/app/msime/android/core/Telemetry.java)
- [诊断上传 DiagnosticsApi](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/platforms/android/java/app/msime/android/account/DiagnosticsApi.java)、[反馈接口 FeedbackApi](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/platforms/android/java/app/msime/android/account/FeedbackApi.java)
- [默认偏好及本地纠错配置](https://github.com/metasequoiaime/msime/blob/87baa4ba78d2d780d8b43d6931e9bdef9032f778/crates/client-core/src/preferences.rs)

0.8.0日文使用随包的Mozc离线模型。罗马字、假名和汉字候选均在本机处理；词典及许可证仅由开发者在构建准备阶段下载并核对固定摘要。手机不请求联网权限，也不上传日文输入。
