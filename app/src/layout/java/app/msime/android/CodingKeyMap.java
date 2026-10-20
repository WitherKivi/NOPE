package app.msime.android;

/** Android KeyEvent 常量的无运行时映射，用于测试终端修饰键与半角符号。 */
public final class CodingKeyMap {
    private CodingKeyMap() { }
    public static final int SHIFT = 1;
    public static final int ALT = 2;
    public static final int CTRL = 0x1000;

    public static final class Stroke {
        public final int code;
        public final int meta;
        Stroke(int code, int meta) { this.code = code; this.meta = meta; }
    }

    /** 字符对应的物理键及必需的 Shift；不支持时返回空值，不偷偷丢掉修饰键。 */
    public static Stroke character(char value, int modifiers) {
        if (value >= 'a' && value <= 'z') return new Stroke(29 + value - 'a', modifiers);
        if (value >= 'A' && value <= 'Z') return new Stroke(29 + value - 'A', modifiers | SHIFT);
        if (value >= '0' && value <= '9') return new Stroke(7 + value - '0', modifiers);
        String plain = "`-=[]\\;',./ ";
        String shifted = "~_+{}|:\"<>? ";
        int[] codes = {68, 69, 70, 71, 72, 73, 74, 75, 55, 56, 76, 62};
        int index = plain.indexOf(value);
        if (index >= 0) return new Stroke(codes[index], modifiers);
        index = shifted.indexOf(value);
        if (index >= 0) return new Stroke(codes[index], modifiers | SHIFT);
        index = ")!@#$%^&*(".indexOf(value);
        return index >= 0 ? new Stroke(7 + index, modifiers | SHIFT) : null;
    }
}
