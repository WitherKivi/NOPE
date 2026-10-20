package app.msime.offline;

/** 延迟读取的会话票据；收起、编辑或换输入框后不接受旧结果。 */
final class CandidateRequest {
    private long ticket,session,editor,generation;
    private boolean pending;
    boolean matches(long session,long editor,long generation){return pending&&this.session==session&&this.editor==editor&&this.generation==generation;}
    long begin(long session,long editor,long generation){this.session=session;this.editor=editor;this.generation=generation;pending=true;return ++ticket;}
    boolean accepts(long token,long session,long editor,long generation){return token==ticket&&matches(session,editor,generation);}
    void cancel(){pending=false;ticket++;}
}
