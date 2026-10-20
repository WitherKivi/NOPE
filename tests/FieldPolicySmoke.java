package app.msime.offline;

/** 覆盖 Android 密码、账号、数值和禁止学习字段的边界。 */
public final class FieldPolicySmoke {
    public static void main(String[] args) {
        for (int type : new int[] {129, 145, 225, 18}) {
            require(FieldPolicy.password(type), "password variation");
            require(FieldPolicy.directField(type), "password bypasses engine");
            require(FieldPolicy.privateField(type, 0), "password does not learn");
        }
        require(!FieldPolicy.password(1), "ordinary text is not a password");
        require(!FieldPolicy.directField(1), "ordinary text can use local pinyin");
        require(!FieldPolicy.directField(0), "raw Termux input can retain Coding");
        for (int type : new int[] {2, 3, 4, 17, 33, 209}) require(FieldPolicy.directField(type), "structured fields use direct input");
        require(FieldPolicy.privateField(1, 0x1000000), "no-personalized-learning is respected");
        require(!FieldPolicy.privateField(1, 0), "ordinary text may learn locally");
        System.out.println("Offline field policy: password, direct fields and private learning passed");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
