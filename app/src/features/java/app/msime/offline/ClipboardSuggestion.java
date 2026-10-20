package app.msime.offline;

/** Transient suggestion for the actual system copy, independent of persistent history ordering. */
final class ClipboardSuggestion {
    private long dismissed, timestamp = -1;
    private String content;
    ClipboardSuggestion(long dismissed) { this.dismissed = dismissed; }
    void offer(long timestamp, String content) {
        this.timestamp = timestamp;
        this.content = timestamp >= 0 ? content : null;
    }
    void clear() { timestamp = -1; content = null; }
    void expire(long now,long retention) {if(timestamp>=0 && ClipboardRetention.expired(timestamp,now,retention,false)) clear();}
    String text() { return timestamp != dismissed ? content : null; }
    long dismiss() {
        if (timestamp >= 0) dismissed = timestamp;
        return dismissed;
    }
}
