package app.msime.offline;

/** Separate tap jitter from an intentionally armed editing/variant gesture. */
public final class InputTouchPolicy {
    private InputTouchPolicy() { }
    public static boolean tap(float dx, float dy, float x, float y, int width, int height, int slop) {
        return Math.abs(dx)<=slop && Math.abs(dy)<=slop && x>=-slop && y>=-slop && x<=width+slop && y<=height+slop;
    }
}
