package app.msime.offline;

/** 预览按指针所属键和代次收尾，旧键松开不能隐藏快速连按的新气泡。 */
final class KeyPreviewState {
    private Object owner;
    private long generation;
    long begin(Object key) { owner=key; return ++generation; }
    long generation() { return generation; }
    boolean owns(Object key) { return owner==key; }
    boolean end(Object key,long ticket) {
        if(owner!=key||ticket!=generation)return false;
        clear();return true;
    }
    void clear() {owner=null;generation++;}
    static int left(int center,int width,int keyboardLeft,int keyboardWidth) {
        return Math.max(keyboardLeft,Math.min(center-width/2,keyboardLeft+Math.max(0,keyboardWidth-width)));
    }
    static boolean eligible(String label,boolean privateField,boolean enabled) {
        return enabled&&!privateField&&label.length()==1&&label.charAt(0)<128&&Character.isLetterOrDigit(label.charAt(0));
    }
}
