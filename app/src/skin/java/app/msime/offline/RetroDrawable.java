package app.msime.offline;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** S4U 的机壳、键帽与液晶槽共用整像素网格，无位图纹理或平滑渐变。 */
final class RetroDrawable extends Drawable {
    enum Surface { FRAME, KEY, DISPLAY, HEADER, SOCKET }
    static final int LCD = SkinPalette.RETRO_LCD, LCD_INK = SkinPalette.RETRO_LCD_INK;
    private final SkinPalette palette;
    private final Surface surface;
    private final boolean pressed;
    private final int fill;
    private final float density;
    private final Paint paint = new Paint();
    private final Path path = new Path();
    RetroDrawable(SkinPalette palette, Surface surface, boolean pressed, int fill, float density) {
        this.palette = palette; this.surface = surface; this.pressed = pressed; this.fill = fill; this.density = density;
    }
    private void box(Canvas canvas, float l, float t, float r, float b, int color) {
        paint.setColor(color); canvas.drawRect(Math.round(l), Math.round(t), Math.round(r), Math.round(b), paint);
    }
    private void cap(Canvas canvas, float l, float t, float r, float b, float p, int color) {
        paint.setColor(color); path.reset();
        path.moveTo(l + 2*p, t); path.lineTo(r - 2*p, t); path.lineTo(r - 2*p, t + p);
        path.lineTo(r - p, t + p); path.lineTo(r - p, t + 2*p); path.lineTo(r, t + 2*p);
        path.lineTo(r, b - 2*p); path.lineTo(r - p, b - 2*p); path.lineTo(r - p, b - p);
        path.lineTo(r - 2*p, b - p); path.lineTo(r - 2*p, b); path.lineTo(l + 2*p, b);
        path.lineTo(l + 2*p, b - p); path.lineTo(l + p, b - p); path.lineTo(l + p, b - 2*p);
        path.lineTo(l, b - 2*p); path.lineTo(l, t + 2*p); path.lineTo(l + p, t + 2*p);
        path.lineTo(l + p, t + p); path.lineTo(l + 2*p, t + p); path.close(); canvas.drawPath(path, paint);
    }
    private void plane(Canvas canvas,int color,float... points){
        paint.setColor(color);path.reset();path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);
        path.close();canvas.drawPath(path,paint);
    }
    @Override public void draw(Canvas canvas) {
        Rect b = getBounds(); float p = Math.max(1, Math.round(density));
        if(surface==Surface.HEADER){
            cap(canvas,b.left,b.top,b.right,b.bottom,p,palette.outline);
            cap(canvas,b.left+p,b.top+p,b.right-p,b.bottom-p,p,0xffdbcbb0);
            box(canvas,b.left+2*p,b.top+2*p,b.right-2*p,b.bottom-5*p,palette.background);
            box(canvas,b.left+3*p,b.top+p,b.right-3*p,b.top+2*p,0xfffff2db);
            plane(canvas,0xffa18c6f,b.left+p,b.bottom-5*p,b.right-p,b.bottom-5*p,b.right-2*p,b.bottom-p,b.left+2*p,b.bottom-p);
            box(canvas,b.left+2*p,b.bottom-5*p,b.right-2*p,b.bottom-4*p,0xfff5e7cb);
            for(float x=b.left+3*p;x<b.right-3*p;x+=4*p)box(canvas,x,b.bottom-3*p,x+p,b.bottom-2*p,0xffb09b7d);
            return;
        }
        if(surface==Surface.SOCKET){
            float l=b.left+3*p,t=b.top+6*p,r=b.right-3*p,bottom=b.bottom-6*p;
            box(canvas,l,t,r,bottom,palette.outline);box(canvas,l+p,t+p,r-p,bottom-p,0xfff5e9d2);
            box(canvas,l+2*p,t+2*p,r-2*p,bottom-2*p,0xffaa9a81);
            box(canvas,l+3*p,t+3*p,r-3*p,bottom-3*p,0xffddd0b9);return;
        }
        if (surface == Surface.DISPLAY) {
            box(canvas, b.left, b.top, b.right, b.bottom, palette.outline);
            box(canvas, b.left+p, b.top+p, b.right-p, b.bottom-p, 0xfff7ead2);
            box(canvas, b.left+2*p, b.top+2*p, b.right-2*p, b.bottom-2*p, 0xffa2967b);
            box(canvas, b.left+3*p, b.top+3*p, b.right-3*p, b.bottom-3*p, fill);
            box(canvas, b.left+3*p, b.top+3*p, b.right-3*p, b.top+4*p, 0xff243229);
            for (float y=b.top+5*p; y<b.bottom-3*p; y+=3*p)
                box(canvas,b.left+4*p,y,b.right-4*p,y+p,0x089bb596);
            return;
        }
        if (surface == Surface.FRAME) {
            box(canvas,b.left,b.top,b.right,b.bottom,palette.background);
            box(canvas,b.left,b.top,b.right,b.top+2*p,0xfff1e4c8);
            box(canvas,b.left,b.bottom-2*p,b.right,b.bottom,palette.outline);
            box(canvas,b.left,b.top,b.left+p,b.bottom,palette.outline);
            box(canvas,b.right-p,b.top,b.right,b.bottom,palette.outline);
            for (float x=b.left+2*p; x<b.right-2*p; x+=4*p) {
                box(canvas,x,b.top+3*p,x+p,b.top+4*p,0xffd8c8aa);
                box(canvas,x+2*p,b.bottom-5*p,x+3*p,b.bottom-4*p,0xff9a876c);
            }
            return;
        }
        float depth=Math.min((pressed?Math.min(2,palette.depth):palette.depth)*p,b.height()*.18f);
        float l=b.left+p,t=b.top+p,r=b.right-p,bottom=b.bottom-p;
        cap(canvas,l,t+p,r,bottom,p,0xff4b392a);
        cap(canvas,l+p,t,r-p,bottom-p,p,palette.shadow);
        int face=pressed?IosTheme.mix(fill,palette.shadow,.09f):fill;
        // Broad lower slope and asymmetric side bevels match the physical cream keycaps.
        float faceBottom=bottom-depth,left=l+2*p,right=r-2*p,top=t+2*p;
        plane(canvas,0xfff6e9cf,l+p,t+4*p,left,top,left,faceBottom,l+p,bottom-2*p);
        plane(canvas,IosTheme.mix(face,palette.shadow,.35f),right,top,r-p,t+4*p,r-p,bottom-2*p,right,faceBottom);
        plane(canvas,IosTheme.mix(face,palette.shadow,.30f),left,faceBottom,right,faceBottom,r-p,bottom-2*p,l+p,bottom-2*p);
        cap(canvas,left,top,right,faceBottom,p,face);
        box(canvas,left+2*p,top,right-2*p,top+p,pressed?0xffcaba9c:0xfffff5df);
        box(canvas,left,top+2*p,left+p,faceBottom-2*p,0xfffff5df);
        // Sparse checker dithering at the chamfer and small reflected corner chips.
        for(float y=faceBottom+p;y<bottom-2*p;y+=2*p)
            for(float x=left+p;x<right-p;x+=2*p)
                box(canvas,x+((int)((y-faceBottom)/p)%4==1?p:0),y,x+p+((int)((y-faceBottom)/p)%4==1?p:0),y+p,0xffb5a184);
        box(canvas,left-p,faceBottom-p,left,faceBottom+p,0xffffefd4);
        box(canvas,left-2*p,faceBottom+p,left-p,faceBottom+2*p,0xffe1d0b1);
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
