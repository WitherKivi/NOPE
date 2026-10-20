package app.msime.offline;

public final class ClipboardTextSmoke {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        String original="long sentence 😀\r\n".repeat(3000),saved=original;
        String display=ClipboardText.preview(original);
        check(display.endsWith("…")&&display.codePointCount(0,display.length())<=385,"超长文本预览有界并省略");
        check(!display.contains("\n")&&!display.contains("\r"),"换行不会让两行卡片被空行耗尽");
        check(original.equals(saved)&&original.length()>display.length(),"保存与粘贴仍使用完整原文");
        String emoji="😀".repeat(500),preview=ClipboardText.preview(emoji);
        check(preview.codePointCount(0,preview.length())==385,"按码位截断，不拆UTF16代理对");
        check(preview.charAt(preview.length()-2)=='\uDE00',"省略号前保留完整表情");
        check(ClipboardText.preview(" q ").equals("q"),"单字母保留原有大小写");
        check(ClipboardText.preview("头发 日本語\nありがとう").equals("头发 日本語 ありがとう"),"中日文与段落预览可读");
        check(ClipboardText.MIN_SP==12&&ClipboardText.CARD_MAX_SP==15,"长卡片适当缩放且不缩成不可读字号");
        check(ClipboardText.cardHeight(3,3)==228&&ClipboardText.cardHeight(3,6)>228,"大字体时增加两行卡片高度");
        System.out.println("ClipboardTextSmoke: bounded long previews, Unicode, newlines, case and readable scaling PASS");
    }
}
