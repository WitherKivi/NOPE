package app.msime.offline;

/** 只处理卡片预览，不改变保存内容或粘贴内容；限制排版成本并保留可读下限。 */
final class ClipboardText {
    static final int MIN_SP=12,CARD_MAX_SP=15,RECENT_MAX_SP=16,MAX_CODE_POINTS=384;
    private static final java.util.regex.Pattern SPACE=java.util.regex.Pattern.compile("[\\r\\n\\t ]+");
    static String preview(String text){
        String flat=SPACE.matcher(text).replaceAll(" ").trim();
        if(flat.codePointCount(0,flat.length())<=MAX_CODE_POINTS)return flat;
        return flat.substring(0,flat.offsetByCodePoints(0,MAX_CODE_POINTS)).trim()+"…";
    }
    static int cardHeight(float density,float scaledDensity){
        return Math.max(Math.round(76*density),(int)Math.ceil(MIN_SP*scaledDensity*2.8f+20*density));
    }
    private ClipboardText(){ }
}
