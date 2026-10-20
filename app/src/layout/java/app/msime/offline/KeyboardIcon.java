package app.msime.offline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** 同一线宽的原生矢量图标，避免使用字符字体代替工具图标。 */
final class KeyboardIcon extends KeyButton {
    enum Kind { BRAND, TOOLS, CLIPBOARD, EMOJI, SETTINGS, DOWN, UP, BACK, SHIFT, DELETE, ENTER, NUMBERS }
    private Glyph glyph;
    final Kind kind;
    KeyboardIcon(Context context, Kind kind) {
        super(context);
        this.kind = kind;
        if (kind != Kind.NUMBERS) {
            glyph = new Glyph(kind);
            int size = Math.round((kind == Kind.BRAND ? 28 : 24) * getResources().getDisplayMetrics().density);
            glyph.setBounds(0, 0, size, size); setCompoundDrawables(glyph, null, null, null);
            setCompoundDrawablePadding(0);
        }
    }
    static boolean toolbar(Kind kind) { return kind == Kind.BRAND || kind == Kind.TOOLS || kind == Kind.CLIPBOARD || kind == Kind.EMOJI || kind == Kind.SETTINGS || kind == Kind.DOWN || kind == Kind.UP; }
    static String caption(Kind kind) {
        return switch (kind) {
            case NUMBERS -> "123";
            default -> "";
        };
    }
    void glyphColor(int color) { if (glyph != null) { glyph.color = color; glyph.invalidateSelf(); } }
    void glyphSelected(boolean selected) { if (glyph != null) { glyph.selected = selected; glyph.invalidateSelf(); } }
    void retroBrand(boolean enabled) { if (glyph != null) { glyph.retroBrand = enabled; glyph.invalidateSelf(); } }
    private static final class Glyph extends Drawable {
    private final Kind kind;
    private int color;
    private boolean selected;
    private boolean retroBrand;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    Glyph(Kind kind) { this.kind = kind; }
    private void line(Canvas c, float... points) {
        path.reset(); path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        c.drawPath(path, paint);
    }
    @Override public void draw(Canvas c) {
        int save = c.save();
        float size = Math.min(getBounds().width(), getBounds().height());
        c.translate(getBounds().left, getBounds().top); c.scale(size / 24, size / 24);
        paint.setAntiAlias(!retroBrand);
        paint.setColor(color); paint.setStrokeWidth(retroBrand?2.2f:1.6f); paint.setStrokeCap(retroBrand?Paint.Cap.SQUARE:Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND); paint.setStyle(Paint.Style.STROKE);
        switch (kind) {
            case BRAND:
                if (!retroBrand) {
                    BrandMark.draw(c,paint,color);
                    break;
                }
                paint.setAntiAlias(false);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff6a5845);c.drawRect(1,1,23,23,paint);
                paint.setColor(0xffeee3cb);c.drawRect(2,2,22,22,paint);
                paint.setColor(0xffa8aba2);c.drawCircle(10,11,7,paint);
                paint.setColor(0xffd9ddd0);c.drawArc(3,4,17,18,210,80,true,paint);
                paint.setColor(0xffb0c7c9);c.drawArc(3,4,17,18,40,65,true,paint);
                paint.setColor(0xffc4adb5);c.drawArc(3,4,17,18,125,30,true,paint);
                paint.setColor(0xfff1e8d6);c.drawCircle(10,11,2,paint);
                paint.setColor(0xff536055);c.drawCircle(10,11,1,paint);
                paint.setColor(0xffece2cf);c.drawRect(13,8,23,17,paint);
                paint.setColor(0xff35483d);c.drawRect(14,9,22,15,paint);
                paint.setColor(0xff998b76);for(int x=5;x<=17;x+=4)c.drawRect(x,20,x+2,21,paint);
                paint.setAntiAlias(true);break;
            case TOOLS:
                paint.setStyle(Paint.Style.FILL);
                for (int x = 3; x <= 14; x += 11) for (int y = 3; y <= 14; y += 11) c.drawRoundRect(x, y, x + 7, y + 7, 2, 2, paint);
                break;
            case CLIPBOARD:
                c.drawRoundRect(5, 4, 19, 22, 2, 2, paint); c.drawRoundRect(8, 2, 16, 6, 1, 1, paint);
                line(c, 9, 11, 15, 11); line(c, 9, 15, 15, 15); line(c, 9, 19, 13, 19); break;
            case EMOJI:
                c.drawCircle(12, 12, 9, paint); c.drawArc(7, 7, 17, 17, 20, 140, false, paint);
                paint.setStyle(Paint.Style.FILL); c.drawCircle(9, 9, 1, paint); c.drawCircle(15, 9, 1, paint); break;
            case SETTINGS:
                c.drawCircle(12, 12, 3.5f, paint);
                for (int i = 0; i < 8; i++) { int s = c.save(); c.rotate(i * 45, 12, 12); line(c, 12, 2, 12, 5); c.restoreToCount(s); }
                c.drawCircle(12, 12, 7, paint); break;
            case DOWN: line(c, 5, 9, 12, 16, 19, 9); break;
            case UP: line(c, 5, 15, 12, 8, 19, 15); break;
            case BACK: line(c, 14, 5, 7, 12, 14, 19); line(c, 7, 12, 21, 12); break;
            case SHIFT:
                if (selected||retroBrand) paint.setStyle(Paint.Style.FILL);
                line(c, 3, 12, 12, 3, 21, 12, 16, 12, 16, 21, 8, 21, 8, 12, 3, 12); break;
            case DELETE: line(c, 8, 5, 21, 5, 21, 19, 8, 19, 2, 12, 8, 5); line(c, 11, 9, 17, 15); line(c, 17, 9, 11, 15); break;
            case ENTER: line(c, 20, 5, 20, 13, 4, 13); line(c, 9, 8, 4, 13, 9, 18); break;
            case NUMBERS:
                paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(11);
                c.drawText("1", 6, 10, paint); c.drawText("2", 18, 10, paint);
                c.drawText("3", 6, 23, paint); c.drawText("4", 18, 23, paint); break;
        }
        c.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
