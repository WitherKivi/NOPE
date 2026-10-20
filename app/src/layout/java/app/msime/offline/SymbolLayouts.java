package app.msime.offline;

/** Screenshot-based layout; Unicode variants are inserted only after explicit selection. */
public final class SymbolLayouts {
    private SymbolLayouts() { }
    public static String[][] page(boolean extra) {
        return extra ? new String[][]{
            {"~", "`", "|", "•", "√", "π", "÷", "×", "*", "§"},
            {"£", "¢", "€", "¥", "^", "°", "=", "「", "」", "\\"},
            {"%", "‘", "’", "™", "✓", "[", "]"}
        } : new String[][]{
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"@", "#", "$", "_", "&", "-", "+", "(", ")", "/"},
            {"、", "\"", "'", ":", ";", "!", "?"}
        };
    }
    public static String hint(String text) {
        if (text.equals(".")) return ",";
        if (text.equals("。")) return "，";
        if (text.equals(",")) return ".";
        if (text.equals("，")) return "。";
        int index = "qwertyuiop".indexOf(text.toLowerCase(java.util.Locale.ROOT));
        return text.length() == 1 && index >= 0 ? String.valueOf("1234567890".charAt(index)) : "";
    }
    public static String[] variants(String text) {
        String hint = hint(text);
        if (!hint.isEmpty()) return new String[]{hint, text};
        return switch (text) {
            case "a" -> new String[]{"á", "à", "ä", "â", text};
            case "A" -> new String[]{"Á", "À", "Ä", "Â", text};
            case "s" -> new String[]{"ß", "ś", text};
            case "S" -> new String[]{"ẞ", "Ś", text};
            case "c" -> new String[]{"ç", "ć", text};
            case "C" -> new String[]{"Ç", "Ć", text};
            case "n" -> new String[]{"ñ", "ń", text};
            case "N" -> new String[]{"Ñ", "Ń", text};
            case "(", "「" -> new String[]{"【", "[", "{", text, "<"};
            case ")", "」" -> new String[]{"】", "]", "}", text, ">"};
            case "[" -> new String[]{"{", "[", "<"};
            case "]" -> new String[]{"}", "]", ">"};
            case "_", "-" -> new String[]{"—", "_", "-", "·"};
            case "+" -> new String[]{"±", "+"};
            case "?" -> new String[]{"?", "¿"};
            case "!" -> new String[]{"!", "¡"};
            case "$", "£", "¢", "€", "¥" -> new String[]{"₹", "¥", "₱", "€", "¢", "£", "$"};
            case "#" -> new String[]{"№", "#"};
            case "=" -> new String[]{"∞", "≠", "≈", "="};
            case "π" -> new String[]{"Ω", "Π", "μ", "π"};
            case "§" -> new String[]{"¶", "§"};
            case "*" -> new String[]{"★", "‡", "†", "*"};
            case "\"" -> new String[]{"“", "\"", "”", "«", "»"};
            case "'", "‘", "’" -> new String[]{"‘", "'", "’", "‹", "›"};
            case "/" -> new String[]{"\\", "/"};
            case "、" -> new String[]{"\\", "、"};
            case ".", "。" -> new String[]{"…", ".", "。"};
            case "%" -> new String[]{"‰", "%"};
            case "^" -> new String[]{"↑", "^"};
            default -> new String[0];
        };
    }
    public static int initial(String text, String[] variants, boolean swipe) {
        if (swipe) return 0;
        for (int i = 0; i < variants.length; i++) if (text.equals(variants[i])) return i;
        return 0;
    }
}
