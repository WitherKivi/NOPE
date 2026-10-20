package app.msime.offline;

import android.inputmethodservice.InputMethodService;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.text.TextUtils;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.TextView;
import app.msime.android.CodingCompletion;
import app.msime.android.CodingInput;
import app.msime.android.CodingKeyMap;
import app.msime.android.CodingLayout;
import app.msime.android.NativeClient;
import app.msime.android.SelectionEchoTracker;
import org.json.JSONArray;
import org.json.JSONObject;

/** 最小离线宿主：中文交给水杉本地引擎，Coding 直接半角上屏。 */
public final class OfflineImeService extends InputMethodService {
    private enum Mode { CHINESE, ENGLISH, CODING, JAPANESE }
    private boolean engineMode() { return mode == Mode.CHINESE || mode == Mode.JAPANESE; }
    private final JapaneseInput japanese = new JapaneseInput();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CodingCompletion completion = new CodingCompletion();
    private final SelectionEchoTracker echo = new SelectionEchoTracker();
    private LinearLayout root, candidates, keys, top;
    private SkinStack skinStack;
    private View skinFooter;
    private CandidateStrip candidateScroll;
    private FrameLayout body;
    private ScrollView expandedCandidates;
    private boolean candidateExpanded;
    private JSONObject candidateSnapshot;
    private long snapshotSession, expandedGeneration = -1;
    private final java.util.ArrayList<CandidateOption> candidateOptions = new java.util.ArrayList<>();
    private record CandidateOption(String text, String description, Runnable action) { }
    private Button ctrlKey, altKey, spaceKey, shiftKey;
    private PopupWindow popover;
    private PopupWindow gestureHint;
    private Runnable variantHold;
    private long variantEpoch;
    private ClipboardManager clipboard;
    private ClipboardStore clips;
    private final ExecutorService clipWorker = Executors.newSingleThreadExecutor();
    private final ClipboardManager.OnPrimaryClipChangedListener clipListener = () -> main.post(this::captureClipboard);
    private List<ClipboardStore.Item> clipItems = List.of();
    private ClipboardSuggestion clipSuggestion = new ClipboardSuggestion(-1);
    private KeyFeedback keyFeedback;
    private KeyPreview keyPreview;
    private List<EmojiCatalog.Category> emojiCategories;
    private int emojiCategory;
    private boolean toolsPanel, skinPanel;
    private final StringBuilder pendingEngine = new StringBuilder();
    private ChineseConversion conversion;
    private boolean engineFailed;
    private final Runnable clipMaintenance = new Runnable() { public void run() { if (!alive) return; loadClips(); main.postDelayed(this, 60000); } };
    private IosTheme theme;
    private GlassWindow glassWindow;
    private boolean glassBlur;
    private int clipLimit = 60, keyboardHeight;
    private long lastClipTimestamp;
    private long editorGeneration;
    private boolean typing, clipboardPanel, emojiPanel, inputActive;
    private String options, feedback = "";
    private JSONObject view;
    private long session;
    private Mode mode = Mode.CHINESE;
    private boolean privateField, forcedEnglish, writing, shift, caps, symbols, extraSymbols, numeric, alive;
    private int modifiers, rowHeight;
    private Runnable repeat;
    private int background, foreground, keyFill, accent, specialFill;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener settingsChanged = (preferences, name) -> main.post(() -> {
        if (!alive) return;
        if ("quick_languages".equals(name) || "skin_revision".equals(name) || "height".equals(name) || "code_height".equals(name) || "skin".equals(name) || "key_text_scale".equals(name) || "press_effects".equals(name)) build();
        else if ("key_preview".equals(name)) keyPreview.dismiss();
        else if ("clipboard_retention".equals(name)) loadClips();
        else if ("clipboard".equals(name)) { clipboardPanel = false; captureClipboard(); build(); }
        else if ("mode".equals(name) && inputActive && !forcedEnglish) {
            Mode next = Mode.valueOf(preferences.getString("mode", "CHINESE"));
            if (next != mode) changeMode(next);
        }
        else if ("learning".equals(name) || "traditional".equals(name)) {
            if (composition() && !finishComposition()) return;
            closeSession(); openSession(); flushQueued(); render();
        }
    });

