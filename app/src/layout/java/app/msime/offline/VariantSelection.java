package app.msime.offline;

/** Geometry in pixels; leaving the allowance cancels instead of selecting a remote edge. */
public final class VariantSelection {
    private VariantSelection() { }
    public static float trackX(float raw, float start, int initial, int columns, int cell, int padding) {
        return padding + (initial % columns + .5f) * cell + raw - start;
    }
    public static int hit(float x, float y, int columns, int count, int cell, int padding, int allowance) {
        int rows = (count + columns - 1) / columns;
        if (x < -allowance || x > columns * cell + 2 * padding + allowance
            || y < -allowance || y > rows * cell + 2 * padding + allowance) return -1;
        int column = Math.max(0, Math.min(columns - 1, (int)Math.floor((x - padding) / cell)));
        int row = Math.max(0, Math.min(rows - 1, (int)Math.floor((y - padding) / cell)));
        int index = row * columns + column;
        return index < count ? index : -1;
    }
    public static int left(int center, int preferredIndex, int columns, int cell, int padding, int min, int max) {
        int width = columns * cell + 2 * padding;
        return Math.max(min, Math.min(max - width, center - padding - (preferredIndex % columns) * cell - cell / 2));
    }
}
