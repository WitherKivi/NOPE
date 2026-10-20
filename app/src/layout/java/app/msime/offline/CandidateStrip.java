package app.msime.offline;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import java.util.function.IntConsumer;

/** Native scrolling within a page; an outward swipe at an edge advances one engine page. */
final class CandidateStrip extends HorizontalScrollView {
    private final CandidatePageSwipe swipe = new CandidatePageSwipe();
    private final IntConsumer page;
    private final boolean previous;
    private final float threshold;
    private boolean owned, cancelled;
    CandidateStrip(Context context, boolean previous, IntConsumer page) {
        super(context); this.previous = previous; this.page = page;
        threshold = 36 * getResources().getDisplayMetrics().density;
        setHorizontalScrollBarEnabled(false); setOverScrollMode(OVER_SCROLL_NEVER);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            owned = false; cancelled = false;
            swipe.start(event.getRawX(), event.getRawY(), previous && !canScrollHorizontally(-1), !canScrollHorizontally(1));
        }
        if (event.getPointerCount() > 1 || action == MotionEvent.ACTION_CANCEL) { swipe.cancel(); cancelled = true; }
        if (event.getPointerCount() > 1 && !owned) {
            MotionEvent cancel = MotionEvent.obtain(event); cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel); cancel.recycle(); owned = true;
        }
        int direction = swipe.direction(event.getRawX(), event.getRawY(), threshold);
        if (action == MotionEvent.ACTION_MOVE && direction != 0 && !owned && !cancelled) {
            MotionEvent cancel = MotionEvent.obtain(event); cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel); cancel.recycle(); owned = true;
        }
        if (owned) {
            if (action == MotionEvent.ACTION_UP) {
                swipe.cancel(); owned = false;
                if (!cancelled && direction != 0 && isAttachedToWindow()) page.accept(direction);
            }
            return true;
        }
        boolean result = super.dispatchTouchEvent(event);
        if (action == MotionEvent.ACTION_UP) swipe.cancel();
        return result;
    }
    @Override public boolean performAccessibilityAction(int action, android.os.Bundle arguments) {
        if (isAttachedToWindow() && action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD && !canScrollHorizontally(1)) { page.accept(1); return true; }
        if (isAttachedToWindow() && previous && action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD && !canScrollHorizontally(-1)) { page.accept(-1); return true; }
        return super.performAccessibilityAction(action, arguments);
    }
}
