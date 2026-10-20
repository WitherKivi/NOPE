package app.msime.offline;

final class CandidatePageSwipe {
    private float x, y;
    private boolean active, left, right;
    void start(float x, float y, boolean left, boolean right) { this.x = x; this.y = y; this.left = left; this.right = right; active = true; }
    int direction(float x, float y, float threshold) {
        float dx = x - this.x, dy = y - this.y;
        if (!active || Math.abs(dx) < threshold || Math.abs(dy) > Math.abs(dx) * .6f) return 0;
        return right && dx < 0 ? 1 : left && dx > 0 ? -1 : 0;
    }
    void cancel() { active = false; }
}