    @Override public void onCreate() {
        super.onCreate(); alive = true;
        keyFeedback = new KeyFeedback(this); keyPreview = new KeyPreview(this);
        clips = new ClipboardStore(this);
        getSharedPreferences("offline", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(settingsChanged);
        lastClipTimestamp = getSharedPreferences("offline", MODE_PRIVATE).getLong("clipboard_time", -1);
        clipSuggestion = new ClipboardSuggestion(getSharedPreferences("offline", MODE_PRIVATE).getLong("clipboard_dismissed_time", -1));
        try { completion.setProfile(CodingCompletion.Profile.valueOf(getSharedPreferences("offline", MODE_PRIVATE).getString("profile", "SHELL"))); }
        catch (IllegalArgumentException ignored) { completion.setProfile(CodingCompletion.Profile.SHELL); }
        clipboard = getSystemService(ClipboardManager.class);
        if (clipboard != null) clipboard.addPrimaryClipChangedListener(clipListener);
        clipWorker.execute(() -> {
            List<EmojiCatalog.Category> loaded;
            try { loaded = EmojiCatalog.load(this); } catch (Exception error) { loaded = List.of(); }
            List<EmojiCatalog.Category> result = loaded;
            main.post(() -> { if (!alive) return; emojiCategories = result; if (emojiPanel && body != null) populateEmoji(); });
        });
        clipWorker.execute(() -> {
            try {
                var assets=getAssets();
                ChineseConversion loaded=new ChineseConversion(
                    new java.io.InputStreamReader(assets.open("opencc/CJK_Compatibility_Ideographs.txt"),java.nio.charset.StandardCharsets.UTF_8),
                    new java.io.InputStreamReader(assets.open("opencc/STPhrases.txt"),java.nio.charset.StandardCharsets.UTF_8),
                    new java.io.InputStreamReader(assets.open("opencc/STPhrases_GeneratedFromRegionalPhrases.txt"),java.nio.charset.StandardCharsets.UTF_8),
                    new java.io.InputStreamReader(assets.open("opencc/STCharacters.txt"),java.nio.charset.StandardCharsets.UTF_8));
                main.post(()->{if(alive){conversion=loaded;openSession();flushQueued();render();}});
            } catch(Exception error) {main.post(()->{if(alive && traditional()) {feedback="繁体字表准备失败，请切回简体";render();}});}
        });
        loadClips(); main.postDelayed(clipMaintenance, 60000);
        prepareEngine();
    }
    private boolean enginePreparing;
    private void prepareEngine() {
        if(enginePreparing)return;
        enginePreparing=true;
        EngineInstall.prepare(this, (ready, error) -> main.post(() -> {
            enginePreparing=false;
            if (!alive) return;
            options=ready;feedback=error;engineFailed=ready==null;
            openSession();flushQueued();render();
        }));
    }
    private void retryEngine(){
        feedback="正在准备词库，已保留输入";
        if(options==null)prepareEngine();else{openSession();flushQueued();}
        render();
    }
    @Override public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        editorGeneration++;
        closeSession(); reset();
        inputActive = true;
        privateField = info != null && FieldPolicy.privateField(info.inputType, info.imeOptions);
        forcedEnglish = info != null && FieldPolicy.directField(info.inputType);
        if (forcedEnglish) mode = Mode.ENGLISH;
        else if (info != null && "com.termux".equals(info.packageName)) mode = Mode.CODING;
        else mode = Mode.valueOf(getSharedPreferences("offline", MODE_PRIVATE).getString("mode", "CHINESE"));
        echo.reset(info == null ? -1 : info.initialSelStart, info == null ? -1 : info.initialSelEnd);
        openSession(); captureClipboard(); if (root != null) build();
    }
    private void openSession() {
        if (!inputActive || session != 0 || !engineMode() || options == null || getCurrentInputConnection() == null) return;
        if(traditional() && conversion==null) return;
        try {
            JSONObject configured = new JSONObject(options);
            configured.getJSONObject("preferences").put("learning", !privateField
                && getSharedPreferences("offline", MODE_PRIVATE).getBoolean("learning", true));
            configured.getJSONObject("preferences").put("scheme", mode == Mode.JAPANESE ? "japanese" : "quanpin");
            configured.getJSONObject("preferences").put("traditional_chinese_output", traditional());
            view = EngineInstall.value(NativeClient.create(configured.toString()));
            session = view.getLong("session");
            engineFailed = false;
            NativeClient.setPrivateSession(session, privateField);
            apply(NativeClient.focus(session, true));
        } catch (Exception | LinkageError error) { android.util.Log.e("NopeEngine", "Session creation failed", error); closeSession(); engineFailed = true; feedback = "本地引擎未就绪，可切换英文或 Coding"; }
    }
    private boolean traditional() {return mode == Mode.CHINESE && getSharedPreferences("offline",MODE_PRIVATE).getBoolean("traditional",false);}
    private String output(String text) {return traditional() && conversion!=null ? conversion.convert(text):text;}
    private void flushQueued() {
        if(inputActive && engineMode() && session!=0 && pendingEngine.length()>0) {
            String queued=pendingEngine.toString();pendingEngine.setLength(0);
            for(char c:queued.toCharArray()) {if(c=='\n') enter();else type(String.valueOf(c));}
        }
    }
    private void closeSession() {
        if (session != 0) {
            try { NativeClient.destroy(session); } catch (RuntimeException | LinkageError ignored) { }
        }
        session = 0; view = null; candidateSnapshot = null; candidateExpanded = false; japanese.reset();
    }
    private void reset() {
        dismissPopover(); stopRepeat(); modifiers = 0; shift = false; caps = false; symbols = false; extraSymbols = false;
        typing = false; clipboardPanel = false; emojiPanel = false; toolsPanel = false; skinPanel = false; numeric = false; pendingEngine.setLength(0);
        completion.clear(); feedback = ""; japanese.reset(); candidateExpanded = false; candidateSnapshot = null;
    }
    @Override public void onFinishInput() {
        editorGeneration++;
        dismissClipboardSuggestion();
        inputActive = false; reset(); closeSession(); super.onFinishInput();
    }
    @Override public void onFinishInputView(boolean finishing) {
        editorGeneration++;
        dismissClipboardSuggestion();
        dismissPopover(); stopRepeat(); modifiers = 0; shift = false; caps = false; completion.clear(); typing = false;
        clipboardPanel = false; emojiPanel = false; toolsPanel = false; skinPanel = false; pendingEngine.setLength(0);
        // 键面收起也销毁会话：刷入本地学习，避免重开后重放旧拼音。
        // 保留当前编辑器的回声记录，重开键盘时首字母仍能识别迟到回报。
        InputConnection connection = getCurrentInputConnection();
        if (connection != null && composition()) {
            try { if (connection.finishComposingText()) { echo.finish(); echo.expect(); } else echo.invalidate(); }
            catch (RuntimeException error) { echo.invalidate(); }
        }
        candidateExpanded = false; closeSession();
        super.onFinishInputView(finishing);
    }
    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        openSession(); captureClipboard(); build();
    }
    @Override public void onDestroy() {
        alive = false; main.removeCallbacks(clipMaintenance); reset(); closeSession();
        if (glassWindow != null) glassWindow.close();
        if (clipboard != null) clipboard.removePrimaryClipChangedListener(clipListener);
        getSharedPreferences("offline", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(settingsChanged);
        clipWorker.execute(() -> clips.close()); clipWorker.shutdown();
        super.onDestroy();
    }
    @Override public void onCurrentInputMethodSubtypeChanged(android.view.inputmethod.InputMethodSubtype subtype) {
        super.onCurrentInputMethodSubtypeChanged(subtype);
        if (subtype == null) return;
        Mode next = subtype.getLocale().startsWith("ja") ? Mode.JAPANESE : Mode.CHINESE;
        if (forcedEnglish) getSharedPreferences("offline", MODE_PRIVATE).edit().putString("mode", next.name()).apply();
        else if (next != mode) changeMode(next);
    }
    @Override public boolean onEvaluateFullscreenMode() { return false; }
    @Override public View onCreateInputView() {
        if (glassWindow != null) glassWindow.close();
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setMotionEventSplittingEnabled(true);
        skinStack=new SkinStack(this,root);
        root.setOnApplyWindowInsetsListener((target, insets) -> {
            root.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom()); return insets;
        });
        build();
        android.view.Window window = getWindow() == null ? null : getWindow().getWindow();
        if (window != null) {
            glassWindow = new GlassWindow(window, getSystemService(android.view.WindowManager.class), skinStack,
                enabled -> { glassBlur = enabled; applyGlass(); });
            GlassWindow controller = glassWindow;
            root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View v) { controller.start(); }
                @Override public void onViewDetachedFromWindow(View v) { controller.close(); }
            });
        }
        return skinStack;
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration); build();
    }
    private void applyGlass() {
        if (root == null || theme == null) return;
        skinStack.skin(theme,glassBlur);
    }
    private void changeMode(Mode next) {
        if (forcedEnglish && next != Mode.ENGLISH) { feedback = "当前字段使用英文直输"; render(); return; }
        if (composition() && !finishComposition()) return;
        if (pendingEngine.length() > 0) literal(pendingEngine.toString().replace("\n", ""));
        closeSession(); reset(); mode = next;
        if (!forcedEnglish) getSharedPreferences("offline", MODE_PRIVATE).edit().putString("mode", next.name()).apply();
        openSession(); build();
    }
    private void build() {
        if (root == null) return;
        dismissPopover(); stopRepeat(); root.removeAllViews();
        ctrlKey = null; altKey = null; shiftKey = null; expandedCandidates = null;
        theme = new IosTheme(this);
        background = theme.background; foreground = theme.foreground;
        keyFill = theme.key; specialFill = theme.special; accent = theme.accent;
        applyGlass();
        android.content.res.Configuration config = getResources().getConfiguration();
        keyboardHeight = KeyboardMetrics.height(getSharedPreferences("offline", MODE_PRIVATE).getInt("height", 0), config.screenWidthDp, config.screenHeightDp);
        if (mode == Mode.CODING) keyboardHeight = KeyboardMetrics.codeHeight(getSharedPreferences("offline", MODE_PRIVATE).getInt("code_height", 0), keyboardHeight, config.screenHeightDp);
        rowHeight = KeyboardMetrics.row(keyboardHeight, mode == Mode.CODING);
        top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL); top.setPadding(dp(6), 0, dp(6), 0);
        if(theme.palette.id.equals("lavender"))top.setBackground(new PixelGlassDrawable(PixelGlassDrawable.Surface.PANEL,0xffe9e6f0,false,theme.density));
        root.addView(top, new LinearLayout.LayoutParams(-1, candidateHeight()));
        body = new FrameLayout(this); body.setPadding(dp(3), 0, dp(3), 0);
        root.addView(body, new LinearLayout.LayoutParams(-1, dp(KeyboardMetrics.body(keyboardHeight))));
        View footer = new View(this);skinFooter=footer;root.addView(footer,new LinearLayout.LayoutParams(-1,dp(8)));
        keys = new LinearLayout(this); keys.setOrientation(LinearLayout.VERTICAL); body.addView(keys, new FrameLayout.LayoutParams(-1, -1));
        skinStack.regions(top,body,keys,skinFooter);
        if (toolsPanel || skinPanel) { populateTools(); render(); return; }
        if (mode == Mode.CODING) {
            LinearLayout tools = row(keys, KeyboardMetrics.controls(keyboardHeight));
            Button tab = key("Tab", "终端补全或缩进", () -> stroke(KeyEvent.KEYCODE_TAB), false); special(tab); add(tools, tab, 1);
            Button profile = key(profileName() + " ▾", "长按选择 Shell、Python 或 JS/TS", () -> { feedback = "长按 " + profileName() + " 选择片段类型"; render(); }, false);
            profile.setTextSize(12); profile.setOnLongClickListener(v -> { keyFeedback.pulse(); profile(v); return true; });
            special(profile);
            add(tools, profile, 1.2f);
            Button escape = key("Esc", "Escape", () -> stroke(KeyEvent.KEYCODE_ESCAPE), false); special(escape); add(tools, escape, 1);
            ctrlKey = key("Ctrl", "Ctrl 作用于下一个键", () -> toggle(CodingKeyMap.CTRL), (modifiers & CodingKeyMap.CTRL) != 0);
            altKey = key("Alt", "Alt 作用于下一个键", () -> toggle(CodingKeyMap.ALT), (modifiers & CodingKeyMap.ALT) != 0);
            ctrlKey.setTextSize(13); altKey.setTextSize(13);
            special(ctrlKey); special(altKey);
            add(tools, ctrlKey, 1); add(tools, altKey, 1);
            LinearLayout punctuation = row(keys, KeyboardMetrics.punctuation(keyboardHeight));
            for (char character : CodingLayout.PUNCTUATION.toCharArray()) {
                String text = String.valueOf(character); add(punctuation, characterKey(text), 1);
            }
        }
        if (numeric) {
            numericKeys();
            codeNavigation();
            if (clipboardPanel) populateClipboard(); else if (emojiPanel) populateEmoji();
            render(); return;
        }
        String[][] symbolRows = SymbolLayouts.page(extraSymbols);
        String[] rows = CodingLayout.rows(false, false);
        for (int index = 0; index < rows.length; index++) {
            LinearLayout line = row(keys);
            if(theme.palette.id.equals("color")) {
                GradientDrawable track=new GradientDrawable();track.setCornerRadius(dp(18));track.setColor(0xff080808);
                int inset=index==1?Math.round(getResources().getDisplayMetrics().widthPixels*.05f):dp(2);
                line.setBackground(new android.graphics.drawable.InsetDrawable(track,inset,dp(2),inset,dp(2)));
            }
            if (!symbols && index == 1) { spacer(line, .5f); }
            if (index == 2) {
                shiftKey = symbols ? key(extraSymbols ? "?123" : "=\\<", "另一页符号", () -> { extraSymbols = !extraSymbols; build(); }, false)
                    : icon(KeyboardIcon.Kind.SHIFT, "Shift；长按锁定大写", () -> { caps = false; shift = !shift; build(); }, shift || caps);
                special(shiftKey);
                if (!symbols) shiftKey.setOnLongClickListener(v -> { caps = !caps; shift = false; build(); return true; });
                add(line, shiftKey, 1.5f);
            }
            if (symbols) for (String symbol : symbolRows[index]) {
                String text = symbol.equals("、") && !engineMode() ? "\\" : symbol;
                add(line, characterKey(text), 1);
            } else for (char letter : rows[index].toCharArray()) {
                String text = String.valueOf((shift || caps) ? Character.toUpperCase(letter) : letter);
                add(line, characterKey(text), 1);
            }
            if (!symbols && index == 1) { spacer(line, .5f); }
            if (index == 2) {
                add(line, deleteKey(), 1.5f);
            }
        }
        LinearLayout bottom = row(keys);
        Runnable switchSymbols = () -> { symbols = !symbols; extraSymbols = false; numeric = false; build(); };
        Button number = symbols ? icon(KeyboardIcon.Kind.BACK, "返回字母键盘", switchSymbols, false)
            : key("123", "切换到符号", switchSymbols, false);
        if (!symbols) number.setTextSize(16);
        special(number); add(bottom, number, 1.5f);
        Button left = characterKey(engineMode() && !symbols ? "。" : ".");
        special(left); add(bottom, left, 1);
        if (symbols) { Button pad = icon(KeyboardIcon.Kind.NUMBERS, "打开独立数字面板", () -> { numeric = true; build(); }, false); special(pad); add(bottom, pad, 1); }
        spaceKey = key("", mode == Mode.JAPANESE ? "空格转换并切换候选；长按切换键盘" : "空格；长按切换键盘", this::space, false);
        spaceKey.setTextSize(17); spaceKey.setOnLongClickListener(v -> { keyFeedback.pulse(); keyboardPicker(v); return true; });
        add(bottom, spaceKey, symbols ? 3.5f : 4.5f);
        Button right = languageKey();
        special(right); add(bottom, right, 1);
        Button enter = icon(KeyboardIcon.Kind.ENTER, "回车或完成输入", this::enter, false); special(enter); add(bottom, enter, 1.5f);
        codeNavigation();
        if (clipboardPanel) populateClipboard(); else if (emojiPanel) populateEmoji();
        render();
    }
    private void spacer(LinearLayout line, float weight) { line.addView(new View(this), new LinearLayout.LayoutParams(0, 1, weight)); }
    private Button languageKey() {
        String name = mode == Mode.JAPANESE ? "日文" : mode == Mode.CHINESE ? "中文" : mode == Mode.CODING ? "Code" : "英文";
        String label = mode == Mode.JAPANESE ? "日" : mode == Mode.CHINESE ? "中" : mode == Mode.CODING ? "Code" : "EN";
        String quick = getSharedPreferences("offline", MODE_PRIVATE).getString("quick_languages", QuickLanguages.DEFAULT);
        Button button = key(label, "当前" + name + "；点按切换到" + QuickLanguages.name(QuickLanguages.next(mode.name(), quick)) + "；可在设置调整快速切换语言",
            () -> changeMode(Mode.valueOf(QuickLanguages.next(mode.name(), getSharedPreferences("offline", MODE_PRIVATE).getString("quick_languages", QuickLanguages.DEFAULT)))), false);
        ((KeyButton)button).skinRole("mode");button.setTextSize(mode == Mode.CODING ? 12 : 17); button.setTypeface(theme.palette.id.equals("retro")?RetroTypeface.get(this):Typeface.DEFAULT, Typeface.BOLD); button.setSelected(true); return button;
    }

    private void codeNavigation() {
        if (mode != Mode.CODING) return;
        LinearLayout navigation = row(keys, KeyboardMetrics.navigation(keyboardHeight));
        Button home = key("Home", "行首", () -> stroke(KeyEvent.KEYCODE_MOVE_HOME), false); special(home); add(navigation, home, 1.2f);
        control(navigation, "←", "左方向键", KeyEvent.KEYCODE_DPAD_LEFT);
        control(navigation, "↓", "下一条历史", KeyEvent.KEYCODE_DPAD_DOWN);
        control(navigation, "↑", "上一条历史", KeyEvent.KEYCODE_DPAD_UP);
        control(navigation, "→", "右方向键", KeyEvent.KEYCODE_DPAD_RIGHT);
        Button end = key("End", "行尾", () -> stroke(KeyEvent.KEYCODE_MOVE_END), false); special(end); add(navigation, end, 1.2f);
    }
    private Button deleteKey() {
        Button button = icon(KeyboardIcon.Kind.DELETE, "删除；上滑抬手清空", this::backspace, false);
        special(button); deleteGesture(button);
        button.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(R.id.clear_input,
                    mode == Mode.CODING ? "清除当前命令行" : "清空当前输入框"));
            }
            @Override public boolean performAccessibilityAction(View host, int action, android.os.Bundle arguments) {
                if (action == R.id.clear_input) { clearInput(); return true; }
                return super.performAccessibilityAction(host, action, arguments);
            }
        });
        return button;
    }
    private void numericKeys() {
        LinearLayout grid = new LinearLayout(this); grid.setOrientation(LinearLayout.HORIZONTAL);
        keys.addView(grid, new LinearLayout.LayoutParams(-1, dp(rowHeight * 3)));
        LinearLayout operators = new app.msime.android.KeyboardKeyArea(this, child -> child instanceof Button); operators.setOrientation(LinearLayout.VERTICAL);
        grid.addView(operators, new LinearLayout.LayoutParams(0, -1, 1.4f));
        Button[] left = {key("Tab", "制表符或终端补全", () -> stroke(KeyEvent.KEYCODE_TAB), false), characterKey("+"), characterKey("-"), characterKey("*")};
        for (Button button : left) {
            special(button);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 0, 1); p.setMargins(dp(2), dp(2), dp(2), dp(2)); operators.addView(button, p);
        }
        LinearLayout digits = new LinearLayout(this); digits.setOrientation(LinearLayout.VERTICAL);
        grid.addView(digits, new LinearLayout.LayoutParams(0, -1, 7.2f));
        for (int r = 0; r < 3; r++) {
            LinearLayout line = row(digits);
            for (int c = 1; c <= 3; c++) { Button digit = characterKey(String.valueOf(r * 3 + c)); digit.setTextSize(30); add(line, digit, 1); }
        }
        LinearLayout utilities = new app.msime.android.KeyboardKeyArea(this, child -> child instanceof Button); utilities.setOrientation(LinearLayout.VERTICAL);
        grid.addView(utilities, new LinearLayout.LayoutParams(0, -1, 1.4f));
        Button[] right = {characterKey("%"), characterKey("/"), deleteKey()};
        for (Button button : right) {
            special(button); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 0, 1); p.setMargins(dp(2), dp(2), dp(2), dp(2)); utilities.addView(button, p);
        }
        LinearLayout bottom = row(keys);
        Button back = icon(KeyboardIcon.Kind.BACK, "返回字母键盘", () -> { numeric = false; symbols = false; build(); }, false); special(back); add(bottom, back, 1.4f);
        add(bottom, characterKey("."), 1);
        Button symbol = key("!?#", "返回符号键盘", () -> { numeric = false; symbols = true; extraSymbols = false; build(); }, false); special(symbol); add(bottom, symbol, 1.2f);
        Button zero = characterKey("0"); zero.setTextSize(30); add(bottom, zero, 2.4f);
        add(bottom, characterKey("="), 1.4f); Button language = languageKey(); special(language); add(bottom, language, 1.2f);
        Button enter = icon(KeyboardIcon.Kind.ENTER, "回车或完成输入", this::enter, false); special(enter); add(bottom, enter, 1.4f);
    }
    private Button characterKey(String text) {
        String hint = mode == Mode.JAPANESE && text.equals("。") ? "、" : SymbolLayouts.hint(text);
        Button button = hint.isEmpty() ? new KeyButton(this) : new HintKey(this, hint);
        String[] variants = mode == Mode.JAPANESE && text.equals("。") ? new String[]{"、", "。"} : SymbolLayouts.variants(text);
        String editing = mode != Mode.CODING && !symbols && !numeric ? EditingSwipe.action(text) : "";
        configure(button, text, "输入 " + text + (variants.length > 0 ? "；上滑或长按选择变体" : "")
            + (editing.isEmpty() ? "" : "；下滑" + editing), () -> type(text), false);
        if(button instanceof HintKey hinted)hinted.retroHint(theme.palette.id.equals("retro"));
        variantGesture(button, text, variants);
        KeyButton input=(KeyButton)button;
        input.preview(() -> {
            if(KeyPreviewState.eligible(text,privateField,getSharedPreferences("offline",MODE_PRIVATE).getBoolean("key_preview",true)))
                keyPreview.show(input,root,button.getText().toString(),theme);
        },()->keyPreview.release(input,false),()->keyPreview.release(input,true));
        if (!editing.isEmpty()) button.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(R.id.edit_shortcut, editing));
            }
            @Override public boolean performAccessibilityAction(View host, int action, android.os.Bundle arguments) {
                if (action == R.id.edit_shortcut) { editShortcut(text); return true; }
                return super.performAccessibilityAction(host, action, arguments);
            }
        });
        return button;
    }
    private void variantGesture(Button button, String text, String[] values) {
        String editing = mode != Mode.CODING && !symbols && !numeric ? EditingSwipe.action(text) : "";
        final PopupWindow[] editHint = {null};
        final float[] start = {0, 0}; final long[] generation = {0}, epoch = {0};
        final boolean[] active = {false}, moved = {false}, swiped = {false};
        final VariantPanel[] panel = {null}; final int[] origin = new int[2], initial = {0};
        final Runnable[] hold = {null};
        java.util.function.Consumer<Boolean> open = swipe -> {
            if (values.length == 0 || !active[0] || generation[0] != editorGeneration || epoch[0] != variantEpoch || !button.isAttachedToWindow() || modifiers != 0) return;
            dismissPopover(); epoch[0] = variantEpoch; swiped[0] = swipe;
            int selected = SymbolLayouts.initial(text, values, swipe);
            initial[0] = selected;
            int[] keyboard = new int[2], anchor = new int[2]; root.getLocationOnScreen(keyboard); button.getLocationOnScreen(anchor);
            int columns = values.length > 5 ? 3 : values.length;
            int available = root.getWidth() > 0 ? root.getWidth() : getResources().getDisplayMetrics().widthPixels;
            int cell = Math.min(dp(48), Math.max(dp(30), (available - dp(24)) / columns));
            panel[0] = new VariantPanel(this, values, selected, cell, columns, keyFill, theme.keyInk, accent, theme.selectedInk);
            int left = VariantSelection.left(anchor[0] + button.getWidth() / 2, selected, columns, cell, panel[0].padding,
                keyboard[0] + dp(4), keyboard[0] + available - dp(4));
            origin[0] = left; origin[1] = Math.max(0, anchor[1] - panel[0].height() - dp(6));
            popover = new PopupWindow(panel[0], panel[0].width(), panel[0].height(), false);
            popover.setTouchable(false); popover.setClippingEnabled(true); popover.setAnimationStyle(0);
            popover.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
            popover.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)); popover.setElevation(dp(10));
            popover.showAsDropDown(button, left - anchor[0], -panel[0].height() - button.getHeight() - dp(6));
            keyFeedback.pulse();
        };
        java.util.function.Consumer<MotionEvent> select = event -> {
            VariantPanel p = panel[0]; if (p == null) return;
            p.getLocationOnScreen(origin);
            float dy = event.getRawY() - start[1];
            float dx = event.getRawX() - start[0];
            float x = event.getRawX() - origin[0];
            float y = event.getRawY() - origin[1];
            if (!swiped[0] && Math.abs(dx) < dp(9) && Math.abs(dy) < dp(9)) { p.selected = initial[0]; p.invalidate(); return; }
            if (event.getRawY() > origin[1] + p.height() && dy <= button.getHeight() + dp(24))
                y = p.padding + (initial[0] / p.columns + .5f) * p.cell;
            p.selected = swiped[0] && dy > -dp(12) ? -1 : VariantSelection.hit(x, y,
                p.columns, values.length, p.cell, p.padding, dp(24));
            p.invalidate();
        };
        if (values.length > 0) button.setOnLongClickListener(v -> { choices(button, "选择符号", values, SymbolLayouts.initial(text, values, false), i -> type(values[i], true)); return true; });
        button.setOnTouchListener((target, event) -> {
            if (event.getPointerCount() > 1 || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                active[0] = false; button.setPressed(false); if (hold[0] != null) main.removeCallbacks(hold[0]);
                if (editHint[0] != null) { editHint[0].dismiss(); editHint[0] = null; }
                if (panel[0] != null && epoch[0] == variantEpoch) dismissPopover(); panel[0] = null; return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (popover != null) dismissPopover();
                    if (variantHold != null) main.removeCallbacks(variantHold);
                    start[0] = event.getRawX(); start[1] = event.getRawY(); generation[0] = editorGeneration; epoch[0] = variantEpoch;
                    active[0] = true; moved[0] = false; swiped[0] = false; panel[0] = null; button.setPressed(true);
                    hold[0] = () -> open.accept(false); variantHold = hold[0]; main.postDelayed(hold[0], 360);
                    button.getParent().requestDisallowInterceptTouchEvent(true); return true;
                case MotionEvent.ACTION_MOVE:
                    if (!active[0] || generation[0] != editorGeneration || epoch[0] != variantEpoch) return true;
                    float dx = event.getRawX() - start[0], dy = event.getRawY() - start[1];
                    if (Math.abs(dx) > dp(9) || Math.abs(dy) > dp(9)) { moved[0] = true; main.removeCallbacks(hold[0]); }
                    if (panel[0] == null && dy <= -dp(24) && Math.abs(dx) < dp(40)) open.accept(true);
                    else if (panel[0] != null) select.accept(event);
                    boolean editReady = panel[0] == null && !editing.isEmpty() && EditingSwipe.ready(dx, dy, dp(36));
                    if (editReady && editHint[0] == null) {
                        TextView hint = label("松手" + editing, 17); hint.setGravity(android.view.Gravity.CENTER); hint.setBackground(surface(specialFill, 18));
                        editHint[0] = new PopupWindow(hint, dp(120), dp(46), false);
                        editHint[0].setAnimationStyle(0); editHint[0].setTouchable(false); editHint[0].setBackgroundDrawable(surface(specialFill,18));
                        editHint[0].showAsDropDown(button, 0, -button.getHeight() - dp(50)); gestureHint = editHint[0];
                    } else if (!editReady && editHint[0] != null) { editHint[0].dismiss(); editHint[0] = null; }
                    return true;
                case MotionEvent.ACTION_UP:
                    main.removeCallbacks(hold[0]); button.setPressed(false);
                    if (editHint[0] != null) { editHint[0].dismiss(); editHint[0] = null; }
                    if (!active[0] || generation[0] != editorGeneration || (panel[0] != null && epoch[0] != variantEpoch) || !button.isAttachedToWindow()) { active[0] = false; return true; }
                    active[0] = false;
                    if (panel[0] != null) {
                        select.accept(event);
                        int selected = panel[0].selected; dismissPopover(); panel[0] = null;
                        if (selected >= 0) type(values[selected], true);
                    } else if (!editing.isEmpty() && EditingSwipe.ready(event.getRawX() - start[0], event.getRawY() - start[1], dp(36))) {
                        keyFeedback.pulse(); editShortcut(text);
                    } else if (InputTouchPolicy.tap(event.getRawX()-start[0], event.getRawY()-start[1], event.getX(), event.getY(), button.getWidth(), button.getHeight(), dp(18))) button.performClick();
                    return true;
                default: return true;
            }
        });
    }
    private LinearLayout row(LinearLayout parent, int height) {
        LinearLayout line = new app.msime.android.KeyboardKeyArea(this, child -> child instanceof Button);
        line.setOrientation(LinearLayout.HORIZONTAL); parent.addView(line, new LinearLayout.LayoutParams(-1, dp(height))); return line;
    }
    private Button icon(KeyboardIcon.Kind kind, String label, Runnable action, boolean selected) {
        Button button = new KeyboardIcon(this, kind); configure(button, KeyboardIcon.caption(kind), label, action, selected);
        button.setTextSize(KeyboardIcon.toolbar(kind) ? 10 : 13); return button;
    }
    private void configure(Button button, String text, String label, Runnable action, boolean selected) {
        button.setAllCaps(false); button.setText(theme != null && !theme.palette.id.equals("ios") && text.length()==1 && Character.isLetter(text.charAt(0)) ? text.toUpperCase(java.util.Locale.ROOT) : text);
        button.setTextSize(text.length() > 2 ? 13 : theme!=null && theme.palette.id.equals("cloud") ? 24 : theme!=null&&theme.palette.id.equals("retro")?30:26); button.setSingleLine(true);
        button.setMinWidth(0); button.setMinimumWidth(0); button.setMinHeight(0); button.setMinimumHeight(0);
        button.setPadding(dp(2), 0, dp(2), 0); button.setContentDescription(label); button.setTypeface(Typeface.DEFAULT);
        button.setGravity(android.view.Gravity.CENTER); button.setIncludeFontPadding(false);
        button.setLetterSpacing(0);
        if(theme.palette.id.equals("retro")||theme.palette.id.equals("lavender"))button.setBackgroundTintList(null);
        if (button instanceof KeyboardIcon icon) icon.retroBrand(theme.palette.id.equals("retro"));
        button.setHapticFeedbackEnabled(false);
        if (button instanceof KeyButton input) {
            input.skinRole(button instanceof KeyboardIcon icon?KeyboardIcon.toolbar(icon.kind)?"toolbar":"function":text.isEmpty()?"space":text.length()==1&&Character.isDigit(text.charAt(0))?"number":text.length()==1&&Character.isLetter(text.charAt(0))?"letter":"other");
            input.feedback(() -> keyFeedback.pulse());
            input.pressFeedback(getSharedPreferences("offline",MODE_PRIVATE).getBoolean("press_effects",true),theme.palette.id.equals("retro")||theme.palette.id.equals("lavender"));
            input.pixelLetters(theme.palette.id.equals("retro")||theme.palette.id.equals("lavender"));
            input.textScale(KeyboardText.scale(getSharedPreferences("offline", MODE_PRIVATE).getInt("key_text_scale", KeyboardText.DEFAULT_PERCENT)));
        }
        button.setStateListAnimator(null); button.setElevation(0); button.setTranslationZ(0);
        face(button, selected); button.setOnClickListener(v -> action.run());
    }
    private void special(Button button) {
        if (button instanceof KeyButton key) key.function(true);
        face(button, button.isSelected());
    }
    private android.graphics.drawable.Drawable surface(int color, int radius) {
        android.graphics.drawable.Drawable fill = keycap(color, radius);
        android.graphics.drawable.Drawable pressed = keycap(IosTheme.mix(color, foreground, theme.dark ? .14f : .09f), radius);
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed); states.addState(new int[0], fill); return states;
    }
    private android.graphics.drawable.Drawable keycap(int color, int radius) {
        if(theme.palette.id.equals("lavender"))return new PixelGlassDrawable(PixelGlassDrawable.Surface.KEY,color,false,theme.density);
        if(theme.palette.id.equals("retro"))return new RetroDrawable(theme.palette,RetroDrawable.Surface.KEY,false,color,theme.density);
        GradientDrawable shadow = new GradientDrawable(); shadow.setCornerRadius(dp(radius));
        shadow.setColor(IosTheme.mix(color, theme.shadow, .65f));
        GradientDrawable cap = new GradientDrawable(); cap.setCornerRadius(dp(radius)); cap.setColor(color);
        android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{shadow, cap});
        layers.setLayerInset(1, 0, 0, 0, dp(1)); return layers;
    }
    private android.graphics.drawable.Drawable chrome(int color, int radius) {
        GradientDrawable normal = new GradientDrawable(); normal.setCornerRadius(dp(radius)); normal.setColor(color);
        GradientDrawable pressed = new GradientDrawable(); pressed.setCornerRadius(dp(radius)); pressed.setColor(IosTheme.alpha(foreground, 24));
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed); states.addState(new int[0], normal); return states;
    }
    private void toolbar() {
        top.removeAllViews();
        if (clipboardPanel || emojiPanel || toolsPanel || skinPanel) {
            add(top, tool(KeyboardIcon.Kind.BACK, "返回键盘", () -> { if (clipboardPanel) dismissClipboardSuggestion(); clipboardPanel = false; emojiPanel = false; toolsPanel = false; skinPanel = false; build(); }), 1);
            TextView title = label(clipboardPanel ? "剪贴板" : emojiPanel ? "表情" : skinPanel ? "皮肤" : "工具", 17); top.addView(title, new LinearLayout.LayoutParams(0, -1, 4));
            add(top, tool(KeyboardIcon.Kind.SETTINGS, "输入法设置", this::settings), 1);
            return;
        }
        if (mode == Mode.CODING) { top.setVisibility(View.GONE); return; }
        if(theme.palette.id.equals("retro")){
            top.addView(tool(KeyboardIcon.Kind.BRAND,"NOPE 工具",this::tools),new LinearLayout.LayoutParams(dp(44),-1));
            LinearLayout lcd=new LinearLayout(this);lcd.setGravity(android.view.Gravity.CENTER_VERTICAL);
            lcd.setPadding(dp(4),0,dp(4),0);
            lcd.setBackground(new RetroDrawable(theme.palette,RetroDrawable.Surface.DISPLAY,false,theme.candidateFill,theme.density));
            add(lcd,lcdTool(KeyboardIcon.Kind.CLIPBOARD,"剪贴板历史",this::showClipboard),1);
            add(lcd,lcdTool(KeyboardIcon.Kind.EMOJI,"表情",this::showEmoji),1);
            add(lcd,lcdTool(KeyboardIcon.Kind.SETTINGS,"输入法设置与高度",this::settings),1);
            top.addView(lcd,new LinearLayout.LayoutParams(0,-1,1));
            top.addView(tool(KeyboardIcon.Kind.DOWN,"收起键盘",()->requestHideSelf(0)),new LinearLayout.LayoutParams(dp(48),-1));return;
        }
        Button brand = tool(KeyboardIcon.Kind.BRAND, "NOPE 工具", this::tools);
        ((KeyboardIcon)brand).glyphColor(accent); add(top, brand, 1);
        add(top, tool(KeyboardIcon.Kind.CLIPBOARD, "剪贴板历史", this::showClipboard), 1);
        add(top, tool(KeyboardIcon.Kind.EMOJI, "表情", this::showEmoji), 1);
        add(top, tool(KeyboardIcon.Kind.SETTINGS, "输入法设置与高度", this::settings), 1);
        add(top, tool(KeyboardIcon.Kind.DOWN, "收起键盘", () -> requestHideSelf(0)), 1);
    }
    private Button lcdTool(KeyboardIcon.Kind kind,String description,Runnable action){
        KeyboardIcon button=(KeyboardIcon)tool(kind,description,action);
        button.glyphColor(theme.candidateInk);button.setBackground(chrome(Color.TRANSPARENT,3));
        return button;
    }
    private Button tool(KeyboardIcon.Kind kind, String label, Runnable action) {
        Button button = icon(kind, label, action, false); button.setTextColor(foreground);
        ((KeyboardIcon)button).glyphColor(foreground);
        button.setBackground(theme.palette.id.equals("retro")?surface(theme.key,3):chrome(Color.TRANSPARENT,6));return button;
    }
    private TextView label(String text, int size) {
        TextView value = new TextView(this); value.setText(text); value.setTextColor(foreground); value.setTextSize(size);
        value.setGravity(android.view.Gravity.CENTER_VERTICAL); value.setPadding(dp(12), 0, dp(12), 0); return value;
    }
    private void settings() {
        startActivity(new Intent(this, SetupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private void tools() {
        if (composition() && !finishComposition()) return;
        toolsPanel=true; skinPanel=false; clipboardPanel=false; emojiPanel=false; typing=false; build();
    }
    private void populateTools() {
        body.removeAllViews(); ScrollView scroll=new ScrollView(this);
        LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(6),dp(6),dp(6),dp(6)); scroll.addView(content); body.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        if (skinPanel) {
            int selected=SkinPalette.index(getSharedPreferences("offline",MODE_PRIVATE).getString("skin","system"));
            for (int i=0;i<SkinPalette.IDS.length;i++) {
                final int index=i; Button option=key(SkinPalette.NAMES[i]+(selected==i?"  ✓":""), "选择皮肤 "+SkinPalette.NAMES[i],
                    ()->{getSharedPreferences("offline",MODE_PRIVATE).edit().putString("skin",SkinPalette.IDS[index]).apply();},selected==i);
                option.setTextSize(17); option.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL); option.setPadding(dp(16),0,dp(16),0);
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(52)); params.setMargins(0,0,0,dp(5)); content.addView(option,params);
            }
            return;
        }
        boolean traditional=getSharedPreferences("offline",MODE_PRIVATE).getBoolean("traditional",false);
        String[] titles={"剪贴板", "表情", "皮肤", traditional?"繁体 → 简体":"简体 → 繁体", "全选", "复制", "剪切", "粘贴", "设置"};
        Runnable[] actions={this::showClipboard,this::showEmoji,()->{skinPanel=true;toolsPanel=false;build();},
            ()->{getSharedPreferences("offline",MODE_PRIVATE).edit().putBoolean("traditional",!traditional).apply();populateTools();},
            ()->editShortcut("a"),()->editShortcut("c"),()->editShortcut("x"),()->editShortcut("v"),this::settings};
        for (int i=0;i<titles.length;i+=3) {
            LinearLayout line=new LinearLayout(this); content.addView(line,new LinearLayout.LayoutParams(-1,dp(64)));
            for(int j=i;j<Math.min(i+3,titles.length);j++) { Button b=key(titles[j],titles[j],actions[j],false); b.setTextSize(15); add(line,b,1); }
        }
    }
    private void keyboardPicker(View anchor) {
        choices(anchor, "选择键盘", new String[]{"拼音", "English", "Code", "日本語（ローマ字）"}, mode.ordinal(), index -> changeMode(Mode.values()[index]));
    }
    private void profile(View anchor) {
        choices(anchor, "代码建议", new String[]{"Shell · 终端命令", "Python", "JavaScript / TypeScript"}, completion.profile().ordinal(), index -> {
            completion.setProfile(CodingCompletion.Profile.values()[index]); modifiers = 0; typing = false;
            getSharedPreferences("offline", MODE_PRIVATE).edit().putString("profile", completion.profile().name()).apply(); build();
        });
    }
    private void choices(View anchor, String title, String[] values, int selected, java.util.function.IntConsumer action) {
        if (anchor == null || !anchor.isAttachedToWindow()) return;
        dismissPopover(); stopRepeat();
        LinearLayout menu = new LinearLayout(this); menu.setOrientation(LinearLayout.VERTICAL); menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        menu.setBackground(surface(keyFill, 12));
        TextView heading = label(title, 12); heading.setTextColor(theme.keyInk); menu.addView(heading, new LinearLayout.LayoutParams(-1, dp(32)));
        for (int i = 0; i < values.length; i++) {
            final int index = i;
            Button button = key(values[i] + (i == selected ? "  ✓" : ""), values[i], () -> { dismissPopover(); action.accept(index); }, i == selected);
            button.setTextSize(15); button.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL); button.setPadding(dp(16), 0, dp(16), 0);
            menu.addView(button, new LinearLayout.LayoutParams(-1, dp(46)));
        }
        int width = Math.min(dp(280), getResources().getDisplayMetrics().widthPixels - dp(24));
        int height = dp(48 + values.length * 46);
        popover = new PopupWindow(menu, width, height, true); popover.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popover.setBackgroundDrawable(surface(keyFill, 12)); popover.setElevation(dp(10)); popover.setOutsideTouchable(true); popover.setAnimationStyle(0);
        // 与触发键保持空间联系，从键的上方展开；不阻塞输入动画。
        popover.showAsDropDown(anchor, -(width - anchor.getWidth()) / 2, -height - anchor.getHeight());
    }
    private void dismissPopover() {
        if(keyPreview!=null)keyPreview.dismiss();
        variantEpoch++;
        if (variantHold != null) { main.removeCallbacks(variantHold); variantHold = null; }
        if (popover != null) { popover.dismiss(); popover = null; }
        if (gestureHint != null) { gestureHint.dismiss(); gestureHint = null; }
    }
    private void showClipboard() {
        if (privateField) { feedback = "私密输入框不显示剪贴板历史"; render(); return; }
        if (composition() && !finishComposition()) return;
        clipboardPanel = true; emojiPanel = false; toolsPanel = false; skinPanel = false; typing = false; captureClipboard(); build();
    }
    private void showEmoji() {
        if (composition() && !finishComposition()) return;
        emojiPanel = true; clipboardPanel = false; toolsPanel = false; skinPanel = false; emojiCategory=0; typing = false; build();
    }
    private void populateEmoji() {
        body.removeAllViews();
        if (emojiCategories == null || emojiCategories.isEmpty()) {
            body.addView(label(emojiCategories == null ? "正在准备表情…" : "表情暂时不可用，请重开键盘", 15)); return;
        }
        List<EmojiCatalog.Category> display=new java.util.ArrayList<>();
        java.util.ArrayList<EmojiCatalog.Item> common=new java.util.ArrayList<>();
        var usage=getSharedPreferences("emoji_usage",MODE_PRIVATE);
        for (var category:emojiCategories) for (var item:category.items()) if (usage.getLong("count:"+item.text(),0)>0) common.add(item);
        common.sort((a,b)->{ int count=Long.compare(usage.getLong("count:"+b.text(),0),usage.getLong("count:"+a.text(),0)); return count!=0?count:Long.compare(usage.getLong("last:"+b.text(),0),usage.getLong("last:"+a.text(),0)); });
        if (common.size()>80) common.subList(80,common.size()).clear();
        display.add(new EmojiCatalog.Category("常用",List.copyOf(common))); display.addAll(emojiCategories);
        emojiCategory = Math.max(0, Math.min(emojiCategory, display.size() - 1));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        body.addView(content, new FrameLayout.LayoutParams(-1, -1));
        HorizontalScrollView tabs = new HorizontalScrollView(this); tabs.setHorizontalScrollBarEnabled(false);
        LinearLayout categories = new LinearLayout(this); categories.setOrientation(LinearLayout.HORIZONTAL); tabs.addView(categories);
        content.addView(tabs, new LinearLayout.LayoutParams(-1, dp(44)));
        for (int i = 0; i < display.size(); i++) {
            final int selected = i;
            Button tab = key(display.get(i).title(), "表情分类：" + display.get(i).title(),
                () -> { emojiCategory = selected; populateEmoji(); }, i == emojiCategory);
            tab.setBackground(chrome(i == emojiCategory ? accent : Color.TRANSPARENT, 6));
            tab.setTextColor(i==emojiCategory?theme.selectedInk:foreground);
            tab.setTextSize(14); categories.addView(tab, new LinearLayout.LayoutParams(dp(60), -1));
            if (i == emojiCategory) tab.post(() -> tabs.scrollTo(Math.max(0, tab.getLeft() - dp(90)), 0));
        }
        List<EmojiCatalog.Item> items = display.get(emojiCategory).items();
        android.widget.GridView grid = new android.widget.GridView(this);
        grid.setNumColumns(android.widget.GridView.AUTO_FIT); grid.setColumnWidth(dp(48));
        grid.setStretchMode(android.widget.GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(dp(4)); grid.setVerticalSpacing(dp(4)); grid.setPadding(dp(4),dp(6),dp(4),dp(6));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        content.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        grid.setAdapter(new android.widget.BaseAdapter() {
            @Override public int getCount() { return items.size(); }
            @Override public EmojiCatalog.Item getItem(int position) { return items.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View reused, android.view.ViewGroup parent) {
                EmojiCatalog.Item item = getItem(position);
                EmojiCell cell = reused instanceof EmojiCell ? (EmojiCell)reused : new EmojiCell(OfflineImeService.this);
                cell.bind(item, () -> {
                    if (!inputActive || getCurrentInputConnection() == null) return;
                    keyFeedback.pulse(); literal(item.text()); completion.clear(); typing=false;
                    if (feedback.isEmpty() && !privateField) usage.edit()
                        .putLong("count:"+item.text(),Math.min(1000000,usage.getLong("count:"+item.text(),0)+1))
                        .putLong("last:"+item.text(),System.currentTimeMillis()).apply();
                    if (emojiCategory==0) populateEmoji(); render();
                });
                cell.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1,dp(48)));
                cell.setBackground(chrome(Color.TRANSPARENT,5)); return cell;
            }
        });
        if (items.isEmpty()) content.addView(label(emojiCategory==0 ? "使用过的表情会按频率出现在这里" : "当前系统字体尚不支持此分类的表情", 14));
    }
    private void editShortcut(String letter) {
        if (mode == Mode.CODING || !inputActive || getCurrentInputConnection() == null) return;
        String action = EditingSwipe.action(letter); if (action.isEmpty()) return;
        if (composition() && !finishComposition()) return;
        InputConnection connection = getCurrentInputConnection(); if (connection == null) return;
        if (letter.equalsIgnoreCase("v")) {
            try {
                ClipData data = clipboard == null ? null : clipboard.getPrimaryClip();
                CharSequence text = data == null || data.getItemCount() == 0 ? null : data.getItemAt(0).getText();
                if (text != null && text.length() > 0) pasteNow(text.toString());
                else { feedback = "剪贴板里没有文字"; render(); }
            } catch (RuntimeException error) { feedback = "当前无法读取剪贴板"; render(); }
            return;
        }
        int id = switch (letter.toLowerCase(java.util.Locale.ROOT)) {
            case "a" -> android.R.id.selectAll; case "x" -> android.R.id.cut;
            case "c" -> android.R.id.copy; case "v" -> android.R.id.paste; default -> android.R.id.undo;
        };
        int keyCode = switch (letter.toLowerCase(java.util.Locale.ROOT)) {
            case "a" -> KeyEvent.KEYCODE_A; case "x" -> KeyEvent.KEYCODE_X;
            case "c" -> KeyEvent.KEYCODE_C; case "v" -> KeyEvent.KEYCODE_V; default -> KeyEvent.KEYCODE_Z;
        };
        closeSession(); echo.invalidate(); completion.clear(); typing = false; feedback = "";
        writing = true;
        try {
            connection.finishComposingText();
            if (!connection.performContextMenuAction(id)) send(keyCode, CodingKeyMap.CTRL);
            else if (id == android.R.id.paste) dismissClipboardSuggestion();
        } catch (RuntimeException error) { feedback = "当前应用不支持" + action; }
        finally { writing = false; }
        openSession(); render();
    }
    private void populateClipboard() {
        body.removeAllViews();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(5), dp(4), dp(5), dp(8));
        scroll.addView(content); body.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        if (!getSharedPreferences("offline", MODE_PRIVATE).getBoolean("clipboard", true)) {
            content.addView(label("剪贴板记录已关闭，可在设置中开启", 14)); return;
        }
        if (clipItems.isEmpty()) { TextView empty = label("复制的文字会保存在这里\n长按置顶 · 仅本机保存 · 不自动过期", 15); empty.setGravity(android.view.Gravity.CENTER); content.addView(empty, new LinearLayout.LayoutParams(-1, -1)); return; }
        TextView hint = label("仅本机保存 · 长按置顶或删除", 12); content.addView(hint, new LinearLayout.LayoutParams(-1, dp(32)));
        for (int i = 0; i < clipItems.size(); i += 2) {
            LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL); content.addView(line);
            for (int j = i; j < Math.min(i + 2, clipItems.size()); j++) {
                ClipboardStore.Item item = clipItems.get(j);
                Button card = key((item.pinned ? "置顶 · " : "") + item.text, "粘贴" + (item.pinned ? "置顶条目，" : "历史条目，") + preview(item.text), () -> paste(item.text), false);
                clipboardText(card,(item.pinned ? "置顶 · " : "") + item.text,false);
                card.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL); card.setPadding(dp(12), dp(10), dp(12), dp(10));
                card.setBackground(surface(keyFill, 10));
                card.setOnLongClickListener(v -> { keyFeedback.pulse();
                    choices(v, "剪贴板条目", new String[]{item.pinned ? "取消置顶" : "置顶", "删除"}, -1,
                        which -> clipTask(() -> { if (which == 0) clips.pin(item.id, !item.pinned); else clips.delete(item.id); })); return true; });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,ClipboardText.cardHeight(getResources().getDisplayMetrics().density,getResources().getDisplayMetrics().scaledDensity),1); params.setMargins(dp(3), dp(3), dp(3), dp(3)); line.addView(card, params);
            }
            if (i + 1 == clipItems.size()) spacer(line, 1);
        }
        if (clipItems.size() == clipLimit) {
            Button more = key("加载更早的内容", "加载更早的剪贴板", () -> { clipLimit += 60; loadClips(); }, false); content.addView(more, new LinearLayout.LayoutParams(-1, dp(48)));
        }
    }
    private void clipTask(Runnable operation) {
        clipWorker.execute(() -> {
            try { operation.run(); refreshClips(); }
            catch (RuntimeException error) { main.post(() -> { if (alive) { lastClipTimestamp = -1; feedback = "剪贴板保存失败，请检查本机空间"; render(); } }); }
        });
    }
    private void loadClips() { clipTask(() -> { }); }
    private String preview(String text) { return ClipboardText.preview(text); }
    private void clipboardText(Button button,String text,boolean recent){
        KeyButton card=(KeyButton)button;
        card.fitText(false);card.textScale(1);card.pixelLetters(false);card.pressFeedback(false,theme.palette.id.equals("retro"));
        card.setSingleLine(false);card.setHorizontallyScrolling(false);card.setMaxLines(2);
        card.setEllipsize(TextUtils.TruncateAt.END);card.setText(ClipboardText.preview(text));
        card.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        card.setAutoSizeTextTypeUniformWithConfiguration(ClipboardText.MIN_SP,recent?ClipboardText.RECENT_MAX_SP:ClipboardText.CARD_MAX_SP,1,android.util.TypedValue.COMPLEX_UNIT_SP);
    }
    private void refreshClips() {
        clips.expire(System.currentTimeMillis(), getSharedPreferences("offline",MODE_PRIVATE).getLong("clipboard_retention",0));
        List<ClipboardStore.Item> items = clips.list(clipLimit);
        main.post(() -> { if (!alive) return; clipItems = items;
            clipSuggestion.expire(System.currentTimeMillis(),getSharedPreferences("offline",MODE_PRIVATE).getLong("clipboard_retention",0));
            if (clipboardPanel && !privateField && body != null) populateClipboard(); renderClipboardSuggestion(); });
    }
    private void renderClipboardSuggestion() {
        // A clipboard callback must not rebuild the candidate strip while the user is browsing it.
        if (clipboardPanel || emojiPanel || !composition() && !typing && modifiers == 0) render();
    }
    private void captureClipboard() {
        if (!alive || clipboard == null || !getSharedPreferences("offline", MODE_PRIVATE).getBoolean("clipboard", true)) return;
        try {
            // 仅默认 IME 服务读取当前纯文本，不使用 URI coercion。
            ClipDescription description = clipboard.getPrimaryClipDescription();
            long timestamp = description == null ? -1 : description.getTimestamp();
            boolean sensitive = description != null && description.getExtras() != null && description.getExtras().getBoolean("android.content.extra.IS_SENSITIVE", false);
            if (sensitive || privateField) {
                // 只记忽略的元数据，离开私密字段后也不会补收这次复制的正文。
                if (timestamp >= 0) getSharedPreferences("offline", MODE_PRIVATE).edit().putLong("ignored_clipboard_time", timestamp).apply();
                clipSuggestion.clear(); renderClipboardSuggestion(); return;
            }
            if (timestamp >= 0 && timestamp == getSharedPreferences("offline", MODE_PRIVATE).getLong("ignored_clipboard_time", -1)) {
                clipSuggestion.clear(); renderClipboardSuggestion(); return;
            }
            ClipData data = clipboard.getPrimaryClip();
            CharSequence value = data == null || data.getItemCount() == 0 ? null : data.getItemAt(0).getText();
            String text = value == null ? null : value.toString();
            boolean accepted = ClipboardPolicy.accept(text, false, privateField, true);
            long retention=getSharedPreferences("offline",MODE_PRIVATE).getLong("clipboard_retention",0);
            if(timestamp>=0 && ClipboardRetention.expired(timestamp,System.currentTimeMillis(),retention,false)) accepted=false;
            if (accepted) clipSuggestion.offer(timestamp, text); else clipSuggestion.clear();
            if (accepted && timestamp != lastClipTimestamp) {
                lastClipTimestamp = timestamp;
                clipTask(() -> { clips.add(text,timestamp); getSharedPreferences("offline", MODE_PRIVATE).edit().putLong("clipboard_time", timestamp).apply(); });
            }
            else renderClipboardSuggestion();
        } catch (RuntimeException error) { clipSuggestion.clear(); renderClipboardSuggestion(); }
    }
    private void dismissClipboardSuggestion() {
        long dismissed = clipSuggestion.dismiss();
        if (dismissed >= 0) getSharedPreferences("offline", MODE_PRIVATE).edit().putLong("clipboard_dismissed_time", dismissed).apply();
    }
    private void paste(String text) {
        if (!inputActive || getCurrentInputConnection() == null) return;
        if (mode == Mode.CODING && (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0)) {
            choices(top, "终端多行粘贴可能执行命令", new String[]{"确认粘贴多行", "取消"}, -1, which -> { if (which == 0) pasteNow(text); });
        } else pasteNow(text);
    }
    private void pasteNow(String text) {
        pasteNow(text, true);
    }
    private void pasteNow(String text, boolean fromClipboard) {
        if (composition() && !finishComposition()) return;
        InputConnection connection = getCurrentInputConnection(); if (connection == null) return;
        writing = true;
        try {
            if (!connection.commitText(text, 1)) { feedback = "当前应用未接受粘贴"; render(); return; }
            echo.commit(text.length()); echo.expect(); completion.clear(); typing = false;
            feedback = ""; clipboardPanel = false; emojiPanel = false;
            if (fromClipboard) dismissClipboardSuggestion();
        } catch (RuntimeException error) { feedback = "当前输入连接已失效"; echo.invalidate(); }
        finally { writing = false; }
        build();
    }
    private void clearInput() {
        dismissPopover(); stopRepeat(); completion.clear(); typing = false; feedback = "";
        boolean shifted = shift; shift = false; modifiers = 0;
        if (mode == Mode.CODING) { send(CodingKeyMap.character('u', CodingKeyMap.CTRL).code, CodingKeyMap.CTRL); if (shifted) build(); else render(); return; }
        closeSession(); echo.invalidate(); InputConnection connection = getCurrentInputConnection(); if (connection == null) return;
        writing = true; boolean batched = false;
        try {
            batched = connection.beginBatchEdit(); connection.finishComposingText();
            if (!connection.performContextMenuAction(android.R.id.selectAll) || !connection.commitText("", 1)) feedback = "当前应用不支持清空";
        } catch (RuntimeException error) { feedback = "当前输入连接已失效"; }
        finally { try { if (batched) connection.endBatchEdit(); } finally { writing = false; } }
        openSession(); if (shifted) build(); else render();
    }
    private void deleteGesture(Button button) {
        DeleteSwipe gesture = new DeleteSwipe(); final boolean[] repeated = {false}, moved = {false};
        final long[] targetEditor = {0};
        final PopupWindow[] hint = {null}; final TextView[] message = {null};
        button.setOnTouchListener((target, event) -> {
            if (event.getPointerCount() > 1 || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                gesture.cancel(); stopRepeat(); button.setPressed(false); if (hint[0] != null) { hint[0].dismiss(); hint[0] = null; } return true;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    targetEditor[0] = editorGeneration;
                    stopRepeat(); gesture.start(event.getRawX(), event.getRawY()); repeated[0] = false; moved[0] = false; button.setPressed(true);
                    repeat = new Runnable() { public void run() { if (repeat != this || moved[0] || targetEditor[0] != editorGeneration || !button.isAttachedToWindow()) return; repeated[0] = true; backspace(); main.postDelayed(this, 70); } };
                    main.postDelayed(repeat, 420); return true;
                case MotionEvent.ACTION_MOVE:
                    if (gesture.moved(event.getRawX(), event.getRawY(), dp(9))) { moved[0] = true; stopRepeat(); }
                    float progress = gesture.progress(event.getRawX(), event.getRawY(), dp(52));
                    if (progress > 0 && !repeated[0]) {
                        if (hint[0] == null) {
                            message[0] = label("上滑清空", 18); message[0].setBackground(surface(specialFill, 18));
                            hint[0] = new PopupWindow(message[0], dp(180), dp(54), false); hint[0].setElevation(dp(10));
                            hint[0].setAnimationStyle(0); hint[0].setTouchable(false);
                            gestureHint = hint[0];
                            hint[0].setBackgroundDrawable(surface(specialFill, 18)); hint[0].showAsDropDown(button, -dp(140), -button.getHeight() - dp(66));
                        }
                        message[0].setText(progress >= 1 ? "松手清空" : "上滑清空"); message[0].setAlpha(.65f + .35f * progress);
                    } else if (hint[0] != null) { hint[0].dismiss(); hint[0] = null; }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!gesture.active() || targetEditor[0] != editorGeneration) {
                        stopRepeat(); gesture.cancel(); button.setPressed(false); if (hint[0] != null) { hint[0].dismiss(); hint[0] = null; } return true;
                    }
                    boolean clear = !repeated[0] && gesture.progress(event.getRawX(), event.getRawY(), dp(52)) >= 1;
                    stopRepeat(); gesture.cancel(); button.setPressed(false); if (hint[0] != null) { hint[0].dismiss(); hint[0] = null; }
                    if (clear) { keyFeedback.pulse(); clearInput(); }
                    else if (!repeated[0] && !moved[0]) button.performClick();
                    return true;
                default: return true;
            }
        });
    }
    private LinearLayout row(LinearLayout parent) {
        LinearLayout line = new app.msime.android.KeyboardKeyArea(this, child -> child instanceof Button);
        line.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(rowHeight))); return line;
    }
    private Button key(String text, String description, Runnable action, boolean selected) {
        int direction = text.equals("←") ? 0 : text.equals("↑") ? 1 : text.equals("→") ? 2 : text.equals("↓") ? 3 : -1;
        Button button = direction < 0 ? new KeyButton(this) : new ArrowButton(this, direction);
        configure(button, text, description, action, selected); return button;
    }
    private void face(Button button, boolean selected) {
        if (button == null) return;
        boolean isShift = button instanceof KeyboardIcon icon && icon.kind == KeyboardIcon.Kind.SHIFT;
        boolean function = button instanceof KeyButton key && key.function();
        int fill = theme.palette.faceFill(selected,isShift,function);
        boolean retroMode=theme.palette.id.equals("retro")&&selected&&java.util.Set.of("中","日","EN","Code").contains(button.getText().toString());
        if(retroMode)fill=theme.special;
        String role=button instanceof KeyButton key?key.skinRole():"other";
        if(!selected)fill=theme.config.keyColor(role,"fill",fill);
        if(button instanceof KeyButton key){
            key.skinTextScale(role.equals("candidate")?1:theme.config.keyScale(role));
            String font=theme.config.keyFont(role);
            if(!font.isEmpty())button.setTypeface(font.equals("pixel")?RetroTypeface.get(this):Typeface.DEFAULT,button.getTypeface()==null?Typeface.NORMAL:button.getTypeface().getStyle());
        }
        button.setTextColor(!selected?theme.config.keyColor(role,"ink",theme.palette.faceInk(false,isShift,function)):retroMode?theme.foreground:theme.palette.faceInk(true,isShift,function));button.setSelected(selected);
        if (button instanceof KeyboardIcon icon) { icon.glyphColor(button.getCurrentTextColor()); icon.glyphSelected(selected); }
        if (android.os.Build.VERSION.SDK_INT >= 30) button.setStateDescription(selected ? "已开启" : null);
        boolean sheen = theme.palette.id.equals("system") && "QWERTqwert".contains(button.getText()) && button.getText().length()==1;
        android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},new SkinKeyDrawable(theme,fill,true,sheen,getResources().getDisplayMetrics().density,button.getText().toString(),role));
        states.addState(new int[0],new SkinKeyDrawable(theme,fill,false,sheen,getResources().getDisplayMetrics().density,button.getText().toString(),role));
        button.setBackground(states);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void add(LinearLayout row, Button button, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, weight);
        params.setMargins(dp(2), dp(2), dp(2), dp(2)); row.addView(button, params);
    }
    private void control(LinearLayout row, String label, String description, int code) {
        Button button = key(label, description, () -> stroke(code), false);
        special(button); button.setTextSize(23); repeat(button, () -> stroke(code)); add(row, button, 1);
    }
    private void repeat(Button button, Runnable action) {
        button.setOnTouchListener((target, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    stopRepeat(); button.setPressed(true); button.performClick();
                    repeat = new Runnable() {
                        @Override public void run() {
                            if (repeat != this || !button.isPressed() || !button.isAttachedToWindow()) return;
                            action.run(); main.postDelayed(this, 70);
                        }
                    };
                    main.postDelayed(repeat, 420); return true;
                case MotionEvent.ACTION_MOVE:
                    if (event.getX() < 0 || event.getY() < 0 || event.getX() >= button.getWidth() || event.getY() >= button.getHeight()) {
                        button.setPressed(false); stopRepeat();
                    }
                    return true;
                default: button.setPressed(false); stopRepeat(); return true;
            }
        });
    }
    private void stopRepeat() { if (repeat != null) main.removeCallbacks(repeat); repeat = null; }
    private boolean finishComposition() {
        return !composition() || engineCommand(mode == Mode.JAPANESE ? japanese.enterCommand() : 9);
    }
    private boolean composition() { return view != null && !view.optString("editing_text").isEmpty(); }
    private void type(String text) {
        type(text, false);
    }
    private void type(String text, boolean direct) {
        if (popover != null) dismissPopover();
        if (engineMode() && session==0 && options!=null) { openSession(); flushQueued(); }
        feedback = ""; typing = mode == Mode.CODING || !text.equals(" ");
        boolean japaneseSpelling = mode == Mode.JAPANESE && (text.equals("-") || text.equals("'"));
        if (!direct && engineMode() && (session==0) && (!symbols && !numeric || japaneseSpelling) && text.length()==1 && text.charAt(0)<128) {
            if (pendingEngine.length()<256) {pendingEngine.append(text);feedback=engineFailed ? "本地引擎未就绪，已保留输入；可切换英文" : "正在准备词库，已保留输入";}
            else feedback="准备期间输入已满，请等待词库就绪";
            render(); return;
        }
        if (mode == Mode.CODING && modifiers != 0) {
            CodingKeyMap.Stroke stroke = CodingKeyMap.character(text.charAt(0), modifiers | (shift ? CodingKeyMap.SHIFT : 0));
            if (stroke != null) send(stroke.code, stroke.meta);
            modifiers = 0; completion.clear();
            typing = false;
        } else if (!direct && engineMode() && session != 0 && text.length() == 1 && text.charAt(0) < 128 && (!symbols && !numeric || japaneseSpelling)) {
            if (mode == Mode.JAPANESE && japanese.converting() && !engineCommand(1)) return;
            try { if (!apply(NativeClient.character(session, text.charAt(0), shift || caps))) literal(text); }
            catch (Exception | LinkageError error) { engineFailure(); }
        } else {
            // Finish 会处理全部残余拼音；选择首候选可能只消费一段。
            if (!composition() || finishComposition() && !composition()) literal(text);
        }
        boolean shifted = shift;
        shift = false;
        if (engineMode()) typing = composition();
        if (shifted) build(); else render();
    }
    private void literal(String text) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) return;
        writing = true;
        try {
            if (!connection.commitText(text, 1)) { feedback = "当前应用没有接受文字"; completion.clear(); echo.invalidate(); return; }
            feedback = ""; echo.commit(text.length()); echo.expect();
            if (mode == Mode.CODING) completion.typed(text);
        } catch (RuntimeException error) { feedback = "当前输入连接已失效"; completion.clear(); echo.invalidate(); }
        finally { writing = false; }
    }
    private boolean apply(String response) throws Exception {
        JSONObject result = EngineInstall.value(response), next = result.getJSONObject("view");
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) throw new IllegalStateException("输入连接已失效");
        String commit = result.isNull("commit") ? null : result.optString("commit");
        japanese.sync(mode == Mode.JAPANESE ? next.optString("editing_text") : "");
        String composing = next.optString("editing_text");
        if (mode == Mode.JAPANESE) {
            composing = JapaneseInput.preedit(composing, next.optString("reading"));
            if (japanese.converting()) {
                JSONArray values = next.optJSONArray("candidates");
                if (values != null) for (int i = 0; i < values.length(); i++) {
                    JSONObject value = values.optJSONObject(i);
                    if (value != null && value.optBoolean("highlighted")) { composing = value.optString("text", composing); break; }
                }
            }
        }
        if (!echo.canPredict() && !composition() && !composing.isEmpty()) {
            // 删除、方向键等原始事件不可预测；在新组字前读取一次真实选区。
            android.view.inputmethod.ExtractedTextRequest request = new android.view.inputmethod.ExtractedTextRequest();
            request.hintMaxChars = 1;
            android.view.inputmethod.ExtractedText current = connection.getExtractedText(request, 0);
            if (current != null && current.selectionStart >= 0 && current.selectionEnd >= 0)
                echo.reset(current.startOffset + current.selectionStart, current.startOffset + current.selectionEnd);
        }
        writing = true;
        boolean batched = false;
        try {
            batched = connection.beginBatchEdit();
            EditorTransition.write(new EditorTransition.Sink() {
                public boolean commit(String text) {
                    String converted=output(text);
                    if (!connection.commitText(converted, 1)) return false;
                    echo.commit(converted.length()); echo.expect(); return true;
                }
                public boolean compose(String text) {
                    if (!connection.setComposingText(text, 1)) return false;
                    echo.compose(text.length()); echo.expect(); return true;
                }
                public boolean finish() {
                    if (!connection.finishComposingText()) return false;
                    echo.finish(); echo.expect(); return true;
                }
            }, commit, composing, composition());
            echo.expect(); view = next;
        } finally {
            try { if (batched) connection.endBatchEdit(); }
            finally { writing = false; }
        }
        render(); return result.optBoolean("handled");
    }
    private boolean engineCommand(int command) {
        if (session == 0) return false;
        try { apply(NativeClient.command(session, command)); return true; }
        catch (Exception | LinkageError error) { engineFailure(); render(); return false; }
    }
    private void engineFailure() {
        // native 已经推进时不可重试同一结果，否则可能重复上屏。
        closeSession(); echo.invalidate();
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) {
            try { connection.finishComposingText(); } catch (RuntimeException ignored) { }
        }
        feedback = "组字处理失败，点「重试」重新开始";
    }
    private void backspace() {
        feedback = ""; typing = false;
        if (pendingEngine.length()>0) { pendingEngine.setLength(pendingEngine.length()-1); render(); return; }
        if (mode == Mode.JAPANESE && composition() && japanese.converting()) { japanese.reset(); engineCommand(104); }
        else if (engineMode() && composition()) engineCommand(0);
        else { send(KeyEvent.KEYCODE_DEL, 0); completion.clear(); }
        render();
    }
    private void space() {
        if (mode == Mode.JAPANESE && composition()) {
            JSONArray values = view.optJSONArray("candidates");
            JSONObject first = values == null ? null : values.optJSONObject(0);
            engineCommand(japanese.spaceCommand(values == null ? 0 : values.length(), first == null ? -1 : first.optInt("source", -1)));
            typing = composition(); render();
        } else if (engineMode() && composition()) { engineCommand(1); typing = composition(); render(); }
        else type(" ");
    }
    private void enter() {
        if (engineMode() && session==0 && pendingEngine.length()>0) { pendingEngine.append('\n'); return; }
        typing = false;
        if (engineMode() && composition()) { engineCommand(mode == Mode.JAPANESE ? japanese.enterCommand() : 2); return; }
        if (mode == Mode.CODING) { stroke(KeyEvent.KEYCODE_ENTER); return; }
        InputConnection connection = getCurrentInputConnection(); EditorInfo info = getCurrentInputEditorInfo();
        int action = info == null ? EditorInfo.IME_ACTION_NONE : info.imeOptions & EditorInfo.IME_MASK_ACTION;
        if (connection != null && info != null && (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
                && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) connection.performEditorAction(action);
        else send(KeyEvent.KEYCODE_ENTER, 0);
    }
    private void toggle(int modifier) { modifiers ^= modifier; build(); }
    private void stroke(int code) { boolean shifted = shift; feedback = ""; typing = false; send(code, modifiers | (shift ? CodingKeyMap.SHIFT : 0)); modifiers = 0; shift = false; completion.clear(); if (shifted) build(); else render(); }
    private void send(int code, int meta) {
        InputConnection connection = getCurrentInputConnection(); if (connection == null) return;
        echo.invalidate(); writing = true;
        long down = SystemClock.uptimeMillis();
        try {
            if (!CodingInput.send((pressed, key, flags) -> connection.sendKeyEvent(new KeyEvent(
                down, SystemClock.uptimeMillis(), pressed ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP,
                key, 0, flags, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD)), code, meta))
                feedback = "当前应用未接受按键";
        } finally { writing = false; }
    }
    private String profileName() { return completion.profile() == CodingCompletion.Profile.SHELL ? "Shell" : completion.profile() == CodingCompletion.Profile.PYTHON ? "Python" : "JS/TS"; }
    private void render() {
        if(skinStack!=null)skinStack.regions(top,body,keys,skinFooter);
        if (top == null) return;
        top.setVisibility(View.VISIBLE);
        candidateOptions.clear();
        if (!composition() && mode != Mode.CODING || !feedback.isEmpty()) candidateExpanded = false;
        if (candidateExpanded && engineMode()) {
            try {
                if (candidateSnapshot == null || snapshotSession != session || candidateSnapshot.optLong("generation", -1) != view.optLong("generation", -2)) {
                    candidateSnapshot = EngineInstall.value(NativeClient.allCandidates(session));
                    snapshotSession = session;
                    // 全部候选释放后会重新排序，横栏也必须使用新的 ID。
                    view = EngineInstall.value(NativeClient.view(session));
                }
            } catch (Exception | LinkageError error) { candidateExpanded = false; feedback = "候选展开失败，请收起后重试"; }
        }
        if (!candidateExpanded) restoreCandidateBody();
        face(ctrlKey, (modifiers & CodingKeyMap.CTRL) != 0); face(altKey, (modifiers & CodingKeyMap.ALT) != 0);
        completion.setEnabled(!privateField);
        if (clipboardPanel || emojiPanel || toolsPanel || skinPanel) { toolbar(); return; }
        if (mode == Mode.CODING && !typing && feedback.isEmpty()) {
            candidateExpanded = false; restoreCandidateBody();
            top.removeAllViews(); top.setVisibility(View.GONE); return;
        }
        boolean suggestions = composition() || typing || modifiers != 0 || !feedback.isEmpty();
        String visibleText = clipSuggestion.text();
        if (!suggestions && (visibleText == null || privateField || !getSharedPreferences("offline", MODE_PRIVATE).getBoolean("clipboard", true))) { toolbar(); return; }
        top.removeAllViews();
        if (!suggestions) {
            add(top, tool(KeyboardIcon.Kind.CLIPBOARD, "打开剪贴板", this::showClipboard), .7f);
            Button preview = key(visibleText, "粘贴最近复制的文字，" + preview(visibleText), () -> paste(visibleText), false);
            clipboardText(preview,visibleText,true); preview.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL); preview.setPadding(dp(10), 0, dp(10), 0);
            add(top, preview, 4); add(top, tool(KeyboardIcon.Kind.TOOLS, "显示工具栏", () -> { dismissClipboardSuggestion(); render(); }), .7f); return;
        }
        long candidateGeneration = view == null ? -1 : view.optLong("generation", -1), targetEditor = editorGeneration;
        CandidateStrip scroll = new CandidateStrip(this, view != null && view.optInt("page", 0) > 0, direction -> {
            if (targetEditor != editorGeneration || !engineMode() || view == null || !composition()
                || candidateGeneration != view.optLong("generation", -2)) return;
            int oldPage = view.optInt("page", 0), oldX = candidateScroll == null ? 0 : candidateScroll.getScrollX();
            if (engineCommand(direction > 0 ? 100 : 101) && candidateScroll != null && view != null) {
                CandidateStrip current = candidateScroll;
                boolean samePage = oldPage == view.optInt("page", 0);
                current.post(() -> { if (current == candidateScroll && current.isAttachedToWindow())
                    current.scrollTo(samePage ? oldX : direction < 0 ? current.getChildAt(0).getWidth() : 0, 0); });
            }
        });
        candidateScroll = scroll;
        if(theme.palette.id.equals("retro")){
            Button brand=tool(KeyboardIcon.Kind.BRAND,"NOPE 工具",this::tools);
            top.addView(brand,new LinearLayout.LayoutParams(dp(44),-1));
            scroll.setBackground(new RetroDrawable(theme.palette,RetroDrawable.Surface.DISPLAY,false,theme.candidateFill,theme.density));
        }else if(theme.palette.id.equals("lavender"))scroll.setBackground(new PixelGlassDrawable(PixelGlassDrawable.Surface.PANEL,theme.candidateFill,false,theme.density));
        else if(theme.candidateFill!=theme.palette.candidateFill())scroll.setBackgroundColor(theme.candidateFill);
        candidates = new LinearLayout(this); candidates.setGravity(android.view.Gravity.CENTER_VERTICAL); scroll.addView(candidates);
        if(theme.palette.id.equals("retro")&&composition()&&feedback.isEmpty()){
            String editing=view.optString("editing_text");
            if(mode==Mode.JAPANESE)editing=JapaneseInput.preedit(editing,view.optString("reading"));
            TextView reading=label(editing,22);reading.setTextColor(theme.candidateInk);reading.setTypeface(RetroTypeface.get(this));
            reading.setSingleLine(true);reading.setMaxWidth(dp(130));reading.setEllipsize(TextUtils.TruncateAt.START);
            candidates.addView(reading,new LinearLayout.LayoutParams(-2,-1));
            View cursor=new View(this);cursor.setBackgroundColor(theme.candidateInk);candidates.addView(cursor,new LinearLayout.LayoutParams(dp(4),dp(23)));
            View divider=new View(this);divider.setBackgroundColor(0xff75876b);LinearLayout.LayoutParams separation=new LinearLayout.LayoutParams(dp(1),dp(26));separation.setMargins(dp(15),0,dp(2),0);candidates.addView(divider,separation);
        }
        top.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1));
        if (!feedback.isEmpty()) { TextView error = label(feedback, 16); if(theme.palette.id.equals("retro"))error.setTextColor(theme.candidateInk); candidates.addView(error, new LinearLayout.LayoutParams(-2, -1));
            if (engineMode() && session == 0) candidates.addView(key("重试", "重新准备输入引擎", this::retryEngine, false), new LinearLayout.LayoutParams(dp(60), -1)); return; }
        if (engineMode() && view != null && composition()) {
            JSONArray values = view.optJSONArray("candidates");
            if (values != null) for (int i = 0; i < values.length(); i++) {
                JSONObject candidate = values.optJSONObject(i); if (candidate == null) continue;
                JSONObject id = candidate.optJSONObject("id"); if (id == null) continue;
                long generation = id.optLong("generation", -1), index = id.optLong("index", -1); String text = output(candidate.optString("text"));
                candidate(text, "选择 " + text + (candidate.optBoolean("corrected") ? "，纠错候选" : ""), () -> {
                    if (view == null || generation != view.optLong("generation", -2)) return;
                    candidateExpanded = false;
                    try { apply(NativeClient.select(session, generation, index)); typing = composition(); render(); }
                    catch (Exception | LinkageError error) { engineFailure(); render(); }
                });
                if (mode == Mode.JAPANESE && japanese.converting() && candidate.optBoolean("highlighted")) {
                    View selected = candidates.getChildAt(candidates.getChildCount() - 1);
                    selected.setBackground(chrome(accent, 5)); ((Button)selected).setTextColor(theme.selectedInk);
                }
            }
        } else if (mode == Mode.CODING && modifiers == 0 && !privateField && !completion.prefix().isEmpty()) {
            for (CodingCompletion.Suggestion suggestion : completion.suggestions()) {
                candidate(suggestion.entry.text, "只填入 " + suggestion.entry.text, () -> { String suffix = completion.suffix(suggestion); if (suffix != null) { literal(suffix); render(); } });
            }
        }
        if (!candidateOptions.isEmpty()) {
            Button expand = tool(candidateExpanded ? KeyboardIcon.Kind.UP : KeyboardIcon.Kind.DOWN,
                candidateExpanded ? "收起候选，返回键盘" : "展开候选，上下滑动浏览", () -> { candidateExpanded = !candidateExpanded; render(); });
            top.addView(expand, new LinearLayout.LayoutParams(dp(48), -1));
        } else candidateExpanded = false;
        updateCandidateBody();
        if (candidates.getChildCount() == 0) {
            TextView hint=label(modifiers != 0 ? "修饰键作用于下一个键" : privateField ? "私密输入 · 不学习" : mode == Mode.CODING ? "Tab 使用终端补全" : engineMode() ? "本地输入暂不可用 · 长按空格切换" : "英文直输", 16);
            if(theme.palette.id.equals("retro"))hint.setTextColor(theme.candidateInk);
            candidates.addView(hint,new LinearLayout.LayoutParams(-2,-1));
        }
    }
    private int candidateHeight() {
        return Math.max(dp(48), Math.round(KeyboardText.CANDIDATE_SP * getResources().getDisplayMetrics().scaledDensity * 1.4f) + dp(12));
    }
    private Button candidateButton(String text, String description, Runnable action, boolean expanded) {
        KeyButton button = (KeyButton)key(text, description, action, false);
        button.skinRole("candidate");button.fitText(false); button.textScale(1); button.setText(text);
        button.pressFeedback(false,theme.palette.id.equals("retro"));button.pixelLetters(false);
        if(theme.config.keyFont("candidate").equals("pixel"))button.setTypeface(RetroTypeface.get(this));
        if(theme.palette.id.equals("retro")){button.getPaint().setAntiAlias(false);button.getPaint().setSubpixelText(false);}
        button.setTextSize(KeyboardText.CANDIDATE_SP); button.setTextColor(theme.candidateInk);
        button.setPadding(dp(14), dp(8), dp(14), dp(8)); button.setMinHeight(candidateHeight());
        if (expanded) { button.setSingleLine(false); button.setHorizontallyScrolling(false); }
        button.setBackground(chrome(expanded && !theme.palette.id.equals("retro") ? IosTheme.alpha(foreground, 12) : Color.TRANSPARENT, 5));
        return button;
    }
    private void candidate(String text, String description, Runnable action) {
        long editor = editorGeneration, targetSession = session;
        Runnable guarded = () -> { if (editor == editorGeneration && targetSession == session) action.run(); };
        candidateOptions.add(new CandidateOption(text, description, guarded));
        Button button = candidateButton(text, description, guarded, false);
        if (candidates.getChildCount()>0) { View divider=new View(this); divider.setBackgroundColor(IosTheme.alpha(foreground,45)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(1),dp(22)); candidates.addView(divider,p); }
        candidates.addView(button, new LinearLayout.LayoutParams(-2, candidateHeight()));
    }
    private void restoreCandidateBody() {
        if (body == null) return;
        if (expandedCandidates != null) { body.removeView(expandedCandidates); expandedCandidates = null; }
        if (keys != null) keys.setVisibility(View.VISIBLE);
        expandedGeneration = -1;
    }
    private void updateCandidateBody() {
        if (!candidateExpanded) { restoreCandidateBody(); return; }
        if (body == null || keys == null) return;
        long generation = view == null ? -1 : view.optLong("generation", -1);
        int previousY = expandedCandidates != null && expandedGeneration == generation ? expandedCandidates.getScrollY() : 0;
        if (expandedCandidates != null) body.removeView(expandedCandidates);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setContentDescription("全部候选，上下滑动浏览");
        expandedCandidates = scroll; expandedGeneration = generation;
        if(theme.palette.id.equals("retro"))scroll.setBackground(new RetroDrawable(theme.palette,RetroDrawable.Surface.DISPLAY,false,theme.candidateFill,theme.density));
        CandidateFlow flow = new CandidateFlow(this); flow.setPadding(dp(4), dp(4), dp(4), dp(4));
        scroll.addView(flow, new ScrollView.LayoutParams(-1, -2));
        if (engineMode() && candidateSnapshot != null) {
            JSONArray values = candidateSnapshot.optJSONArray("candidates");
            long editor = editorGeneration, targetSession = session, targetGeneration = candidateSnapshot.optLong("generation", -1);
            if (values != null) for (int i = 0; i < values.length(); i++) {
                JSONObject value = values.optJSONObject(i); if (value == null) continue;
                JSONObject id = value.optJSONObject("id"); if (id == null) continue;
                long index = id.optLong("index", -1); String text = output(value.optString("text"));
                flow.addView(candidateButton(text, "选择 " + text, () -> {
                    if (editor != editorGeneration || targetSession != session || view == null || targetGeneration != view.optLong("generation", -2)) return;
                    candidateExpanded = false;
                    try { apply(NativeClient.selectAnyCandidate(session, targetGeneration, index)); typing = composition(); render(); }
                    catch (Exception | LinkageError error) { engineFailure(); render(); }
                }, true));
            }
        } else for (CandidateOption option : candidateOptions) {
            flow.addView(candidateButton(option.text(), option.description(), () -> { candidateExpanded = false; option.action().run(); }, true));
        }
        keys.setVisibility(View.GONE); body.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        scroll.post(() -> { if (expandedCandidates == scroll && scroll.isAttachedToWindow()) scroll.scrollTo(0, previousY); });
    }
    @Override public void onUpdateSelection(int oldStart, int oldEnd, int newStart, int newEnd, int composingStart, int composingEnd) {
        super.onUpdateSelection(oldStart, oldEnd, newStart, newEnd, composingStart, composingEnd);
        // 同进程宿主可能在写入返回前同步回报；此时预测尚未推进，不能再次定基。
        if (writing) return;
        boolean unchanged = echo.matchesCurrent(newStart, newEnd, composingStart, composingEnd);
        boolean own = echo.acknowledge(newStart, newEnd, composingStart, composingEnd);
        if (own || unchanged) return;
        completion.clear(); typing = false;
        if (composition() && session != 0) {
            // 光标由外部移动时只清引擎状态，不向新的光标位置回写旧组字。
            try {
                JSONObject cancelled = EngineInstall.value(NativeClient.command(session, 3));
                view = cancelled.getJSONObject("view");
                InputConnection connection = getCurrentInputConnection();
                if (connection != null) connection.finishComposingText();
            } catch (Exception | LinkageError ignored) { closeSession(); }
        }
        render();
    }
    @Override public boolean onKeyDown(int code, KeyEvent event) {
        completion.clear(); modifiers = 0; stopRepeat();
        if (engineMode() && !event.isCtrlPressed() && !event.isAltPressed()) {
            if (code == KeyEvent.KEYCODE_DEL && (composition() || pendingEngine.length() > 0)) { backspace(); return true; }
            if (code == KeyEvent.KEYCODE_SPACE && (composition() || pendingEngine.length() > 0)) { space(); return true; }
            if (code == KeyEvent.KEYCODE_ENTER && (composition() || pendingEngine.length() > 0)) { enter(); return true; }
            int character = event.getUnicodeChar();
            if (mode == Mode.JAPANESE && (character == '\'' || character == '-')) { type(String.valueOf((char)character)); return true; }
            if (character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z') { type(String.valueOf((char) character)); return true; }
        }
        render(); return super.onKeyDown(code, event);
    }
}
