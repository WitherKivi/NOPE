package app.msime.offline;

/** JNI 结果写入顺序；拒绝任一步就让宿主关闭会话，不重试已上屏部分。 */
final class EditorTransition {
    interface Sink {
        boolean commit(String text);
        boolean compose(String text);
        boolean finish();
    }
    private EditorTransition() { }
    static void write(Sink sink, String commit, String composing, boolean hadComposition) {
        if (commit != null && !sink.commit(commit)) throw new IllegalStateException("文字上屏被拒绝");
        // commit 已移除原组字区，不能再用空组字覆盖当前选区。
        if ((!composing.isEmpty() || commit == null && hadComposition) && !sink.compose(composing))
            throw new IllegalStateException("组字更新被拒绝");
        // 空会话 focus 不写编辑器，避免制造无意义的迟到 finish 回报。
        if (composing.isEmpty() && (hadComposition || commit != null) && !sink.finish()) throw new IllegalStateException("组字结束被拒绝");
    }
}
