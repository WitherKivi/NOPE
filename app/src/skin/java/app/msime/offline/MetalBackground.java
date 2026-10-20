package app.msime.offline;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Deterministic native brushed metal; no copied image or animation. */
final class MetalBackground extends Drawable {
    private final Paint paint=new Paint();
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds();paint.setShader(new LinearGradient(b.left,0,b.right,0,
            new int[]{0xff969ca2,0xffc2c7cb,0xff959ca3,0xffb4bac0},null,Shader.TileMode.CLAMP));
        canvas.drawRect(b,paint);paint.setShader(null);paint.setStrokeWidth(1);
        int step=Math.max(1,b.height()/240);
        for(int y=b.top;y<b.bottom;y+=step) {paint.setColor((y*17%5)<2?0x12ffffff:0x100d1822);canvas.drawLine(b.left,y,b.right,y,paint);}
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() {return PixelFormat.OPAQUE;}
}
