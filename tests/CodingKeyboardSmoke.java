import app.msime.android.CodingCompletion;
import app.msime.android.CodingInput;
import app.msime.android.CodingKeyMap;
import app.msime.android.CodingLayout;
import java.util.ArrayList;
import java.util.List;

/** 合成数据验证后缀补全、失效候选、半角键码及异常时的按键释放。 */
public final class CodingKeyboardSmoke {
    public static void main(String[] args) {
        completion();
        keyMap();
        keyDispatch();
        layout();
        System.out.println("Coding keyboard: suffix completion, privacy, stale candidates and terminal key dispatch passed");
    }

    private static void layout() {
        StringBuilder available = new StringBuilder(" ").append(CodingLayout.PUNCTUATION);
        for (boolean symbols : new boolean[] {false, true}) {
            for (boolean extra : new boolean[] {false, true}) {
                String[] rows = CodingLayout.rows(symbols, extra);
                check(rows.length == 3, "each layer keeps three rows");
                for (String row : rows) {
                    check(row.length() <= 10, "no character row exceeds ten keys");
                    available.append(row).append(row.toUpperCase(java.util.Locale.ROOT));
                }
            }
        }
        for (char value = 32; value <= 126; value++)
            check(available.indexOf(String.valueOf(value)) >= 0, "ASCII character is reachable: " + value);
    }

    private static void completion() {
        CodingCompletion model = new CodingCompletion();
        model.typed("git st");
        CodingCompletion.Suggestion status = find(model, "git status");
        check("atus".equals(model.suffix(status)), "only append the missing suffix");
        model.typed("a");
        check(model.suffix(status) == null, "late click cannot apply to a changed prefix");
        CodingCompletion.Suggestion current = find(model, "git status");
        check("tus".equals(model.suffix(current)), "new generation uses the new prefix");
        model.typed(model.suffix(current));
        check(model.suggestions().stream().noneMatch(item -> item.entry.text.equals("git status")),
            "an exact match never duplicates the command");
        model.clear();
        check(model.suffix(current) == null, "Tab, navigation and focus clear old candidates");
        model.typed("sudo git st");
        check("atus".equals(model.suffix(find(model, "git status"))), "match a newly typed command suffix");
        model.setEnabled(false);
        model.typed("synthetic_private_value");
        check(model.prefix().isEmpty() && model.suggestions().isEmpty(), "private input leaves no prefix");
        model.setEnabled(true);
        check(model.prefix().isEmpty(), "reenabling does not recover private text");
        model.setProfile(CodingCompletion.Profile.PYTHON);
        model.typed("pri");
        check("nt(".equals(model.suffix(find(model, "print("))), "Python snippet suffix");
        model.setProfile(CodingCompletion.Profile.JAVASCRIPT);
        model.typed("console.l");
        check("og(".equals(model.suffix(find(model, "console.log("))), "JS snippet suffix");
        model.typed("z".repeat(4096));
        check(model.prefix().length() < 64, "unrecognized input is never accumulated");
        for (CodingCompletion.Profile profile : CodingCompletion.Profile.values()) {
            for (CodingCompletion.Entry entry : CodingCompletion.catalog(profile)) {
                check(entry.text.chars().allMatch(c -> c >= 32 && c <= 126),
                    "catalog contains literal ASCII only, never Enter or escape sequences");
            }
        }
    }

    private static CodingCompletion.Suggestion find(CodingCompletion model, String text) {
        return model.suggestions().stream().filter(item -> item.entry.text.equals(text)).findFirst().orElseThrow();
    }

    private static void keyMap() {
        CodingKeyMap.Stroke ctrlC = CodingKeyMap.character('c', CodingKeyMap.CTRL);
        check(ctrlC.code == 31 && ctrlC.meta == 0x1000, "Ctrl+C uses Android KEYCODE_C and CTRL meta");
        CodingKeyMap.Stroke altB = CodingKeyMap.character('b', CodingKeyMap.ALT);
        check(altB.code == 30 && altB.meta == 2, "Alt+B word movement");
        check(CodingKeyMap.character('"', 0).code == 75
            && CodingKeyMap.character('"', 0).meta == 1, "double quote is Shift+apostrophe");
        check(CodingKeyMap.character('|', 0).code == 73, "pipe uses backslash key");
        check(CodingKeyMap.character('[', CodingKeyMap.CTRL).meta == 0x1000, "Ctrl+[ keeps literal bracket");
        for (char c = 32; c < 127; c++) check(CodingKeyMap.character(c, 0) != null, "all ASCII characters map: " + c);
        check(CodingKeyMap.character('中', 0) == null, "unsupported modifier characters do not degrade silently");
    }

    private static void keyDispatch() {
        List<String> events = new ArrayList<>();
        check(CodingInput.send((down, code, meta) -> {
            events.add((down ? "D" : "U") + code + ":" + meta);
            return true;
        }, 31, CodingKeyMap.CTRL), "Ctrl+C dispatch succeeds");
        check(events.equals(List.of("D113:4096", "D31:4096", "U31:4096", "U113:0")),
            "Ctrl released after the complete chord");
        for (int code : new int[] {61, 111, 19, 20, 21, 22, 66, 67, 122, 123}) {
            events.clear();
            check(CodingInput.send((down, key, meta) -> {
                events.add((down ? "D" : "U") + key);
                return true;
            }, code, 0), "terminal control key accepted");
            check(events.equals(List.of("D" + code, "U" + code)), "each control is a down/up pair");
        }
        events.clear();
        check(!CodingInput.send((down, code, meta) -> {
            events.add((down ? "D" : "U") + code);
            if (down && code == 31) throw new IllegalStateException("synthetic input failure");
            return true;
        }, 31, CodingKeyMap.CTRL | CodingKeyMap.ALT), "input failure is reported");
        check(events.equals(List.of("D113", "D57", "D31", "U31", "U57", "U113")),
            "failure still releases main key and every modifier in reverse order");
        events.clear();
        check(!CodingInput.send((down, code, meta) -> {
            events.add((down ? "D" : "U") + code);
            return !down;
        }, 31, CodingKeyMap.CTRL), "rejected modifier does not send a bare letter");
        check(events.equals(List.of("D113", "U113")), "attempted modifier is released after rejection");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
