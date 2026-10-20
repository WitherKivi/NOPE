package app.msime.offline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Source-anchored capsule with immediate finger-controlled selection, no timed animation. */
final class VariantPanel extends View {
    final String[] values;
    final int cell, padding, columns;
    int selected;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int fill, ink, highlight, selectedInk;
    VariantPanel(Context context, String[] values, int selected, int cell, int columns,
                 int fill, int ink, int highlight, int selectedInk) {
        super(context); this.values = values; this.selected = selected; this.cell = cell; this.columns = columns;
        this.padding = Math.round(8 * getResources().getDisplayMetrics().density);
        this.fill = fill; this.ink = ink; this.highlight = highlight; this.selectedInk = selectedInk;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    int width() { return columns * cell + 2 * padding; }
    int height() { return ((values.length + columns - 1) / columns) * cell + 2 * padding; }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); paint.setColor(fill);
        float radius = 12 * getResources().getDisplayMetrics().density;
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), radius, radius, paint);
        paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(25 * getResources().getDisplayMetrics().scaledDensity);
        for (int i = 0; i < values.length; i++) {
            float x = padding + (i % columns + .5f) * cell, y = padding + (i / columns + .5f) * cell;
            if (i == selected) {
                paint.setColor(highlight); float half = cell * .44f;
                canvas.drawRoundRect(x-half, y-half, x+half, y+half, radius * .5f, radius * .5f, paint);
            }
            paint.setColor(i == selected ? selectedInk : ink);
            canvas.drawText(values[i], x, y - (paint.ascent() + paint.descent()) / 2, paint);
        }
    }
}
