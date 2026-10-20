package app.msime.offline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

final class HintKey extends KeyButton {
    private final String hint;
    private boolean retroHint;
    void retroHint(boolean value){retroHint=value;}
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    HintKey(Context context, String hint) { super(context); this.hint = hint; }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); paint.setColor(getCurrentTextColor()); paint.setAlpha(155);
        paint.setTypeface(retroHint?RetroTypeface.get(getContext()):android.graphics.Typeface.DEFAULT);
        paint.setTextAlign(retroHint?Paint.Align.CENTER:Paint.Align.RIGHT); paint.setTextSize(10 * getResources().getDisplayMetrics().scaledDensity);
        float density = getResources().getDisplayMetrics().density;
        canvas.drawText(hint, retroHint?getWidth()/2f:getWidth() - 5 * density, 13 * density, paint);
    }
}
