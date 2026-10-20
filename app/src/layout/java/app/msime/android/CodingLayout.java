package app.msime.android;

/** 编程键面：符号分成两页，避免为了塞下全部字符而缩窄每个键。 */
public final class CodingLayout {
    private CodingLayout() {}
    public static final String PUNCTUATION = "./-_|$'\"`";
    public static String[] rows(boolean symbols, boolean extraSymbols) {
        if (!symbols) return new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
        if (extraSymbols) return new String[] {"`~@#$%^&*|", "-_+={}[]()", "/\\:;'\"<>"};
        return new String[] {"1234567890", "(){}[]<>\\=", ".:;,+!?&"};
    }
}
