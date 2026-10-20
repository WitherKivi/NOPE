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
    private boolean night;
    private int ink,secondary,accent;
    @Override public void onCreate(Bundle saved) {
        night=(getResources().getConfiguration().uiMode&0x30)==0x20;
        setTheme(night?R.style.SettingsNightTheme:R.style.SettingsDayTheme);
        super.onCreate(saved);
        ink=night?0xfff3f1f6:0xff26242b;secondary=night?0xffc1bbc9:0xff68616f;accent=night?0xffc6b3ef:0xff665383;
        getWindow().setStatusBarColor(night?0xff37373e:0xfffaf9f5);getWindow().setNavigationBarColor(night?0xff25252c:0xffe3e3e9);
        getWindow().getDecorView().setSystemUiVisibility(night?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        preferences = getSharedPreferences("offline", MODE_PRIVATE);
        feedback = new KeyFeedback(this);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);scroll.setBackground(new SettingsMaterial(night,false,getResources().getDisplayMetrics().density));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(22); content.setPadding(padding, padding, padding, padding);
        scroll.addView(content); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            content.setPadding(padding, padding + insets.getSystemWindowInsetTop(), padding, padding + insets.getSystemWindowInsetBottom()); return insets;
        });
        text("NOPE 输入法", 26);
        text("中文 · 日本語 · English · Code", 16);
        TextView status = text("正在准备中日文词库…", 14);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        button("启用输入法", () -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        button("选择输入法", () -> getSystemService(InputMethodManager.class).showInputMethodPicker());
        section("输入语言");
        String[] languageIds = {"CHINESE", "JAPANESE", "ENGLISH", "CODING"};
        String[] languageNames = {"中文拼音", "日本語（ローマ字）", "English", "Code"};
        button("选择默认输入语言", () -> {
            int selected = java.util.Arrays.asList(languageIds).indexOf(preferences.getString("mode", "CHINESE"));
            dialog().setTitle("默认输入语言")
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
            android.app.AlertDialog dialog = dialog().setTitle("点按语言键的快速切换")
                .setMultiChoiceItems(names, checked, (list, index, selected) -> checked[index] = selected)
                .setPositiveButton("保存", null).setNegativeButton("取消", null).create();
            dialog.setOnShowListener(ignored -> {frostDialog(dialog);dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                java.util.List<String> selected = new java.util.ArrayList<>();
                for (int i = 0; i < ids.length; i++) if (checked[i]) selected.add(ids[i]);
                if (selected.size() < 2) {
                    android.widget.Toast.makeText(this, "请至少选择两种键盘", android.widget.Toast.LENGTH_SHORT).show(); return;
                }
                String value = String.join(",", selected);
                preferences.edit().putString("quick_languages", value).apply();
                quickSummary.setText(getString(R.string.quick_languages_summary, QuickLanguages.summary(value))); dialog.dismiss();
            });});
            dialog.show();
        });
        button("恢复中英快速切换", () -> {
            preferences.edit().putString("quick_languages", QuickLanguages.DEFAULT).apply();
            quickSummary.setText(getString(R.string.quick_languages_summary, QuickLanguages.summary(QuickLanguages.DEFAULT)));
        });
        text("点按语言键快速切换；长按空格选择全部键盘。", 14);
        section("键盘高度");
        int configured = preferences.getInt("height", 0);
        int defaultHeight = normalHeight(0);
        TextView height = text(configured == 0 ? "默认 · " + defaultHeight + " dp" : configured + " dp", 16);
        text("普通键盘高度，可随时恢复默认。", 14);
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
        text("独立调整 Code 键盘高度。", 14);
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
        text("调整按键文字；候选保持统一字号。", 14);
        section("按键反馈");
        toggle("按压动效", "press_effects", true);
        toggle("按键预览气泡", "key_preview", true);
        text("气泡预览本次按键，私密输入不显示。", 14);
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
        text("松开滑块体验震动。", 14);
        section("本地数据");
        toggle("记忆选词习惯", "learning", true);
        toggle("保存剪贴板历史", "clipboard", true);
        TextView retention=text(ClipboardSettings.summary(this),16);
        button("剪贴板保留时间",()->ClipboardSettings.show(this,()->retention.setText(ClipboardSettings.summary(this))));
        text("历史仅保存在本机；📌 条目不自动过期。",14);
        section("皮肤与中文输出");
        TextView skin=text(SkinPalette.NAMES[SkinPalette.index(preferences.getString("skin","system"))],16);
        button("选择键盘皮肤",()->dialog().setTitle("皮肤")
            .setSingleChoiceItems(SkinPalette.NAMES,SkinPalette.index(preferences.getString("skin","system")),(dialog,index)->{
                preferences.edit().putString("skin",SkinPalette.IDS[index]).apply();skin.setText(SkinPalette.NAMES[index]);dialog.dismiss();
            }).setNegativeButton("取消",null).show());
        button("导入皮肤配置",()->{
            android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(android.content.Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent,651);
        });
        button("恢复当前皮肤原始配置",()->{
            try{SkinFiles.reset(this);android.widget.Toast.makeText(this,"已恢复当前皮肤",android.widget.Toast.LENGTH_SHORT).show();}
            catch(Exception error){android.widget.Toast.makeText(this,"恢复失败："+error.getMessage(),android.widget.Toast.LENGTH_LONG).show();}
        });
        text("可导入自定义皮肤，或恢复当前皮肤。",14);
        toggle("繁体中文输出","traditional",false);
        text("默认皮肤跟随系统明暗；也可选择固定风格。",14);
        section("手势与 Coding");
        text("下滑 Z/A/X/C/V：撤回、全选、剪切、复制、粘贴。",15);
        text("上滑输入数字或标点；长按选择变体。", 14);
        text("候选箭头展开列表；Code 左侧 Tab 补全或缩进。", 15);
        section("试打");
        EditText tryout = new EditText(this); tryout.setId(R.id.tryout); tryout.setHint("中文 nihao → 你好；日文 nihon → にほん / 日本"); tryout.setMinLines(3);tryout.setTextColor(ink);tryout.setHintTextColor(secondary);tryout.setPadding(dp(16),dp(12),dp(16),dp(12));tryout.setBackground(new SettingsMaterial(night,true,getResources().getDisplayMetrics().density));
        tryout.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        content.addView(tryout, new LinearLayout.LayoutParams(-1, -2));
        button("关于与隐私", () -> dialog()
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
    private android.app.AlertDialog.Builder dialog(){
        return new android.app.AlertDialog.Builder(this){
            @Override public android.app.AlertDialog show(){android.app.AlertDialog result=super.show();frostDialog(result);return result;}
        };
    }
    private void frostDialog(android.app.AlertDialog dialog){
        if(dialog.getWindow()!=null)dialog.getWindow().setBackgroundDrawable(new SettingsMaterial(night,true,getResources().getDisplayMetrics().density));
        for(int id:new int[]{android.app.AlertDialog.BUTTON_POSITIVE,android.app.AlertDialog.BUTTON_NEGATIVE})if(dialog.getButton(id)!=null)dialog.getButton(id).setTextColor(accent);
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
        TextView view = new TextView(this); view.setTextColor(size<=14?secondary:ink);view.setText(value); view.setTextSize(size); view.setPadding(0, dp(6), 0, dp(10));
        content.addView(view); return view;
    }
    private void section(String value) { TextView view = text(value, 18); view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); view.setPadding(0, dp(24), 0, dp(8)); }
    private void button(String value, Runnable action) {
        Button view = new Button(this); view.setAllCaps(false); view.setText(value); view.setMinHeight(dp(48));
        view.setStateListAnimator(null);view.setElevation(0);
        view.setTextColor(ink);view.setTextSize(16);view.setMinimumHeight(dp(52));view.setPadding(dp(16),dp(10),dp(16),dp(10));view.setBackgroundTintList(null);
        android.graphics.drawable.Drawable glass=new SettingsMaterial(night,true,getResources().getDisplayMetrics().density);
        view.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(night?0x309d8fbc:0x206c5a85),glass,null));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(4),0,dp(4));
        view.setOnClickListener(v -> action.run()); content.addView(view,params);
    }
    private void toggle(String value, String name, boolean defaultValue) {
        Switch view = new Switch(this); view.setText(value); view.setTextSize(16); view.setMinHeight(dp(56));
        view.setTextColor(ink);view.setPadding(dp(16),dp(8),dp(16),dp(8));view.setBackground(new SettingsMaterial(night,true,getResources().getDisplayMetrics().density));
        view.setChecked(preferences.getBoolean(name, defaultValue));
        view.setOnCheckedChangeListener((button, enabled) -> preferences.edit().putBoolean(name, enabled).apply()); LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(4),0,dp(4));content.addView(view,params);
    }
}
