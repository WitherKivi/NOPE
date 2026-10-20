package app.msime.offline;

import app.msime.android.SelectionEchoTracker;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class NopeUpgradeSmoke {
    private static void check(boolean value,String message) {if(!value) throw new AssertionError(message);}
    private static double luminance(int color) {
        double result=0; double[] weights={.2126,.7152,.0722}; int[] shifts={16,8,0};
        for(int i=0;i<3;i++) {double c=((color>>shifts[i])&255)/255.0;result+=(c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4))*weights[i];}return result;
    }
    private static void contrast(int ink,int bg,String message) {
        double a=luminance(ink),b=luminance(bg);check((Math.max(a,b)+.05)/(Math.min(a,b)+.05)>=4.5,message);
    }
    public static void main(String[] arguments) throws Exception {
        check(KeyboardText.percent(-100)==80 && KeyboardText.percent(900)==140,"文字设置上下限");
        check(KeyboardText.scale(100)==1f && KeyboardText.scale(140)==1.4f,"默认和大字号缩放");
        check(KeyboardText.CANDIDATE_SP==22,"候选使用独立统一字号");
        SelectionEchoTracker echo=new SelectionEchoTracker();echo.reset(0,0);echo.compose(1);echo.expect();
        check(echo.acknowledge(0,0,-1,-1),"initial editor callback must not cancel first letter");
        check(echo.acknowledge(1,1,0,1),"first letter composition survives initial callback");
        echo.reset(0,0); for(int i=1;i<=12;i++) {echo.compose(i);echo.expect();}
        check(echo.acknowledge(0,0,-1,-1),"rapid typing retains initial editor expectation");
        check(echo.acknowledge(12,12,0,12),"coalesced rapid composition callback");
        check(!echo.acknowledge(2,2,-1,-1),"real cursor movement still external");
        check(InputTouchPolicy.tap(12,5,20,18,32,48,18),"quick tap jitter accepted");
        check(!InputTouchPolicy.tap(38,0,48,18,32,48,18),"drag across another key not a tap");
        int left=VariantSelection.left(22,4,5,48,8,4,356);
        check(VariantSelection.hit(30-left,32,5,5,48,8,24)==0,"left-edge clamped popup exposes first choice at its physical position");
        check(VariantSelection.hit(82-left,32,5,5,48,8,24)==1,"absolute popup movement reaches second left choice");
        check(SymbolLayouts.hint(".").equals(",") && SymbolLayouts.hint("。").equals("，"),"period default and comma up-swipe");
        check(ClipboardRetention.minutes("15")==900000,"custom minutes");
        check(ClipboardRetention.expired(0,900000,900000,false),"expiry boundary");
        check(!ClipboardRetention.expired(0,900000,0,false)&&!ClipboardRetention.expired(0,900000,1,true),"never clear and pinned exemption");
        for(String invalid:new String[]{"0","-1","999999999999999999999999","525601"}) {boolean refused=false;try{ClipboardRetention.minutes(invalid);}catch(IllegalArgumentException e){refused=true;}check(refused,"invalid retention rejected");}
        Path dir=Path.of(arguments[0]);
        ChineseConversion conversion=new ChineseConversion(Files.newBufferedReader(dir.resolve("CJK_Compatibility_Ideographs.txt"),StandardCharsets.UTF_8),
            Files.newBufferedReader(dir.resolve("STPhrases.txt"),StandardCharsets.UTF_8), Files.newBufferedReader(dir.resolve("STPhrases_GeneratedFromRegionalPhrases.txt"),StandardCharsets.UTF_8),
            Files.newBufferedReader(dir.resolve("STCharacters.txt"),StandardCharsets.UTF_8));
        check(conversion.convert("头发 开发 干杯 面条").equals("頭髮 開發 乾杯 麪條"),"phrase-first output matches pinned OpenCC s2t dictionary");
        check(conversion.convert("git status 😀 ⿰女尔").equals("git status 😀 ⿰女尔"),"ASCII emoji and IDS preserved");
        for(String id:SkinPalette.IDS) for(boolean dark:new boolean[]{false,true}) {
            SkinPalette p=SkinPalette.of(id,dark);int ink=id.equals("mono")||id.equals("metal")?0xfff5f5f7:p.foreground;
            contrast(p.foreground,p.background,id+" chrome");contrast(ink,p.key,id+" key");
            contrast(p.candidateInk(),p.candidateFill(),id+" candidate");
            contrast(id.equals("color")?0xfff7f7f7:ink,p.special,id+" function");contrast(p.selectedInk,p.accent,id+" selected");
            for(boolean selected:new boolean[]{false,true}) for(boolean shift:new boolean[]{false,true}) for(boolean function:new boolean[]{false,true})
                if(!shift||function) contrast(p.faceInk(selected,shift,function),p.faceFill(selected,shift,function),id+" actual control state");
        }
        System.out.println("NOPE: rapid first-letter echo, tap jitter, physical left selection, punctuation, retention, OpenCC phrases and all skin label contrast PASS");
    }
}
