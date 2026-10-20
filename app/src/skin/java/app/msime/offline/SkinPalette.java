package app.msime.offline;

/** Shared native skin tokens; no bitmap copies of third-party keys or logos. */
public final class SkinPalette {
    public static final int RETRO_LCD=0xff304237,RETRO_LCD_INK=0xffdde8b9;
    public static final String[] IDS = {"system", "cloud", "mono", "metal", "line", "color", "ios", "retro", "lavender"};
    public static final String[] NAMES = {"跟随系统", "浮云", "简约黑白", "mac 拟物", "简约线条", "简约 color", "iOS", "S4U", "淡紫像素"};
    public final int background, foreground, key, special, accent, selectedInk, shadow, outline;
    public final int radius, depth;
    public final String id;
    public final boolean dark;
    private SkinPalette(String id, boolean dark, int bg, int fg, int key, int special, int accent,
                        int selected, int shadow, int outline, int radius, int depth) {
        this.id=id; this.dark=dark; background=bg; foreground=fg; this.key=key; this.special=special;
        this.accent=accent; selectedInk=selected; this.shadow=shadow; this.outline=outline;
        this.radius=radius; this.depth=depth;
    }
    public static int index(String id) {
        for (int i=0;i<IDS.length;i++) if (IDS[i].equals(id)) return i;
        return 0;
    }
    public int keyInk() {return id.equals("mono")||id.equals("metal")?0xfff5f5f7:foreground;}
    public int functionInk() {return id.equals("color")?0xfff7f7f7:keyInk();}
    public int candidateInk() {return id.equals("retro")?RETRO_LCD_INK:foreground;}
    public int candidateFill() {return id.equals("retro")?RETRO_LCD:background;}
    public int faceInk(boolean selected,boolean shift,boolean function) {
        return selected ? shift?keyInk():selectedInk : function?functionInk():keyInk();
    }
    public int faceFill(boolean selected,boolean shift,boolean function) {
        return selected ? shift?key:accent : function?special:key;
    }
    SkinPalette configured(java.util.Map<String,Integer> colors,int radius,int depth){
        return new SkinPalette(id,dark,colors.getOrDefault("background",background),colors.getOrDefault("foreground",foreground),
            colors.getOrDefault("key",key),colors.getOrDefault("special",special),colors.getOrDefault("accent",accent),
            colors.getOrDefault("selectedInk",selectedInk),colors.getOrDefault("shadow",shadow),colors.getOrDefault("outline",outline),radius,depth);
    }
    public static SkinPalette of(String requested, boolean night) {
        String id=IDS[index(requested)];
        return switch(id) {
            case "lavender" -> new SkinPalette(id,false,0xffe5dcf6,0xff30206f,0xfff0eafa,0xffe7daf7,0xff5540ac,0xffffffff,0xffac91d2,0xff5142ce,1,2);
            case "retro" -> new SkinPalette(id,false,0xffc4b499,0xff49372a,0xffeee2c9,0xffd9c6a8,0xff45594a,0xffe6edc3,0xff826e56,0xff705a43,3,4);
            case "cloud" -> new SkinPalette(id,false,0xffe7e8ea,0xff41464c,0xfffafbfc,0xffeef0f2,0xff485c70,0xffffffff,0xffaeb3b8,0xfff8fafb,12,4);
            case "mono" -> new SkinPalette(id,false,0xffdadde2,0xff25262a,0xff101113,0xff191b1f,0xffc62738,0xffffffff,0xff73767a,0xff3b3d40,7,2);
            case "metal" -> new SkinPalette(id,false,0xff9da3a8,0xff171c25,0xff171c25,0xff10151c,0xff08729a,0xffffffff,0xff444a50,0xff606c7d,3,3);
            case "line" -> new SkinPalette(id,false,0xfffdfdfd,0xff151515,0xffffffff,0xfff3f3f3,0xff202020,0xffffffff,0xff151515,0xff151515,6,3);
            case "color" -> night
                ? new SkinPalette(id,true,0xff222222,0xfff7f7f7,0xff303030,0xff181818,0xffb40953,0xffffffff,0xff050505,0xff080808,14,0)
                : new SkinPalette(id,false,0xffe5e5e5,0xff141414,0xffeeeeee,0xff181818,0xffb40953,0xffffffff,0xff080808,0xff080808,14,0);
            case "ios" -> night
                ? new SkinPalette(id,true,0xff2b2b2f,0xfffafafa,0xff626267,0xff444449,0xff1267b5,0xffffffff,0xff151518,0xff444449,5,1)
                : new SkinPalette(id,false,0xffd1d3d9,0xff17171a,0xffffffff,0xffadb2bd,0xff1465b4,0xffffffff,0xff777982,0xffe6e6e8,5,1);
            default -> night
                ? new SkinPalette("system",true,0xff1b1b1d,0xfff8f8fa,0xff303032,0xff48323f,0xffc46698,0xff101015,0xff0a0a0b,0xff655060,14,2)
                : new SkinPalette("system",false,0xffecedef,0xff19191c,0xfff9f1f5,0xfff0cbdc,0xff934566,0xffffffff,0xffbcbfc5,0xffffffff,14,2);
        };
    }
}
