package app.msime.offline;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;

/** iOS-inspired neutral materials, system appearance and a restrained wallpaper tint. */
final class IosTheme {
    final SkinPalette palette;
    final SkinConfig config;
    final int keyInk, functionInk,candidateInk,candidateFill;
    final int background, foreground, key, special, accent, selectedInk, shadow;
    final boolean dark;
    final float density;
    IosTheme(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        SkinPalette base = SkinPalette.of(context.getSharedPreferences("offline", Context.MODE_PRIVATE).getString("skin", "system"),
            (context.getResources().getConfiguration().uiMode & 0x30) == 0x20);
        config=SkinConfig.load(context,base.id,base.dark);
        palette=config.palette(base);
        dark = palette.dark;
        background = palette.background;
        key = palette.key; special = palette.special; foreground = palette.foreground;
        accent = palette.accent; selectedInk = palette.selectedInk; shadow = palette.shadow;
        keyInk = palette.keyInk(); functionInk = palette.functionInk();
        candidateInk=config.keyColor("candidate","ink",palette.candidateInk());candidateFill=config.keyColor("candidate","fill",palette.candidateFill());
    }
    android.graphics.drawable.Drawable material(boolean blur) {
        if (palette.id.equals("lavender")) return new PixelGlassDrawable(PixelGlassDrawable.Surface.FRAME,background,false,density);
        if (palette.id.equals("retro")) return new RetroDrawable(palette, RetroDrawable.Surface.FRAME, false, background, density);
        if(palette.id.equals("metal")) return new MetalBackground();
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(alpha(background, blur && palette.id.equals("system") ? 242 : 255));
        return fill;
    }
    static int alpha(int color, int alpha) { return (color & 0xffffff) | (alpha << 24); }
    static int mix(int a, int b, float weight) {
        return Color.rgb(Math.round(Color.red(a) * (1-weight) + Color.red(b) * weight),
            Math.round(Color.green(a) * (1-weight) + Color.green(b) * weight),
            Math.round(Color.blue(a) * (1-weight) + Color.blue(b) * weight));
    }
}
