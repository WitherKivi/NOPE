package app.msime.offline;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** 本机设置：启用、学习、可调高度与持久剪贴板，没有服务器入口。 */
public final class SetupActivity extends Activity {
    private LinearLayout content;
    private SharedPreferences preferences;
    private TextView codeHeightLabel;
    private SeekBar codeHeightSlider;
    private KeyFeedback feedback;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        preferences = getSharedPreferences("offline", MODE_PRIVATE);
        feedback = new KeyFeedback(this);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(24); content.setPadding(padding, padding, padding, padding);
        scroll.addView(content); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            content.setPadding(padding, padding + insets.getSystemWindowInsetTop(), padding, padding + insets.getSystemWindowInsetBottom()); return insets;
        });
        text("NOPE 输入法", 26);
        text("长按空格切换键盘\n中文、日文、英文与终端输入，都在本机处理。", 16);
        TextView status = text("正在准备中日文词库…", 14);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        button("启用输入法", () -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        button("选择输入法", () -> getSystemService(InputMethodManager.class).showInputMethodPicker());
        section("输入语言");
        String[] languageIds = {"CHINESE", "JAPANESE", "ENGLISH", "CODING"};
        String[] languageNames = {"中文拼音", "日本語（ローマ字）", "English", "Code"};
        button("选择默认输入语言", () -> {
            int selected = java.util.Arrays.asList(languageIds).indexOf(preferences.getString("mode", "CHINESE"));
            new android.app.AlertDialog.Builder(this).setTitle("默认输入语言")
                .setSingleChoiceItems(languageNames, Math.max(0, selected), (dialog, index) -> {
                    preferences.edit().putString("mode", languageIds[index]).apply(); dialog.dismiss();
                }).setNegativeButton("取消", null).show();
        });
        TextView quickSummary = text(getString(R.string.quick_languages_summary, QuickLanguages.summary(preferences.getString("quick_languages", QuickLanguages.DEFAULT))), 16);
        button("调整快速切换语言", () -> {
            String[] ids = QuickLanguages.ids();
            String[] names = java.util.Arrays.stream(ids).map(QuickLanguages::name).toArray(String[]::new);
            java.util.List<String> savedIds = QuickLanguages.parse(preferences.getString("quick_languages", QuickLanguages.DEFAULT));
            boolean[] checked = new boolean[ids.length];
            for (int i = 0; i < ids.length; i++) checked[i] = savedIds.contains(ids[i]);
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).setTitle("点按语言键的快速切换")
                .setMultiChoiceItems(names, checked, (list, index, selected) -> checked[index] = selected)
                .setPositiveButton("保存", null).setNegativeButton("取消", null).create();
            dialog.setOnShowListener(ignored -> dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                java.util.List<String> selected = new java.util.ArrayList<>();
                for (int i = 0; i < ids.length; i++) if (checked[i]) selected.add(ids[i]);
                if (selected.size() < 2) {
                    android.widget.Toast.makeText(this, "请至少选择两种键盘", android.widget.Toast.LENGTH_SHORT).show(); return;
                }
                String value = String.join(",", selected);
                preferences.edit().putString("quick_languages", value).apply();
                quickSummary.setText(getString(R.string.quick_languages_summary, QuickLanguages.summary(value))); dialog.dismiss();
            }));
            dialog.show();
        });
        button("恢复中英快速切换", () -> {
            preferences.edit().putString("quick_languages", QuickLanguages.DEFAULT).apply();
            quickSummary.setText(getString(R.string.quick_languages_summary, QuickLanguages.summary(QuickLanguages.DEFAULT)));
        });
        text("点按右侧语言键默认切换中文 ↔ English；上方可选择两种或更多键盘循环切换。长按空格始终可选择全部键盘。日文使用 QWERTY 罗马字：nihon → にほん；空格进入汉字转换，再按空格切换候选，回车确认。未转换时回车提交假名。繁简设置只作用于中文。", 14);
        section("键盘高度");
        int configured = preferences.getInt("height", 0);
        int defaultHeight = normalHeight(0);
        TextView height = text(configured == 0 ? "默认 · " + defaultHeight + " dp" : configured + " dp", 16);
        text("默认比例参照键盘截图。可调范围 220–420 dp，横屏会自动限制高度。", 14);
        SeekBar slider = new SeekBar(this); slider.setMax(200); slider.setProgress((configured == 0 ? defaultHeight : configured) - 220);
        slider.setContentDescription("键盘高度"); content.addView(slider, new LinearLayout.LayoutParams(-1, dp(48)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int value = 220 + progress; height.setText(value + " dp"); preferences.edit().putInt("height", value).apply(); refreshCodeHeight();
            }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { }
        });
        button("恢复默认高度", () -> { preferences.edit().putInt("height", 0).apply(); slider.setProgress(defaultHeight - 220); height.setText("默认 · " + defaultHeight + " dp"); refreshCodeHeight(); });
        section("Code 键盘高度");
        text("Code 独立调节，默认增加 120 dp，给控制键和底部方向键留出空间。工具栏隐藏，打字时展开候选栏；设置高度包含候选栏。横屏自动收紧。", 14);
        codeHeightLabel = text("", 16);
        codeHeightSlider = new SeekBar(this); codeHeightSlider.setMax(200); codeHeightSlider.setContentDescription("Code 键盘高度");
        content.addView(codeHeightSlider, new LinearLayout.LayoutParams(-1, dp(48)));
        refreshCodeHeight();
        codeHeightSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int value = 320 + progress; codeHeightLabel.setText(value + " dp"); preferences.edit().putInt("code_height", value).apply();
            }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { }
        });
        button("恢复 Code 默认高度", () -> { preferences.edit().putInt("code_height", 0).apply(); refreshCodeHeight(); });
        section("按键文字");
        int textPercent = KeyboardText.percent(preferences.getInt("key_text_scale", KeyboardText.DEFAULT_PERCENT));
        TextView textSizeLabel = text("按键文字大小 · " + textPercent + "%", 16);
        SeekBar textSizeSlider = new SeekBar(this); textSizeSlider.setMax(KeyboardText.MAX_PERCENT - KeyboardText.MIN_PERCENT);
        textSizeSlider.setProgress(textPercent - KeyboardText.MIN_PERCENT); textSizeSlider.setContentDescription("按键文字大小，80% 到 140%");
        content.addView(textSizeSlider, new LinearLayout.LayoutParams(-1, dp(48)));
        textSizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) textSizeLabel.setText("按键文字大小 · " + (KeyboardText.MIN_PERCENT + progress) + "%");
            }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) {
                preferences.edit().putInt("key_text_scale", KeyboardText.MIN_PERCENT + bar.getProgress()).apply();
            }
        });
        button("恢复默认文字大小", () -> {
            preferences.edit().putInt("key_text_scale", KeyboardText.DEFAULT_PERCENT).apply();
            textSizeSlider.setProgress(KeyboardText.DEFAULT_PERCENT - KeyboardText.MIN_PERCENT);
            textSizeLabel.setText("按键文字大小 · 100%");
        });
        text("调整字母、数字及功能按钮的文字，窄键会适当缩小以免裁切。候选文字统一为 22 sp，不随按键文字大小变化。", 14);
        section("按键反馈");
        toggle("按压动效", "press_effects", true);
        toggle("按键预览气泡", "key_preview", true);
        text("字母与数字按下时在键帽上方预览，松开后短暂保留；滑动选变体时隐藏。私密字段不显示气泡。系统关闭动画时仅保留即时按下状态。", 14);
        toggle("按键震动", "haptics", true);
        TextView strengthLabel = text("震动强度 · " + preferences.getInt("haptic_strength", 35) + "%", 16);
        SeekBar strength = new SeekBar(this); strength.setMax(100); strength.setProgress(preferences.getInt("haptic_strength", 35));
        strength.setContentDescription("按键震动强度，零为关闭"); content.addView(strength, new LinearLayout.LayoutParams(-1, dp(48)));
        strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                strengthLabel.setText("震动强度 · " + progress + "%"); preferences.edit().putInt("haptic_strength", progress).apply();
            }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { feedback.pulse(); }
        });
        text("松开滑块体验反馈。0% 不震动；不支持强度控制的手机会调整震动时长。效果受手机马达和系统设置影响。", 14);
        section("本地数据");
        toggle("记忆选词习惯", "learning", true);
        toggle("保存剪贴板历史", "clipboard", true);
        TextView retention=text(ClipboardSettings.summary(this),16);
        button("剪贴板保留时间",()->ClipboardSettings.show(this,()->retention.setText(ClipboardSettings.summary(this))));
        text("保留时间从最近一次复制计算，置顶条目不自动清除。输入法运行时每分钟检查，重开时补清过期条目。返回或成功粘贴后隐藏最近复制提示。私密字段和敏感内容不收入历史。",14);
        section("皮肤与中文输出");
        TextView skin=text(SkinPalette.NAMES[SkinPalette.index(preferences.getString("skin","system"))],16);
        button("选择键盘皮肤",()->new android.app.AlertDialog.Builder(this).setTitle("皮肤")
            .setSingleChoiceItems(SkinPalette.NAMES,SkinPalette.index(preferences.getString("skin","system")),(dialog,index)->{
                preferences.edit().putString("skin",SkinPalette.IDS[index]).apply();skin.setText(SkinPalette.NAMES[index]);dialog.dismiss();
            }).setNegativeButton("取消",null).show());
        button("导入皮肤配置（JSON）",()->{
            android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(android.content.Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent,651);
        });
        button("恢复当前皮肤原始配置",()->{
            try{SkinFiles.reset(this);android.widget.Toast.makeText(this,"已恢复当前皮肤",android.widget.Toast.LENGTH_SHORT).show();}
            catch(Exception error){android.widget.Toast.makeText(this,"恢复失败："+error.getMessage(),android.widget.Toast.LENGTH_LONG).show();}
        });
        text("皮肤可分别配置背景、按键区域、文字、上下装饰层与透视。导入会覆盖配置文件指定的皮肤，可随时恢复。",14);
        toggle("繁体中文输出","traditional",false);
        text("默认明暗跟随系统。浮云、简约黑白、mac 拟物、简约线条、简约 color、iOS、S4U 与淡紫像素可自由切换；候选和上屏文字使用同一繁简规则。淡紫像素的毛玻璃使用本地渐变材质，不采样其他应用画面。",14);
        section("手势与 Coding");
        text("普通键盘：下滑 Z 撤回、A 全选、X 剪切、C 复制、V 粘贴。松手执行，滑回原位取消。\n句号键上滑输入逗号；右侧语言键显示当前模式，默认中英互切，可在输入语言设置中调整。Code 控制行与数字面板的 Tab 均在最左侧。空格留白。表情包含常用和九个分类，常用按次数排序，输入后保持打开。",15);
        text("iOS 风格：灰色键盘底、独立键帽、轻阴影，明暗跟随系统。字母键与功能键用不同灰阶；按下即时反馈。窗口模糊可用且范围只覆盖键盘时保留轻透材质，否则用清晰的纯色背景。", 14);
        text("长按空格：选择拼音 / 日本語 / English / Code\n候选栏左右滑动浏览，在边缘继续滑动可翻页；点击右侧下箭头展开全部候选，上下滑动浏览，上箭头返回键盘\n首排字母上滑：输入右上角的数字\n符号上滑或长按：拖动选变体，松手输入；滑回原位可取消上滑\n符号页点 123：打开独立数字面板\nDel 上滑后松手：普通输入框清空内容；Code 发送 Ctrl+U 清当前命令行\n长按 Shell：选择命令或代码建议类型\n点按建议仅填入文字，Enter 才执行。", 15);
        section("试打");
        EditText tryout = new EditText(this); tryout.setId(R.id.tryout); tryout.setHint("中文 nihao → 你好；日文 nihon → にほん / 日本"); tryout.setMinLines(3);
        tryout.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        content.addView(tryout, new LinearLayout.LayoutParams(-1, -2));
        button("来源、许可和数据说明", () -> new android.app.AlertDialog.Builder(this)
            .setTitle("本地引擎与数据")
            .setMessage("复用你提供的水杉 0.3.1 本地引擎和词库，新增离线键盘宿主。日文词典使用同版本固定清单的 Mozc 数据，字典声明与许可证随包保留。源码与许可随项目提供，来源和第三方声明在 APK assets 内。\n\n本机保存词库副本、学习、设置和剪贴板历史。没有联网权限、账号、云同步、云剪贴板、录音或使用报告；Coding 不保存命令历史。清除应用数据或卸载会删除这些本机数据。")
            .setPositiveButton("知道了", null).show());
        EngineInstall.prepare(this, (options, error) -> runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) status.setText(options == null ? error : "本地中日文词库已就绪");
        }));
    }
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){
        super.onActivityResult(request,result,data);
        if(request==651&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            try{String name=SkinFiles.install(this,data.getData());android.widget.Toast.makeText(this,"已应用 "+name,android.widget.Toast.LENGTH_LONG).show();recreate();}
            catch(Exception error){android.widget.Toast.makeText(this,"配置未导入："+error.getMessage(),android.widget.Toast.LENGTH_LONG).show();}
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int normalHeight(int preference) {
        android.content.res.Configuration config = getResources().getConfiguration();
        return KeyboardMetrics.height(preference, config.screenWidthDp, config.screenHeightDp);
    }
    private void refreshCodeHeight() {
        if (codeHeightLabel == null || codeHeightSlider == null) return;
        int configured = preferences.getInt("code_height", 0);
        int height = KeyboardMetrics.codeHeight(configured, normalHeight(preferences.getInt("height", 0)), getResources().getConfiguration().screenHeightDp);
        codeHeightLabel.setText((configured == 0 ? "默认 · " : "") + height + " dp");
        codeHeightSlider.setProgress(Math.max(0, Math.min(200, height - 320)));
    }
    private TextView text(String value, int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setPadding(0, dp(6), 0, dp(10));
        content.addView(view); return view;
    }
    private void section(String value) { TextView view = text(value, 18); view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); view.setPadding(0, dp(24), 0, dp(8)); }
    private void button(String value, Runnable action) {
        Button view = new Button(this); view.setAllCaps(false); view.setText(value); view.setMinHeight(dp(48));
        view.setOnClickListener(v -> action.run()); content.addView(view, new LinearLayout.LayoutParams(-1, -2));
    }
    private void toggle(String value, String name, boolean defaultValue) {
        Switch view = new Switch(this); view.setText(value); view.setTextSize(16); view.setMinHeight(dp(56));
        view.setChecked(preferences.getBoolean(name, defaultValue));
        view.setOnCheckedChangeListener((button, enabled) -> preferences.edit().putBoolean(name, enabled).apply()); content.addView(view);
    }
}
