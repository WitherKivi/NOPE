# NOPE0.9.1：引擎恢复、S4U与统一皮肤标准

根因：0.9.0删除了msime-wubi.db和sentence-model.safetensors，虽然engine层的merge_split_wubi允许缺失且神经重排未启用，但JNI prepare_host先按Edition::full内置资源锁执行verify_resources_once。这层启动校验要求两文件齐全，因此引擎永远不能就绪。已从用户原始APK恢复并核对SHA256，取消ResourcePrune，runtime清单与原版full锁一致；原生准备成功之后才写宿主校验标记，失败重试会真正重新准备，并增加无输入文本的错误诊断。

手机PLZ110连接期间，实际JNI回归的nihao→你好、nihon→日本通过。字母旧的Canvas/ReplacementSpan方案未通过真实Button绘制检查，未宣称成功；最终改为13.6KB自有像素TTF与TextView标准文字管线。TTF实际cmap、loca和glyf轮廓覆盖全部拉丁大小写、数字、中/日，通过资源测试。手机随后被暂时断开，最终字体、S4U透视和分层的视觉/触控待重连检查。

S4U按用户图调整：7度轻微斜俯视，厚斜面键帽、棕色像素字、居中数字提示、填实Shift，米色语言键、空白空格；顶部只用唱片入口、灰绿LCD和下箭头，组字时显示输入串、光标和候选。只有S4U使用唱片，其他皮肤与应用图标仍为N/ope。

为客制化建立统一SkinStack，所有内置皮肤均走同一层级：0背景、5下装饰、10键帽、20文字图标、30上装饰、40交互反馈；坐标取实际区域，装饰不拦触摸。JSON可配置色板、按键角色、圆角/深度、字体/文字比例、透视与多个装饰；设置可导入完整JSON并恢复。S4U的抖色和角钉已从背景材质拆到独立装饰层，作为完整示例。格式、层级及可导入样例见skin-standard-v1.md与S4U-custom-example.json。

编译/Lint、逻辑回归、原生资源契约、字体表、所有皮肤JSON、APK签名/权限/原资产/日文许可、源码包分别核验。最终报告保存在dist。尚未验收部分明确记录，不以静态图代替原生手机截图。
