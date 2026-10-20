package app.msime.offline;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.widget.Button;

/** 键帽按设置缩放文字；候选可关闭单键缩字，避免同一栏出现多种字号。 */
class KeyButton extends Button {
    private Runnable feedback;
    private boolean function;
    private String skinRole="other";
    void skinRole(String role){skinRole=role;}
    String skinRole(){return skinRole;}
    private float requestedSize, textScale = 1,skinTextScale=1;
    void skinTextScale(float scale){skinTextScale=scale;fit();}
    private boolean fitting, fitText = true;
    private boolean pressFeedback, retro, pixelLetters;
    private Runnable previewShow, previewHide, previewCancel;
    private float previewX,previewY;
    KeyButton(Context context) { super(context); }
    void textScale(float scale) { textScale = scale; fit(); }
    void fitText(boolean enabled) { fitText = enabled; fit(); }
    @Override public void setTextSize(int unit, float size) {
        if (fitting) { super.setTextSize(unit, size); return; }
        requestedSize = android.util.TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics());
        fit();
    }
    void feedback(Runnable feedback) { this.feedback = feedback; }
    void function(boolean value) { function = value;if(value&&!skinRole.equals("mode")&&!skinRole.equals("space")&&!skinRole.equals("toolbar"))skinRole="function"; }
    boolean function() { return function; }
    void pressFeedback(boolean enabled,boolean retro) {this.pressFeedback=enabled;this.retro=retro;}
    void pixelLetters(boolean enabled){
        pixelLetters=enabled;
        setTypeface(enabled?RetroTypeface.get(getContext()):android.graphics.Typeface.DEFAULT);
    }
    void preview(Runnable show,Runnable hide,Runnable cancel){previewShow=show;previewHide=hide;previewCancel=cancel;}
    private void restore(){animate().cancel();setTranslationY(0);setScaleX(1);setScaleY(1);}
    @Override public void setPressed(boolean pressed){
        boolean changed=pressed!=isPressed();super.setPressed(pressed);
        if(!changed||!pressFeedback)return;
        animate().cancel();
        if(!android.animation.ValueAnimator.areAnimatorsEnabled()){restore();return;}
        if(pressed){setTranslationY((retro?2:1)*getResources().getDisplayMetrics().density);setScaleX(retro?1:.97f);setScaleY(retro?1:.97f);}
        else animate().translationY(0).scaleX(1).scaleY(1).setDuration(100)
            .setInterpolator(new android.view.animation.PathInterpolator(.23f,1,.32f,1)).start();
    }
    @Override protected void onDetachedFromWindow(){restore();if(previewCancel!=null)previewCancel.run();super.onDetachedFromWindow();}
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight) {
        super.onSizeChanged(width,height,oldWidth,oldHeight);
        fit();
    }
    private void fit() {
        if(getAutoSizeTextType()!=AUTO_SIZE_TEXT_TYPE_NONE)return;
        if (requestedSize <= 0) return;
        float requested = requestedSize * textScale * skinTextScale, size = requested;
        android.graphics.Paint measure=new android.graphics.Paint(getPaint());measure.setTextSize(requested);
        if (fitText && getMaxLines()==1 && getWidth() > 0 && getHeight() > 0 && getText().length() > 0) {
            float available = Math.max(1, getWidth() - getPaddingLeft() - getPaddingRight() - 4 * getResources().getDisplayMetrics().density);
            size *= Math.min(1, available / Math.max(1, measure.measureText(getText().toString())));
            measure.setTextSize(size); android.graphics.Paint.FontMetrics metrics = measure.getFontMetrics();
            size *= Math.min(1, (getHeight() - getPaddingTop() - getPaddingBottom() - 4 * getResources().getDisplayMetrics().density)
                / Math.max(1, metrics.descent - metrics.ascent));
        }
        fitting = true;
        try { super.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, Math.max(1, size)); }
        finally { fitting = false; }
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && feedback != null) feedback.run();
        boolean result=super.dispatchTouchEvent(event);
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){previewX=event.getRawX();previewY=event.getRawY();if(result&&isPressed()&&previewShow!=null)previewShow.run();}
        else if(action==MotionEvent.ACTION_UP){if(previewHide!=null)previewHide.run();}
        else if(action==MotionEvent.ACTION_CANCEL||event.getPointerCount()>1||
            action==MotionEvent.ACTION_MOVE&&Math.hypot(event.getRawX()-previewX,event.getRawY()-previewY)>12*getResources().getDisplayMetrics().density){
            if(previewCancel!=null)previewCancel.run();
        }
        return result;
    }
}
