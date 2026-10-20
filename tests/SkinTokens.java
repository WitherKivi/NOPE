package app.msime.offline;
public final class SkinTokens {
    public static void main(String[] args) {
        System.out.print("[");boolean first=true;
        for(String id:SkinPalette.IDS) for(boolean night:new boolean[]{false,true}) {
            if(!id.equals("system")&&!id.equals("color")&&!id.equals("ios")&&night) continue;
            SkinPalette p=SkinPalette.of(id,night);if(!first)System.out.print(",");first=false;
            int ink=id.equals("mono")||id.equals("metal")?0xfff5f5f7:p.foreground;
            System.out.printf("{\"id\":\"%s\",\"name\":\"%s\",\"dark\":%s,\"background\":%d,\"foreground\":%d,\"key\":%d,\"special\":%d,\"accent\":%d,\"selectedInk\":%d,\"shadow\":%d,\"outline\":%d,\"keyInk\":%d,\"functionInk\":%d,\"radius\":%d,\"depth\":%d}",
                id,SkinPalette.NAMES[SkinPalette.index(id)],night,p.background,p.foreground,p.key,p.special,p.accent,p.selectedInk,p.shadow,p.outline,ink,id.equals("color")?0xfff7f7f7:ink,p.radius,p.depth);
        }
        System.out.print("]");
    }
}
