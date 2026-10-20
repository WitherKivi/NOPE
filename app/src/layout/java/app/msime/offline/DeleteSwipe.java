package app.msime.offline;

/** 删除手势可在抬手前退回，不因一次快速滑动误触而立即清空。 */
final class DeleteSwipe {
    private float startX, startY;
    private boolean active;
    void start(float x, float y) { startX = x; startY = y; active = true; }
    boolean moved(float x, float y, float slop) { return active && (Math.abs(x - startX) > slop || Math.abs(y - startY) > slop); }
    float progress(float x, float y, float threshold) {
        if (!active || Math.abs(x - startX) > threshold * 1.5f) return 0;
        return Math.max(0, Math.min(1, (startY - y) / threshold));
    }
    void cancel() { active = false; }
    boolean active() { return active; }
}
