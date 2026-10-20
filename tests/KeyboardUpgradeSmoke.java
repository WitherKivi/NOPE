package app.msime.offline;

public final class KeyboardUpgradeSmoke {
    private static void require(boolean condition, String reason) { if (!condition) throw new AssertionError(reason); }
    public static void main(String[] arguments) {
        require(EditingSwipe.action("Z").equals("撤回") && EditingSwipe.action("a").equals("全选"), "大小写编辑快捷手势");
        require(EditingSwipe.action("x").equals("剪切") && EditingSwipe.action("c").equals("复制") && EditingSwipe.action("v").equals("粘贴"), "剪切复制粘贴映射");
        require(EditingSwipe.action("q").isEmpty(), "其他字母不执行编辑");
        require(EditingSwipe.ready(2, 40, 36), "下滑足够距离松手触发");
        require(!EditingSwipe.ready(2, 20, 36) && !EditingSwipe.ready(50, 45, 36) && !EditingSwipe.ready(2, -40, 36), "滑回、横移、上滑不触发编辑");
        require(SymbolLayouts.variants(",")[0].equals(".") && SymbolLayouts.variants("，")[0].equals("。"), "逗号上滑选对应句号");
        DeleteSwipe swipe = new DeleteSwipe();
        swipe.start(100, 100);
        require(!swipe.moved(101, 101, 9), "轻微抖动仍为点按");
        require(swipe.progress(100, 47, 52) == 1, "上滑越过阈值");
        require(swipe.progress(100, 80, 52) < 1, "抬手前可以退回取消");
        require(swipe.progress(210, 10, 52) == 0, "横向移出不清空");
        swipe.cancel(); require(swipe.progress(100, 0, 52) == 0 && !swipe.active(), "取消/多指后抬手也不执行清空或点按");
        require(KeyboardMetrics.height(0, 360, 800) == 259, "参考宽高比例");
        require(KeyboardMetrics.height(400, 360, 800) == 400, "自定义高度生效");
        require(KeyboardMetrics.height(400, 800, 360) == 234, "横屏有高度限制");
        int height = KeyboardMetrics.height(0, 393, 820);
        require(KeyboardMetrics.row(height, false) * 4 + 56 <= height, "普通键盘无额外上方按钮高度");
        int codingHeight = KeyboardMetrics.codeHeight(0, height, 820);
        require(codingHeight == height + 120, "Code 默认增加控制和方向键空间");
        require(KeyboardMetrics.row(codingHeight, true) >= KeyboardMetrics.row(height, false), "Code 主键行不因控制键被挤小");
        require(KeyboardMetrics.row(codingHeight, true) * 4 + KeyboardMetrics.auxiliary(codingHeight) + 56 <= codingHeight, "Code 独立高度预算");
        require(KeyboardMetrics.codeHeight(520, 234, 360) == 295, "横屏 Code 限制高度");
        ClipboardSuggestion suggestion = new ClipboardSuggestion(-1);
        suggestion.offer(100, "new copy"); require("new copy".equals(suggestion.text()), "显示当前复制而非数据库旧项");
        require(suggestion.dismiss() == 100 && suggestion.text() == null, "返回或成功粘贴隐藏提示");
        suggestion.offer(100, "new copy"); require(suggestion.text() == null, "重读同一系统复制不恢复提示");
        suggestion = new ClipboardSuggestion(100); suggestion.offer(100, "new copy"); require(suggestion.text() == null, "服务重启后保持隐藏");
        suggestion.offer(101, "new copy"); require(suggestion.text() != null, "重新复制相同文字恢复新提示");
        suggestion.clear(); require(suggestion.text() == null, "敏感复制不显示或退回旧记录");
        require(HapticStrength.amplitude(0) == 0 && HapticStrength.duration(0) == 0, "震动零强度关闭");
        require(HapticStrength.amplitude(100) == 255 && HapticStrength.duration(100) == 28, "最大反馈范围");
        require(HapticStrength.amplitude(150) == 255 && HapticStrength.amplitude(-20) == 0, "震动配置夹紧");
        require(HapticStrength.amplitude(20) < HapticStrength.amplitude(70), "强度单调变化");
        CandidatePageSwipe page = new CandidatePageSwipe();
        page.start(100, 100, true, true); require(page.direction(60, 100, 36) == 1, "候选左滑下一页");
        require(page.direction(140, 100, 36) == -1, "候选右滑上一页");
        require(page.direction(99, 100, 36) == 0 && page.direction(60, 145, 36) == 0, "滑回和竖滑不翻页");
        page.cancel(); require(page.direction(60, 100, 36) == 0, "多指或系统取消不翻页");
        page.start(100, 100, false, false); require(page.direction(60, 100, 36) == 0, "页内滚动不提前翻页");
        require(ClipboardPolicy.accept("git status", false, false, true), "保存普通文本");
        require(ClipboardPolicy.accept("a\nb", false, false, true), "保留多行原文");
        require(!ClipboardPolicy.accept("secret", true, false, true), "系统敏感剪贴板");
        require(!ClipboardPolicy.accept("secret", false, true, true), "私密字段");
        require(!ClipboardPolicy.accept("text", false, false, false), "关闭记录");
        require(!ClipboardPolicy.accept(null, false, false, true), "没有纯文本");
        require(!ClipboardPolicy.accept("x".repeat(65537), false, false, true), "过长文本不截断保存");
        require(SymbolLayouts.page(false)[0].length == 10 && SymbolLayouts.page(false)[2].length == 7, "两页符号保持十列基准和功能键留位");
        require(SymbolLayouts.hint("Q").equals("1") && SymbolLayouts.hint("p").equals("0"), "大小写字母提示对应数字");
        String[] brackets = SymbolLayouts.variants("(");
        require(brackets[SymbolLayouts.initial("(", brackets, false)].equals("("), "长按保持默认字符");
        require(brackets[SymbolLayouts.initial("(", brackets, true)].equals("【"), "上滑默认选择变体");
        StringBuilder all = new StringBuilder(" abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        all.append(','); for (String variant : SymbolLayouts.variants(",")) all.append(variant);
        for (boolean extra : new boolean[]{false, true}) for (String[] row : SymbolLayouts.page(extra)) for (String text : row) {
            all.append(text); for (String variant : SymbolLayouts.variants(text)) all.append(variant);
        }
        for (char c = 32; c <= 126; c++) require(all.indexOf(String.valueOf(c)) >= 0, "可达 ASCII: " + c);
        require(VariantSelection.hit(78, 32, 3, 7, 48, 8, 24) == 1, "候选横向命中");
        require(VariantSelection.hit(32, 78, 3, 7, 48, 8, 24) == 3, "货币二行命中");
        require(VariantSelection.hit(130, 130, 3, 7, 48, 8, 24) == -1, "空网格不误选");
        require(VariantSelection.hit(-40, 20, 3, 7, 48, 8, 24) == -1, "超出容差取消");
        require(VariantSelection.left(10, 3, 5, 48, 8, 4, 356) == 4, "左边缘夹紧");
        require(VariantSelection.left(355, 0, 5, 48, 8, 4, 356) == 100, "右边缘夹紧");
        require(VariantSelection.hit(VariantSelection.trackX(355, 355, 0, 2, 48, 8), 32, 2, 2, 48, 8, 24) == 0,
            "右侧浮窗夹紧后保持上滑默认候选，避免误选字母");
        require(VariantSelection.hit(VariantSelection.trackX(20, 20, 1, 2, 48, 8), 32, 2, 2, 48, 8, 24) == 1,
            "左侧浮窗夹紧后保持长按默认候选");
        require(VariantSelection.hit(VariantSelection.trackX(307, 355, 1, 2, 48, 8), 32, 2, 2, 48, 8, 24) == 0,
            "拖动保留抓取偏移并选择相邻项");
        System.out.println("KeyboardUpgradeSmoke: height, cancelable swipe, variants, ASCII coverage and clipboard privacy PASS");
    }
}
