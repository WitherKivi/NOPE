package app.msime.offline;

/** 设置边界共用一处；候选文字不随键帽大小变化。 */
final class KeyboardText {
    static final int MIN_PERCENT = 80, MAX_PERCENT = 140, DEFAULT_PERCENT = 100;
    static final int CANDIDATE_SP = 22;
    static int percent(int value) { return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, value)); }
    static float scale(int value) { return percent(value) / 100f; }
    private KeyboardText() { }
}
