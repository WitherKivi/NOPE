package app.msime.offline;

public final class ClipboardRetention {
    private ClipboardRetention() { }
    public static long minutes(String input) {
        long minutes=Long.parseLong(input.trim());
        if(minutes<1 || minutes>525600) throw new IllegalArgumentException("1–525600 分钟");
        return minutes*60000;
    }
    public static boolean expired(long copied, long now, long retention, boolean pinned) {
        return !pinned && retention>0 && copied<=now-retention;
    }
}
