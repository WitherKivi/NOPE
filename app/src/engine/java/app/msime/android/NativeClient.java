package app.msime.android;

import java.nio.charset.StandardCharsets;

/** 从水杉 Android 0.3.1 JNI 接口提取的本地子集，包名用于匹配原生符号。 */
public final class NativeClient {
    static { System.loadLibrary("msime_android"); }
    private NativeClient() {}
    private static String text(byte[] bytes) {
        if (bytes == null) throw new IllegalStateException("本地引擎未返回结果");
        return new String(bytes, StandardCharsets.UTF_8);
    }
    public static String prepareHost(String options) { return text(prepareHostRaw(options.getBytes(StandardCharsets.UTF_8))); }
    public static String defaultPreferences() { return text(defaultPreferencesRaw()); }
    public static String create(String options) { return text(createRaw(options.getBytes(StandardCharsets.UTF_8))); }
    public static String focus(long session, boolean focused) { return text(focusRaw(session, focused)); }
    public static String setPrivateSession(long session, boolean enabled) { return text(setPrivateSessionRaw(session, enabled)); }
    public static String character(long session, int ascii, boolean shift) { return text(characterRaw(session, ascii, shift)); }
    public static String command(long session, int command) { return text(commandRaw(session, command)); }
    public static String select(long session, long generation, long index) { return text(selectRaw(session, generation, index)); }
    /** 仅展开候选时请求完整列表；引擎可能更新 generation，随后读取最新 view。 */
    public static String allCandidates(long session) { return text(allCandidatesRaw(session)); }
    public static String view(long session) { return text(viewRaw(session)); }
    public static String selectAnyCandidate(long session, long generation, long index) { return text(selectAnyCandidateRaw(session, generation, index)); }
    public static String destroy(long session) { return text(destroyRaw(session)); }
    private static native byte[] prepareHostRaw(byte[] options);
    private static native byte[] defaultPreferencesRaw();
    private static native byte[] createRaw(byte[] options);
    private static native byte[] focusRaw(long session, boolean focused);
    private static native byte[] setPrivateSessionRaw(long session, boolean enabled);
    private static native byte[] characterRaw(long session, int ascii, boolean shift);
    private static native byte[] commandRaw(long session, int command);
    private static native byte[] selectRaw(long session, long generation, long index);
    private static native byte[] allCandidatesRaw(long session);
    private static native byte[] viewRaw(long session);
    private static native byte[] selectAnyCandidateRaw(long session, long generation, long index);
    private static native byte[] destroyRaw(long session);
}
