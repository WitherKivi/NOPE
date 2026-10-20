package app.msime.offline;

/** 敏感字段绕开中文引擎；禁止学习的字段不记入本地词频。 */
final class FieldPolicy {
    private FieldPolicy() {}
    static boolean password(int type) {
        int kind = type & 15, variation = type & 4080;
        return kind == 1 && (variation == 128 || variation == 144 || variation == 224)
            || kind == 2 && variation == 16;
    }
    static boolean privateField(int type, int options) {
        return password(type) || (options & 0x1000000) != 0;
    }
    static boolean directField(int type) {
        int kind = type & 15, variation = type & 4080;
        return password(type) || kind == 2 || kind == 3 || kind == 4
            || kind == 1 && (variation == 16 || variation == 32 || variation == 208);
    }
}
