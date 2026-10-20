package app.msime.offline;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;

/** NET. 纹理只解码一次；图标保持透明图片资源，字体随包离线可用。 */
final class NetAssets {
    private static NetAssets shared;
    final Bitmap frost;
    final Typeface rounded;
    private final EnumMap<KeyboardIcon.Kind, Bitmap> icons = new EnumMap<>(KeyboardIcon.Kind.class);
    static synchronized NetAssets get(Context context) {
        if(shared==null)shared=new NetAssets(context.getApplicationContext());
        return shared;
    }
    private NetAssets(Context context) {
        frost=bitmap(context,"frosted-background.png",2);
        rounded=Typeface.createFromAsset(context.getAssets(),"skin/net/net-rounded.ttf");
        icons.put(KeyboardIcon.Kind.BRAND,bitmap(context,"brand.png",1));
        icons.put(KeyboardIcon.Kind.CLIPBOARD,bitmap(context,"clipboard.png",1));
        icons.put(KeyboardIcon.Kind.EMOJI,bitmap(context,"emoji.png",1));
        icons.put(KeyboardIcon.Kind.SETTINGS,bitmap(context,"settings.png",1));
    }
    private Bitmap bitmap(Context context,String name,int sample) {
        try(InputStream input=context.getAssets().open("skin/net/"+name)) {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;options.inScaled=false;
            Bitmap result=BitmapFactory.decodeStream(input,null,options);
            if(result==null)throw new IOException("invalid image "+name);
            return result;
        }catch(IOException error){throw new IllegalStateException("NET. asset missing: "+name,error);}
    }
    Drawable icon(Context context,KeyboardIcon.Kind kind) {
        Bitmap source=icons.get(kind);
        if(source==null)return null;
        BitmapDrawable drawable=new BitmapDrawable(context.getResources(),source);
        drawable.setFilterBitmap(true);return drawable;
    }
}
