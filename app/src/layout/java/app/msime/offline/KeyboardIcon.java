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
    private Drawable imageGlyph;
    private RetroRecordDrawable recordGlyph;
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
    void glyphColor(int color) { if (glyph != null) { glyph.color = color; glyph.invalidateSelf(); } if(imageGlyph!=null)imageGlyph.setTint(color); }
    void netStyle(NetAssets assets) {
        imageGlyph=assets.icon(getContext(),kind);
        if(imageGlyph!=null){
            int size=Math.round((kind==Kind.BRAND?28:24)*getResources().getDisplayMetrics().density);
            imageGlyph.setBounds(0,0,size,size);setCompoundDrawables(imageGlyph,null,null,null);
        }
    }
    void glyphSelected(boolean selected) { if (glyph != null) { glyph.selected = selected; glyph.invalidateSelf(); } }
    void retroBrand(boolean enabled) {
        if (glyph == null) return;
        glyph.retroBrand = enabled;
        if (kind == Kind.BRAND) {
            // A manually drawn record must not also participate in TextView's
            // compound-drawable measurement, tinting and single-line scrolling.
            recordGlyph = enabled ? new RetroRecordDrawable() : null;
            setCompoundDrawables(enabled ? null : glyph, null, null, null);
            scrollTo(0, 0);
        }
        invalidate();
    }
    void brandSkin(String skin){if(glyph!=null)glyph.skin=skin;}
    @Override protected void onDraw(Canvas canvas){
        if(recordGlyph!=null){
            float density=getResources().getDisplayMetrics().density;
            int size=Math.max(0,Math.min(Math.round(32*density),
                Math.min(getWidth()-Math.round(12*density),getHeight()-Math.round(16*density))));
            if(size==0)return;
            int x=(getWidth()-size)/2,y=(getHeight()-size)/2;
            int save=canvas.save();
            // View's content canvas includes scroll offsets; the logo belongs
            // to the fixed button face, independent of any stale text scroll.
            canvas.translate(getScrollX(),getScrollY());
            recordGlyph.setBounds(x,y,x+size,y+size);recordGlyph.draw(canvas);
            canvas.restoreToCount(save);return;
        }
        super.onDraw(canvas);
    }
    private static final class Glyph extends Drawable {
    private final Kind kind;
    private int color;
    private boolean selected;
    private boolean retroBrand;
    private String skin="system";
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
        paint.setColor(color); paint.setStrokeWidth(1.6f); paint.setStrokeCap(retroBrand?Paint.Cap.SQUARE:Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND); paint.setStyle(Paint.Style.STROKE);
        if(retroBrand&&(kind==Kind.CLIPBOARD||kind==Kind.EMOJI||kind==Kind.SETTINGS)){
            paint.setStrokeJoin(Paint.Join.MITER);
            if(kind==Kind.CLIPBOARD){line(c,8,5,5,5,5,21,19,21,19,5,16,5);line(c,9,3,15,3,15,7,9,7,9,3);line(c,9,12,15,12);line(c,9,16,15,16);}
            if(kind==Kind.EMOJI){line(c,7,3,17,3,21,7,21,17,17,21,7,21,3,17,3,7,7,3);line(c,8,14,10,17,14,17,17,14);paint.setStyle(Paint.Style.FILL);c.drawRect(7,8,9,10,paint);c.drawRect(15,8,17,10,paint);}
            if(kind==Kind.SETTINGS){line(c,3,6,21,6);line(c,3,12,21,12);line(c,3,18,21,18);line(c,8,3,8,9);line(c,16,9,16,15);line(c,10,15,10,21);}
            c.restoreToCount(save);return;
        }
        switch (kind) {
            case BRAND:
                if(BrandSymbols.draw(c,paint,path,skin))break;
                BrandMark.draw(c,paint,color);break;
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
