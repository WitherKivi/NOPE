package app.msime.offline;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** NET. 固定雾面纹理、圆润玻璃键帽与烘绘数字，不依赖跨窗口模糊。 */
final class PixelGlassDrawable extends Drawable {
    enum Surface { FRAME, PANEL, KEY }
    private final Surface surface;
    private final IosTheme theme;
    private final int color;
    private final boolean pressed;
    private final float d,radius;
    private final String number;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path clip=new Path();
    PixelGlassDrawable(IosTheme theme,Surface surface,int color,boolean pressed) {
        this(theme,surface,color,pressed,theme.palette.radius,"");
    }
    PixelGlassDrawable(IosTheme theme,Surface surface,int color,boolean pressed,float radiusDp,String label) {
        this.theme=theme;this.surface=surface;this.color=color;this.pressed=pressed;d=theme.density;radius=radiusDp*d;
        int index=label.length()==1?"QWERTYUIOP".indexOf(label.toUpperCase(java.util.Locale.ROOT)):-1;
        number=index<0?"":String.valueOf("1234567890".charAt(index));
    }
    private void stroke(Canvas canvas,RectF bounds,float radius,float width,int color) {
        paint.setShader(null);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);
        canvas.drawRoundRect(bounds,radius,radius,paint);paint.setStyle(Paint.Style.FILL);
    }
    @Override public void draw(Canvas canvas) {
        if(getBounds().isEmpty())return;
        RectF bounds=new RectF(getBounds());paint.setShader(null);paint.setAlpha(255);
        if(surface==Surface.FRAME) {
            // The image contains material only; key geometry remains entirely native.
            canvas.drawBitmap(theme.net.frost,null,bounds,paint);
            bounds.inset(2*d,2*d);stroke(canvas,bounds,5*d,4*d,0x65d778ec);
            bounds.inset(d,d);stroke(canvas,bounds,4*d,d,0xffa56ee3);
            bounds.inset(d,d);stroke(canvas,bounds,3*d,d,0xfffaf5ff);
            return;
        }
        float depth=surface==Surface.KEY?Math.min(2*d,bounds.height()*.04f):0;
        bounds.inset(d,d);
        if(bounds.width()<=0||bounds.height()<=depth)return;
        float rounding=Math.min(radius,Math.min(bounds.width(),bounds.height())*.24f);
        if(depth>0) {
            RectF shadow=new RectF(bounds);shadow.offset(0,depth);
            paint.setColor(0x328878b6);canvas.drawRoundRect(shadow,rounding,rounding,paint);
            shadow.inset(d,d);paint.setColor(0x457e66b8);canvas.drawRoundRect(shadow,rounding,rounding,paint);
        }
        bounds.bottom-=depth;
        int base=pressed?IosTheme.mix(color,0xffa485d1,.18f):color;
        boolean dark=Color.red(base)+Color.green(base)+Color.blue(base)<400;
        int save=canvas.save();clip.reset();clip.addRoundRect(bounds,rounding,rounding,Path.Direction.CW);canvas.clipPath(clip);
        paint.setAlpha(255);paint.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,
            new int[]{IosTheme.mix(base,Color.WHITE,dark?.06f:.24f),base,IosTheme.mix(base,0xffcdb8ed,.15f)},null,Shader.TileMode.CLAMP));
        canvas.drawRect(bounds,paint);paint.setShader(null);
        paint.setAlpha(dark?22:100);canvas.drawBitmap(theme.net.frost,null,bounds,paint);paint.setAlpha(255);
        paint.setShader(new RadialGradient(bounds.left+3*d,bounds.top+3*d,Math.max(1,bounds.height()*.9f),
            new int[]{dark?0x20ffffff:0x85ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));
        canvas.drawRect(bounds,paint);paint.setShader(null);
        if(pressed){paint.setColor(0x16835aaa);canvas.drawRect(bounds,paint);}
        canvas.restoreToCount(save);
        stroke(canvas,bounds,rounding,.75f*d,IosTheme.alpha(theme.palette.outline,180));
        RectF inner=new RectF(bounds);inner.inset(d,d);
        stroke(canvas,inner,Math.max(0,rounding-d),.75f*d,dark?0x509f8ed0:0xdafaf6ff);
        RectF rim=new RectF(bounds);rim.inset(.5f*d,.5f*d);
        paint.setShader(new LinearGradient(rim.left,rim.top,rim.right,rim.bottom,new int[]{0x00db86e3,0x00db86e3,0xc0d897eb},new float[]{0,.6f,1},Shader.TileMode.CLAMP));
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);canvas.drawRoundRect(rim,rounding,rounding,paint);
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);
        if(surface==Surface.KEY&&!number.isEmpty()) {
            // Same material draw as the cap, so font fitting cannot hide the numeric legend.
            paint.setTypeface(theme.net.rounded);paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(Math.min(10*theme.scaledDensity,bounds.height()*.23f));paint.setColor(theme.foreground);
            canvas.drawText(number,bounds.centerX(),bounds.top+2*d-paint.ascent(),paint);
        }
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
