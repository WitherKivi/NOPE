package app.msime.offline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.PopupWindow;

/** 与按键同一皮肤的即时字母气泡；窗口不接管触摸，不延迟文字上屏。 */
final class KeyPreview {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final KeyPreviewState state=new KeyPreviewState();
    private PopupWindow popup;
    private Runnable pendingHide;
    KeyPreview(Context context){this.context=context;}
    void show(KeyButton key,View keyboard,String text,IosTheme theme) {
        if(keyboard==null||keyboard.getWidth()<=0||!key.isAttachedToWindow())return;
        dismiss();state.begin(key);
        float density=key.getResources().getDisplayMetrics().density;
        int width=Math.round(56*density),height=Math.round(76*density);
        int[] anchor=new int[2],bounds=new int[2];key.getLocationOnScreen(anchor);keyboard.getLocationOnScreen(bounds);
        width=Math.min(width,keyboard.getWidth());
        int center=anchor[0]+key.getWidth()/2;
        int left=KeyPreviewState.left(center,width,bounds[0],keyboard.getWidth());
        android.graphics.Rect visible=new android.graphics.Rect();keyboard.getWindowVisibleDisplayFrame(visible);
        int top=Math.max(visible.top,anchor[1]-height+Math.round(8*density));
        Bubble bubble=new Bubble(context,text,theme,center-left);
        popup=new PopupWindow(bubble,width,height,false);popup.setTouchable(false);popup.setClippingEnabled(false);
        popup.setAnimationStyle(0);popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        try {popup.showAsDropDown(key,left-anchor[0],top-anchor[1]-key.getHeight());}
        catch(android.view.WindowManager.BadTokenException error){dismiss();}
    }
    void release(KeyButton key,boolean immediate) {
        if(!state.owns(key))return;
        if(pendingHide!=null)main.removeCallbacks(pendingHide);
        long ticket=state.generation();
        pendingHide=()->{if(state.end(key,ticket))close();};
        if(immediate)pendingHide.run();else main.postDelayed(pendingHide,100);
    }
    private void close(){if(popup!=null){popup.dismiss();popup=null;}}
    void dismiss(){if(pendingHide!=null)main.removeCallbacks(pendingHide);pendingHide=null;state.clear();close();}
    private static final class Bubble extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shape=new Path();
        private final String text;
        private final IosTheme theme;
        private final float tail;
        private final RetroDrawable cap;
        private final PixelGlassDrawable pixelGlass;
        Bubble(Context context,String text,IosTheme theme,float tail){
            super(context);this.text=text;this.theme=theme;this.tail=tail;
            cap=theme.palette.id.equals("retro")?new RetroDrawable(theme.palette,RetroDrawable.Surface.KEY,false,theme.key,getResources().getDisplayMetrics().density):null;
            pixelGlass=theme.palette.id.equals("lavender")?new PixelGlassDrawable(PixelGlassDrawable.Surface.KEY,theme.key,false,getResources().getDisplayMetrics().density):null;
        }
        @Override protected void onDraw(Canvas canvas){
            float d=getResources().getDisplayMetrics().density,w=getWidth(),body=getHeight()-12*d;
            float center=Math.max(8*d,Math.min(w-8*d,tail));boolean retro=theme.palette.id.equals("retro");
            boolean pixel=retro||pixelGlass!=null;
            paint.setAntiAlias(!pixel);paint.setColor(theme.palette.outline);
            if(pixel){
                canvas.drawRect(center-7*d,body-2*d,center+7*d,body+4*d,paint);
                canvas.drawRect(center-4*d,body+4*d,center+4*d,body+10*d,paint);
                if(retro){cap.setBounds(0,0,getWidth(),Math.round(body));cap.draw(canvas);}
                else{pixelGlass.setBounds(0,0,getWidth(),Math.round(body));pixelGlass.draw(canvas);}
            }else{
                paint.setColor(theme.shadow);canvas.drawRoundRect(0,d,w,body+d,9*d,9*d,paint);
                paint.setColor(theme.key);canvas.drawRoundRect(0,0,w,body,9*d,9*d,paint);
                shape.reset();shape.moveTo(center-8*d,body-d);shape.lineTo(center,body+10*d);shape.lineTo(center+8*d,body-d);shape.close();canvas.drawPath(shape,paint);
            }
            if(pixel&&PixelGlyphs.supports(text))PixelGlyphs.draw(canvas,text,w/2,body/2-2*d,w-18*d,34*getResources().getDisplayMetrics().scaledDensity,theme.keyInk);
            else{paint.setColor(theme.keyInk);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(android.graphics.Typeface.DEFAULT);
                paint.setTextSize(Math.min(34*getResources().getDisplayMetrics().scaledDensity,body*.7f));
                paint.setTextSize(paint.getTextSize()*Math.min(1,(w-16*d)/Math.max(1,paint.measureText(text))));Paint.FontMetrics fm=paint.getFontMetrics();
                canvas.drawText(text,w/2,body/2-(fm.ascent+fm.descent)/2,paint);}
        }
    }
}
