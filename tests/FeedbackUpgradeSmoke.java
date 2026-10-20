package app.msime.offline;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FeedbackUpgradeSmoke {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        KeyPreviewState state=new KeyPreviewState();Object q=new Object(),w=new Object();
        long first=state.begin(q),second=state.begin(w);
        check(!state.end(q,first)&&state.owns(w),"先抬起的手指不能关闭后一键气泡");
        long third=state.begin(w);
        check(!state.end(w,second)&&state.owns(w),"同一键重新按下不能被旧延迟任务关闭");
        check(state.end(w,third)&&!state.owns(w),"当前气泡正常收尾");
        state.begin(q);state.clear();check(!state.end(q,first),"会话关闭取消旧气泡");
        check(KeyPreviewState.left(2,56,0,360)==0,"屏幕左边缘气泡可见");
        check(KeyPreviewState.left(358,56,0,360)==304,"屏幕右边缘气泡可见");
        check(KeyPreviewState.left(110,56,80,30)==80,"横屏窄窗口不产生负边距");
        check(KeyPreviewState.eligible("q",false,true)&&KeyPreviewState.eligible("7",false,true),"字母与数字允许预览");
        check(!KeyPreviewState.eligible("q",true,true)&&!KeyPreviewState.eligible("q",false,false),"私密字段与设置关闭不预览");
        check(!KeyPreviewState.eligible("Tab",false,true)&&!KeyPreviewState.eligible(" ",false,true),"控制与空格不显示误导气泡");
        check(KeyboardText.CANDIDATE_SP==22,"候选字号增大且独立于键帽缩放");
        check(SkinPalette.NAMES[SkinPalette.index("retro")].equals("S4U"),"皮肤选择展示S4U名称");
        check(SkinPalette.of("retro",true).background==SkinPalette.of("retro",false).background,"S4U固定米色风格");
        System.out.println("FeedbackUpgradeSmoke: fast preview ownership, edge positions, private fields, S4U, larger candidates PASS");
    }
}
