package app.msime.offline;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Emoji draw independently of Button's single-line scrolling/ellipsizing state on recycled cells. */
final class EmojiCell extends View {
    private String text="";
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    EmojiCell(Context context) { super(context); setClickable(true); setFocusable(true); }
    void bind(EmojiCatalog.Item item, Runnable action) {
        text=item.text(); setContentDescription("输入表情："+item.description());
        setOnClickListener(v->action.run()); invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setTypeface(Typeface.DEFAULT); paint.setTextSize(27*getResources().getDisplayMetrics().scaledDensity);
        float width=paint.measureText(text), available=getWidth()-8*getResources().getDisplayMetrics().density;
        if (width>available && width>0) paint.setTextSize(paint.getTextSize()*available/width);
        Paint.FontMetrics f=paint.getFontMetrics(); float height=f.descent-f.ascent;
        if (height>getHeight()-4) { paint.setTextSize(paint.getTextSize()*(getHeight()-4)/height); f=paint.getFontMetrics(); }
        paint.setColor(Color.BLACK);
        canvas.drawText(text,(getWidth()-paint.measureText(text))/2,(getHeight()-f.ascent-f.descent)/2,paint);
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    @Override public CharSequence getAccessibilityClassName() { return "android.widget.Button"; }
}
