package app.msime.offline;

public final class JapaneseInputSmoke {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] arguments) {
        JapaneseInput input = new JapaneseInput();
        input.sync("nihon");
        check(input.enterCommand() == 11, "未转换时回车提交读音，不能提交 nihon 原文");
        check(input.spaceCommand(9, 0) == 104, "首次空格选中首个转换候选而不提交");
        input.sync("nihon");
        check(input.enterCommand() == 1, "转换时回车提交当前高亮候选");
        check(input.spaceCommand(9, 0) == 102, "再次空格移到下一候选");
        input.sync("nihongo");
        check(!input.converting() && input.enterCommand() == 11, "继续编辑后返回假名确认模式");
        check(input.spaceCommand(1, 9) == 11 && !input.converting(), "仅有原文兜底时空格直接结束组字");
        check(input.spaceCommand(0, -1) == 11, "无候选不能进入转换状态");
        input.spaceCommand(2, 0); input.sync("");
        check(!input.converting(), "选词或切换会话后清空转换状态");
        input.sync("tokyo"); input.spaceCommand(3, 0); input.reset();
        check(input.enterCommand() == 11, "取消转换或销毁会话后不沿用上次高亮");
        check(JapaneseInput.preedit("nihon", "にほん").equals("にほん"), "组字区显示假名读音");
        check(JapaneseInput.preedit("k", "").equals("k"), "尚未成音的罗马字仍可见");
        check(JapaneseInput.preedit("kya", "きゃ").length() == 2, "组字长度使用显示假名的UTF16长度");
        System.out.println("JapaneseInputSmoke: kana preedit, Space conversion, Enter confirmation, cancel and session reset PASS");
    }
}
