package app.msime.offline;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Immediate native key feedback. Gradients, bevels and outlines use the skin's own geometry. */
final class SkinKeyDrawable extends Drawable {
    private final IosTheme theme;
    private final int color;
    private final boolean pressed, sheen;
    private final String label;
    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RetroDrawable retro;
    private final PixelGlassDrawable pixelGlass;
    private final SkinPalette material;
    SkinKeyDrawable(IosTheme theme, int color, boolean pressed, boolean sheen, float density, String label,String role) {
        this.theme=theme; this.color=color; this.pressed=pressed; this.sheen=sheen; this.density=density;
        this.label=label;
        material=theme.palette.configured(java.util.Map.of(),theme.config.keySize(role,"radiusDp",theme.palette.radius),theme.config.keySize(role,"depthDp",theme.palette.depth));
        retro=theme.palette.id.equals("retro")?new RetroDrawable(material,RetroDrawable.Surface.KEY,pressed,color,density):null;
        pixelGlass=theme.palette.id.equals("lavender")?new PixelGlassDrawable(PixelGlassDrawable.Surface.KEY,color,pressed,density):null;
    }
    @Override public void draw(Canvas canvas) {
        if(retro!=null){retro.setBounds(getBounds());retro.draw(canvas);return;}
        if(pixelGlass!=null){pixelGlass.setBounds(getBounds());pixelGlass.draw(canvas);return;}
        Rect b=getBounds(); float depth=material.depth*density, radius=material.radius*density;
        RectF cap=new RectF(b.left+density,b.top+density,b.right-density-depth/2,b.bottom-density-depth);
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(theme.shadow);
        RectF shadow=new RectF(cap); shadow.offset(theme.palette.id.equals("line")?depth:0,depth);
        String id=theme.palette.id;
        if (id.equals("cloud")) paint.setShadowLayer(3*density,0,2*density,0x35747e88);
        shape(canvas,shadow,radius,paint); paint.clearShadowLayer();
        int base=pressed?IosTheme.mix(color, theme.accent,.35f):color;
        if (id.equals("cloud") || id.equals("metal") || id.equals("system")) {
            int top=IosTheme.mix(base,Color.WHITE,theme.dark?.08f:.12f);
            int bottom=IosTheme.mix(base,theme.shadow,id.equals("cloud")?.17f:.07f);
            paint.setShader(new LinearGradient(0,cap.top,0,cap.bottom,new int[]{top,base,bottom},new float[]{0,.7f,1},Shader.TileMode.CLAMP));
        } else paint.setColor(base);
        shape(canvas,cap,radius,paint); paint.setShader(null);
        if (id.equals("system") && sheen && !pressed) {
            paint.setShader(new LinearGradient(cap.left,cap.top,cap.right,cap.bottom,
                new int[]{0x18dc8eb7,0x1293aeea,0x30e989b9},null,Shader.TileMode.CLAMP));
            shape(canvas,cap,radius,paint); paint.setShader(null);
        }
        if (!id.equals("ios")) {
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth((id.equals("line")?1.25f:.7f)*density);
            paint.setColor(theme.palette.outline); shape(canvas,cap,radius,paint); paint.setStyle(Paint.Style.FILL);
        }
    }
    private void shape(Canvas canvas,RectF rect,float radius,Paint paint) {
        if(theme.palette.id.equals("mono") && (label.equals("Q")||label.equals("P"))) {
            float outer=22*density; float[] corners=label.equals("Q")
                ?new float[]{outer,outer,radius,radius,radius,radius,radius,radius}
                :new float[]{radius,radius,outer,outer,radius,radius,radius,radius};
            Path path=new Path();path.addRoundRect(rect,corners,Path.Direction.CW);canvas.drawPath(path,paint);
        } else canvas.drawRoundRect(rect,radius,radius,paint);
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
