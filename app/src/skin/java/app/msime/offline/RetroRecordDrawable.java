package app.msime.offline;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** S4U 独立全彩图标：唱片与小显示屏，不受文字或工具图标染色影响。 */
final class RetroRecordDrawable extends Drawable {
    private final Paint paint=new Paint();
    private int alpha=255;
    private void color(int color){paint.setColor(color);paint.setAlpha(alpha);}
    @Override public void draw(Canvas canvas){
        int size=Math.min(getBounds().width(),getBounds().height());
        if(size<=0)return;
        int save=canvas.save();
        canvas.translate(getBounds().left,getBounds().top);canvas.scale(size/24f,size/24f);
        color(0xff6a5845);canvas.drawRect(1,1,23,23,paint);
        color(0xffeee3cb);canvas.drawRect(2,2,22,22,paint);
        color(0xffa8aba2);canvas.drawCircle(10,11,7,paint);
        color(0xffd9ddd0);canvas.drawArc(3,4,17,18,210,80,true,paint);
        color(0xffb0c7c9);canvas.drawArc(3,4,17,18,40,65,true,paint);
        color(0xffc4adb5);canvas.drawArc(3,4,17,18,125,30,true,paint);
        color(0xfff1e8d6);canvas.drawCircle(10,11,2,paint);
        color(0xff536055);canvas.drawCircle(10,11,1,paint);
        color(0xffece2cf);canvas.drawRect(13,8,23,17,paint);
        color(0xff35483d);canvas.drawRect(14,9,22,15,paint);
        color(0xff998b76);for(int x=5;x<=17;x+=4)canvas.drawRect(x,20,x+2,21,paint);
        canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha){this.alpha=alpha;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
