package app.msime.offline;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.util.function.Consumer;

/** Limit cross-window blur to the keyboard outline, never the editor above it. */
final class GlassWindow implements AutoCloseable {
    private final Window window;
    private final WindowManager manager;
    private final View keyboard;
    private final Consumer<Boolean> changed;
    private final Drawable outline;
    private boolean listening;
    private boolean systemEnabled;
    GlassWindow(Window window, WindowManager manager, View keyboard, Consumer<Boolean> changed) {
        this.window = window; this.manager = manager; this.keyboard = keyboard; this.changed = changed;
        outline = new Drawable() {
            @Override public void draw(Canvas canvas) { }
            @Override public void getOutline(Outline result) {
                int[] location = new int[2], decorLocation = new int[2];
                keyboard.getLocationInWindow(location); window.getDecorView().getLocationInWindow(decorLocation);
                int y = location[1] - decorLocation[1];
                result.setRect(0, y, getBounds().width(), Math.min(getBounds().height(), y + keyboard.getHeight()));
                result.setAlpha(1);
            }
            @Override public void setAlpha(int alpha) { }
            @Override public void setColorFilter(ColorFilter filter) { }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
        window.setBackgroundDrawable(outline);
        keyboard.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            outline.invalidateSelf(); window.getDecorView().invalidateOutline();
            update(systemEnabled);
        });
    }
    private final Consumer<Boolean> listener = enabled -> { if (listening) update(enabled); };
    void start() {
        if (Build.VERSION.SDK_INT < 31 || manager == null) { changed.accept(false); return; }
        if (listening) return;
        try { manager.addCrossWindowBlurEnabledListener(listener); listening = true; update(manager.isCrossWindowBlurEnabled()); }
        catch (RuntimeException error) { update(false); }
    }
    private void update(boolean enabled) {
        systemEnabled = enabled;
        boolean applied = false;
        if (Build.VERSION.SDK_INT >= 31) {
            // Full-screen IME decor cannot safely use a public window blur: keep the
            // editor sharp and fall back to a tonal material on those ROMs.
            int windowHeight = window.getDecorView().getHeight();
            boolean bounded = keyboard.getHeight() > 0 && windowHeight > 0 && windowHeight <= keyboard.getHeight() + 2;
            try { window.setBackgroundBlurRadius(enabled && bounded ? 64 : 0); applied = enabled && bounded; }
            catch (RuntimeException ignored) { }
        }
        changed.accept(applied);
    }
    @Override public void close() {
        if (Build.VERSION.SDK_INT >= 31 && listening) {
            listening = false;
            try { manager.removeCrossWindowBlurEnabledListener(listener); } catch (RuntimeException ignored) { }
        }
        update(false);
    }
}
