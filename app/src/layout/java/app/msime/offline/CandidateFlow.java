package app.msime.offline;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** 按实际词长换行；长候选折行，所有候选保持同一字号。 */
final class CandidateFlow extends ViewGroup {
    private final int gap;
    CandidateFlow(Context context) {
        super(context);
        gap = Math.round(4 * getResources().getDisplayMetrics().density);
    }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec), available = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        int x = 0, y = getPaddingTop(), rowHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            child.measure(MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            if (x > 0 && x + child.getMeasuredWidth() > available) { x = 0; y += rowHeight + gap; rowHeight = 0; }
            x += child.getMeasuredWidth() + gap;
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
        }
        setMeasuredDimension(width, resolveSize(y + rowHeight + getPaddingBottom(), heightSpec));
    }
    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int available = right - left - getPaddingLeft() - getPaddingRight(), x = 0, y = getPaddingTop(), rowHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int width = child.getMeasuredWidth(), height = child.getMeasuredHeight();
            if (x > 0 && x + width > available) { x = 0; y += rowHeight + gap; rowHeight = 0; }
            child.layout(getPaddingLeft() + x, y, getPaddingLeft() + x + width, y + height);
            x += width + gap; rowHeight = Math.max(rowHeight, height);
        }
    }
    @Override protected LayoutParams generateDefaultLayoutParams() { return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); }
}
