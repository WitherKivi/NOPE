package app.msime.offline;

/** 普通键盘参照截图比例；Code 增加空间，候选变化不改变各模式高度。 */
final class KeyboardMetrics {
    private KeyboardMetrics() { }
    static int height(int preference, int width, int screenHeight) {
        int desired = preference == 0 ? Math.max(240, Math.min(320, Math.round(width * .72f))) : Math.max(220, Math.min(420, preference));
        return Math.min(desired, Math.max(160, Math.round(screenHeight * .65f)));
    }
    static int body(int height) { return height - 56; }
    static int codeHeight(int preference, int normalHeight, int screenHeight) {
        int desired = preference == 0 ? Math.max(340, Math.min(520, normalHeight + 120)) : Math.max(320, Math.min(520, preference));
        return Math.min(desired, Math.max(200, Math.round(screenHeight * .82f)));
    }
    static int controls(int height) { return height < 320 ? 32 : 40; }
    static int punctuation(int height) { return height < 320 ? 28 : 36; }
    static int navigation(int height) { return height < 320 ? 32 : 44; }
    static int auxiliary(int height) { return controls(height) + punctuation(height) + navigation(height); }
    static int row(int height, boolean coding) { return (body(height) - (coding ? auxiliary(height) : 0)) / 4; }
}
