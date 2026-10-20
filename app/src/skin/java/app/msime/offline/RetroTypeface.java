package app.msime.offline;

import android.content.Context;
import android.graphics.Typeface;

/** S4U 的小型自有像素字体；TextView负责排版与CJK系统回退。 */
final class RetroTypeface {
    private static Typeface face;
    static Typeface get(Context context){
        if(face==null)face=Typeface.createFromAsset(context.getAssets(),"skin/s4u-keycaps.ttf");
        return face;
    }
    private RetroTypeface(){ }
}
