# NOPE 皮肤标准 v1

所有9款内置皮肤使用同一套SkinStack，不复制语言键位或输入逻辑。配置位于app/src/resources/assets/skins/<id>.json，schema-v1.json记录格式；Android源码在skin目录。S4U对应retro，名字保留S4U。系统明暗、color和iOS使用light/dark分支，其余皮肤固定外观。

## 图层顺序

| z | 图层 | 实际载体 | 职责 |
|---|---|---|---|
| 0 | 背景 | 独立View与材质Drawable | 机壳底色、金属等底材 |
| 5 | 按键下方装饰 | Decorations View | 抖色、底纹、衬底；不拦触摸 |
| 10 | 键帽 | 原生Button背景Drawable | 普通/功能/模式等键面，按下状态 |
| 20 | 文字和图标 | 原生TextView/compound Drawable | 字母、功能标签、图标、无障碍名称 |
| 30 | 按键上方装饰 | 独立Decorations View | 角钉、标记、小型叠加文字；不拦触摸 |
| 40 | 交互反馈 | 原生按键状态和非触摸PopupWindow | 按下复位、字母气泡；菜单按原逻辑接管触摸 |

10与20位于同一原生控件的背景/内容绘制阶段，顺序由Android保证；装饰不画到输入文字层里。z5和z30按order从小到大绘制，后者覆盖前者，同order沿文件顺序。装饰不生成可点击遮挡，不修改按键输入；反馈始终位于装饰之上。S4U抖色在5层、角钉在30层，背景不再负责这些装饰。

## 区域

frame是整个键盘，header是候选/工具栏，body是可替换的主区域，keys是当前键盘布局，footer是底部边缘。key:letter、key:number、key:function、key:space、key:mode、key:candidate、key:toolbar、key:other对每个可见匹配按键分别定位。装饰坐标基于实际布局，不写死屏幕像素；被候选或表情面板替换的键位不参与绘制。只有背景机壳及frame装饰使用透视；header、各按键区域、候选文字和触摸坐标保持标准平面布局。

## 配置入口

设置页提供“导入皮肤配置（JSON）”和“恢复当前皮肤原始配置”。文件id选择要客制化的现有皮肤，不能改写其他应用数据。覆盖文件保存在应用私有files/skins/<id>.json；更换普通皮肤仍保留其配置，恢复只删除该皮肤覆盖文件。导入完整配置，不做含糊的跨版本深合并；从内置文件复制一份再改最方便。docs/S4U-custom-example.json是可直接导入的完整例子。

- variants.light/dark.palette：background、foreground、key、special、accent、selectedInk、shadow、outline；颜色为#RRGGBB或#AARRGGBB。没有对应分支时保留内置默认色板。
- variants里的radiusDp、depthDp：全局键帽圆角/深度。具体皮肤仍保留其材质绘制器，S4U使用台阶边，不会改成圆角键。
- keys.<区域>：fill、ink、radiusDp、depthDp、font（system/pixel）、textScale。fill/ink作用于普通状态，选中状态保留当前皮肤的明确标记；候选fill/ink也用于S4U的LCD槽。候选默认统一22sp，不受按键大小设置影响。
- perspective：target固定为background（省略时同样只作用于背景），rotationX范围-12到12度、cameraHeightFactor范围1.5到8。S4U默认背景7度、相机距离2倍高度，其余默认0度。禁止改变整套键盘的尺寸、旋转和触摸映射；旧v1配置也按背景透视解释。
- layers：id唯一，z只能5或30，order为排序号，region为上述区域，type支持rect/text/dots/rivet。x/y为区域宽高0–1的位置；width/height为区域比例，widthDp/heightDp若给出则优先。textSp、stepDp用于文本和点阵。每个元素裁切到所属区域。
- color可写十六进制或@foreground、@key、@accent、@shadow、@outline。

导入限制64KB、64个装饰元素，不执行脚本、不联网、不读取配置中的外部文件路径。配置不负责引擎、词库、隐私或剪贴板内容。所有皮肤都沿用相同状态和无障碍操作。

## S4U参考落实

保留参考图机壳背景的轻微斜俯视，按键和候选栏保持原标准尺寸与水平位置；米色斜面厚键帽、棕色像素字、居中数字提示、填实Shift、空白空格及米色语言键；顶栏正常状态为唱片、包含剪贴板/表情/设置浅绿图标的墨绿LCD、下箭头，组字时LCD显示输入串与候选。字母使用13.6KB的自有TTF，经Android标准TextView管线显示；中日文候选保留系统字形并关闭平滑以保可读性。CD只属于S4U，其他皮肤和应用图标保留N在上、ope在下，字形改为简约方正的直角线条。

标准可以继续增加新的装饰类型或字体资源，但应在SkinConfig/SkinStack中统一实现并升级schema，不能在每款皮肤或每种语言里重复编写。

## 1.0.0 淡紫像素

新增ID `lavender`，共9种内置皮肤。仍使用0/5/10/20/30/40的共用分层和KeyboardMetrics；不新增标题栏高度，不改变触摸区域。毛玻璃采用静态扩散色渐变，边缘炫光为像素线框，颗粒位于z5，页脚装饰位于z30。字形复用项目自制像素TTF；候选保留22sp系统字体以兼顾中日文阅读。JSON中可调整色板、文字和装饰，原生材质属于皮肤的固定绘制方法。
