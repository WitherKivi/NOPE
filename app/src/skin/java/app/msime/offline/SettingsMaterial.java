package app.msime.offline;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.Random;

/** 设置页固定明暗雾面与低对比花纹；不依赖应用背后的内容。 */
final class SettingsMaterial extends Drawable {
    final boolean dark;
    private final boolean panel;
    private final float d;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final BitmapShader grain;
    private static Bitmap sharedGrain;
    SettingsMaterial(boolean dark,boolean panel,float density){
        this.dark=dark;this.panel=panel;d=density;
        grain=new BitmapShader(texture(),Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);
    }
    private static synchronized Bitmap texture(){
        if(sharedGrain==null){sharedGrain=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);Random random=new Random(41);
            int[] pixels=new int[4096];for(int i=0;i<pixels.length;i++)pixels[i]=((random.nextInt(8)+2)<<24)|(random.nextBoolean()?0xffffff:0);
            sharedGrain.setPixels(pixels,0,64,0,0,64,64);}
        return sharedGrain;
    }
    @Override public void draw(Canvas canvas){
        RectF area=new RectF(getBounds());if(area.isEmpty())return;paint.setAlpha(255);
        float radius=panel?20*d:0;
        int top=dark?(panel?0xff48484f:0xff37373e):(panel?0xfffcfcfa:0xfffaf9f5);
        int bottom=dark?(panel?0xff35353d:0xff25252c):(panel?0xffe8e8ed:0xffe3e3e9);
        paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,area.top,area.right,area.bottom,top,bottom,Shader.TileMode.CLAMP));
        canvas.drawRoundRect(area,radius,radius,paint);paint.setShader(null);
        int saved=canvas.save();Path mask=new Path();mask.addRoundRect(area,radius,radius,Path.Direction.CW);canvas.clipPath(mask);
        paint.setShader(grain);canvas.drawRect(area,paint);paint.setShader(null);
        if(!panel){
            paint.setColor(dark?0x109d98ad:0x18998fae);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);
            for(int i=-1;i<8;i++){
                Path ribbon=new Path();float y=area.top+i*160*d;
                ribbon.moveTo(area.left-40*d,y);ribbon.cubicTo(area.width()*.42f,y-90*d,area.width()*.58f,y+180*d,area.right+50*d,y+70*d);canvas.drawPath(ribbon,paint);
            }
        }
        canvas.restoreToCount(saved);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);
        paint.setColor(dark?0x30ffffff:0xd8ffffff);area.inset(d/2,d/2);canvas.drawRoundRect(area,radius,radius,paint);paint.setStyle(Paint.Style.FILL);
    }
    @Override public void setAlpha(int alpha){ }
    @Override public void setColorFilter(ColorFilter filter){ }
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
