package app.msime.offline;

import java.util.ArrayList;
import java.util.List;

/** 点按语言键的循环；完整键盘列表仍由长按空格提供。 */
public final class QuickLanguages {
    public static final String DEFAULT = "CHINESE,ENGLISH";
    private static final List<String> SUPPORTED = List.of("CHINESE", "ENGLISH", "JAPANESE", "CODING");
    private QuickLanguages() { }
    public static String[] ids() { return SUPPORTED.toArray(new String[0]); }
    public static String name(String id) {
        return switch (id) {
            case "CHINESE" -> "中文";
            case "ENGLISH" -> "English";
            case "JAPANESE" -> "日本語";
            case "CODING" -> "Code";
            default -> id;
        };
    }
    public static List<String> parse(String stored) {
        List<String> result = new ArrayList<>();
        if (stored != null) for (String id : stored.split(",")) {
            id = id.trim();
            if (SUPPORTED.contains(id) && !result.contains(id)) result.add(id);
        }
        return result.size() >= 2 ? java.util.Collections.unmodifiableList(result) : List.of("CHINESE", "ENGLISH");
    }
    public static String next(String current, String stored) {
        List<String> ids = parse(stored);
        return ids.get((ids.indexOf(current) + 1) % ids.size());
    }
    public static String summary(String stored) {
        return parse(stored).stream().map(QuickLanguages::name).collect(java.util.stream.Collectors.joining(" ↔ "));
    }
}
