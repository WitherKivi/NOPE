package app.msime.offline;

/** 仅接收明确的纯文本，不读取 URI，也不保存敏感字段/系统敏感剪贴板。 */
final class ClipboardPolicy {
    static final int MAX_TEXT = 65536;
    private ClipboardPolicy() { }
    static boolean accept(String text, boolean sensitive, boolean privateField, boolean enabled) {
        return enabled && !sensitive && !privateField && text != null && !text.isBlank() && text.length() <= MAX_TEXT;
    }
}
