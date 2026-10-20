package app.msime.android;

/** 终端按键的成对派发；目标拒绝或抛错时也释放已经尝试按下的键。 */
public final class CodingInput {
    public interface Sink {
        boolean key(boolean down, int code, int meta);
    }

    private CodingInput() { }

    public static boolean send(Sink sink, int code, int meta) {
        int[] modifiers = {CodingKeyMap.CTRL, CodingKeyMap.ALT, CodingKeyMap.SHIFT};
        int[] codes = {113, 57, 59};
        int held = 0;
        boolean attempted = false;
        boolean accepted = true;
        try {
            for (int i = 0; i < modifiers.length; i++) {
                if ((meta & modifiers[i]) == 0) continue;
                held |= modifiers[i];
                accepted &= sink.key(true, codes[i], held);
                if (!accepted) return false;
            }
            attempted = true;
            accepted = sink.key(true, code, meta);
        } catch (RuntimeException error) {
            accepted = false;
        } finally {
            if (attempted) {
                try { accepted &= sink.key(false, code, meta); }
                catch (RuntimeException error) { accepted = false; }
            }
            for (int i = modifiers.length - 1; i >= 0; i--) {
                if ((held & modifiers[i]) == 0) continue;
                held &= ~modifiers[i];
                try { accepted &= sink.key(false, codes[i], held); }
                catch (RuntimeException error) { accepted = false; }
            }
        }
        return accepted;
    }
}
