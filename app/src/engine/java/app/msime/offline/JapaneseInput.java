package app.msime.offline;

/** 罗马字组字、空格转换与回车确认的状态，不依赖 Android。 */
final class JapaneseInput {
    private boolean converting;
    private String editing = "";
    void sync(String text) {
        if (!editing.equals(text) || text.isEmpty()) converting = false;
        editing = text;
    }
    void reset() { converting = false; editing = ""; }
    int spaceCommand(int count, int firstSource) {
        if (count <= 0 || count == 1 && firstSource == 9) return 11;
        int command = converting ? 102 : 104;
        converting = true;
        return command;
    }
    int enterCommand() { return converting ? 1 : 11; }
    boolean converting() { return converting; }
    static String preedit(String raw, String reading) { return reading.isEmpty() ? raw : reading; }
}
