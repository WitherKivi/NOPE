package app.msime.offline;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** 静态原生毛玻璃材质；不采样宿主画面，不改变任何布局或触摸边界。 */
final class PixelGlassDrawable extends Drawable {
    enum Surface { FRAME, PANEL, KEY }
    private final Surface surface;
    private final int color;
    private final boolean pressed;
    private final float d;
    private final Paint paint = new Paint();
    PixelGlassDrawable(Surface surface, int color, boolean pressed, float density) {
        this.surface=surface;this.color=color;this.pressed=pressed;d=density;
    }
    private void box(Canvas canvas,float l,float t,float r,float b,int color) {
        paint.setShader(null);paint.setColor(color);canvas.drawRect(l,t,r,b,paint);
    }
    @Override public void draw(Canvas canvas) {
        Rect bounds=getBounds();float l=bounds.left,t=bounds.top,r=bounds.right,b=bounds.bottom;
        if(bounds.isEmpty())return;
        if(surface==Surface.FRAME) {
            paint.setShader(new LinearGradient(l,t,r,b,new int[]{0xffeee7fb,color,0xffded4f4},null,Shader.TileMode.CLAMP));
            canvas.drawRect(bounds,paint);
            // Diffuse color under the frosted surface is deterministic and inexpensive.
            paint.setShader(new RadialGradient(l+bounds.width()*.2f,t+bounds.height()*.35f,Math.max(1,bounds.width()*.7f),new int[]{0x65f4a9df,0x00f4a9df},null,Shader.TileMode.CLAMP));canvas.drawRect(bounds,paint);
            paint.setShader(new RadialGradient(l+bounds.width()*.8f,t+bounds.height()*.8f,Math.max(1,bounds.width()*.6f),new int[]{0x558e9def,0x008e9def},null,Shader.TileMode.CLAMP));canvas.drawRect(bounds,paint);
            // Three crisp rim bands give a pink/violet glow without a blur pass.
            edge(canvas,l,t,r,b,2*d,0x60d27beb);edge(canvas,l+2*d,t+2*d,r-2*d,b-2*d,d,0xffb37ae9);
            edge(canvas,l+3*d,t+3*d,r-3*d,b-3*d,d,0xfffdf4ff);
            return;
        }
        float depth=surface==Surface.KEY?2*d:0;
        l+=d;t+=d;r-=d;b-=d;
        if(r<=l||b<=t+depth)return;
        if(depth>0)box(canvas,l,t+depth,r,b,0xffac91d2);
        b-=depth;
        int base=pressed?IosTheme.mix(color,0xffbf8fea,.28f):color;
        int top=IosTheme.mix(base,Color.WHITE,surface==Surface.PANEL?.42f:.35f);
        paint.setShader(new LinearGradient(l,t,r,b,new int[]{top,base,IosTheme.mix(base,0xffd7c6f4,.25f)},null,Shader.TileMode.CLAMP));canvas.drawRect(l,t,r,b,paint);
        edge(canvas,l,t,r,b,d,0xff6553cb);
        box(canvas,l+d,t+d,r-d,t+2*d,0xfffdfaff);
        box(canvas,l+d,t+d,l+2*d,b-d,0xfffdfaff);
        box(canvas,l+2*d,b-d,r-d,b,pressed?0xff9e6cce:0xffcc8ee7);
        box(canvas,r-2*d,t+2*d,r-d,b-d,0xffb8a3e1);
        // Sparse pixel highlights stay clear of the text center.
        box(canvas,l+3*d,t+3*d,l+5*d,t+4*d,0x90ffffff);
        box(canvas,l+3*d,t+4*d,l+4*d,t+6*d,0x70ffffff);
    }
    private void edge(Canvas canvas,float l,float t,float r,float b,float width,int color) {
        box(canvas,l,t,r,t+width,color);box(canvas,l,b-width,r,b,color);
        box(canvas,l,t,l+width,b,color);box(canvas,r-width,t,r,b,color);
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
