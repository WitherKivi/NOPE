package app.msime.offline;

import java.util.List;

public final class QuickLanguagesSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(QuickLanguages.next("CHINESE", null).equals("ENGLISH"), "default skips Japanese");
        check(QuickLanguages.next("ENGLISH", null).equals("CHINESE"), "default returns to Chinese");
        check(QuickLanguages.next("JAPANESE", null).equals("CHINESE"), "picker mode outside cycle returns to first");
        check(QuickLanguages.next("CODING", null).equals("CHINESE"), "Code outside cycle returns to first");
        String custom = "ENGLISH,JAPANESE,CODING";
        check(QuickLanguages.next("ENGLISH", custom).equals("JAPANESE"), "custom Japanese reachable");
        check(QuickLanguages.next("JAPANESE", custom).equals("CODING"), "custom Code reachable");
        check(QuickLanguages.next("CODING", custom).equals("ENGLISH"), "custom cycle wraps");
        check(QuickLanguages.parse("CHINESE,CHINESE, ENGLISH ,INVALID").equals(List.of("CHINESE", "ENGLISH")), "deduplicate and ignore unknown modes");
        for (String invalid : new String[]{"", "JAPANESE", "INVALID,UNKNOWN", "CHINESE,CHINESE"})
            check(QuickLanguages.parse(invalid).equals(List.of("CHINESE", "ENGLISH")), "invalid configuration falls back safely");
        check(QuickLanguages.next("CHINESE", "ENGLISH,JAPANESE").equals("ENGLISH"), "changing settings affects next tap immediately");
        System.out.println("QuickLanguagesSmoke: default Chinese/English, custom cycles, outside modes and invalid preferences PASS");
    }
}
