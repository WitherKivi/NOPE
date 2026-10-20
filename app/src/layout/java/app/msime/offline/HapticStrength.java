package app.msime.offline;

final class HapticStrength {
    private HapticStrength() { }
    static int percent(int value) { return Math.max(0, Math.min(100, value)); }
    static int amplitude(int value) { int p = percent(value); return p == 0 ? 0 : Math.max(1, Math.round(p * 2.55f)); }
    static int duration(int value) { int p = percent(value); return p == 0 ? 0 : 6 + Math.round(p * .22f); }
}
