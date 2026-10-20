package app.msime.offline;

/** Release-to-apply editing shortcuts. Reversing, sideways drift or cancellation do nothing. */
public final class EditingSwipe {
    private EditingSwipe() { }
    public static String action(String letter) {
        return switch (letter.toLowerCase(java.util.Locale.ROOT)) {
            case "z" -> "撤回"; case "a" -> "全选"; case "x" -> "剪切"; case "c" -> "复制";
            case "v" -> "粘贴";
            default -> "";
        };
    }
    public static boolean ready(float dx, float dy, float threshold) {
        return dy >= threshold && Math.abs(dx) <= threshold * .75f;
    }
}
