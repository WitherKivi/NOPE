package app.msime.offline;

import app.msime.android.SelectionEchoTracker;
import java.util.ArrayList;
import java.util.List;

public final class EditorTransitionSmoke {
    static final class Fake implements EditorTransition.Sink {
        final List<String> events = new ArrayList<>();
        String reject;
        public boolean commit(String text) { events.add("commit:" + text); return !"commit".equals(reject); }
        public boolean compose(String text) { events.add("compose:" + text); return !"compose".equals(reject); }
        public boolean finish() { events.add("finish"); return !"finish".equals(reject); }
    }
    static void expect(String commit, String composing, boolean old, String... events) {
        Fake sink = new Fake(); EditorTransition.write(sink, commit, composing, old);
        if (!sink.events.equals(List.of(events))) throw new AssertionError(sink.events);
    }
    public static void main(String[] args) {
        expect(null, "ni", false, "compose:ni");
        expect("你好", "", true, "commit:你好", "finish");
        expect("你", "hao", true, "commit:你", "compose:hao");
        expect(null, "", true, "compose:", "finish");
        expect(null, "", false);
        for (String rejected : List.of("commit", "compose", "finish")) {
            Fake sink = new Fake(); sink.reject = rejected;
            try {
                EditorTransition.write(sink, "你", rejected.equals("finish") ? "" : "hao", true);
                throw new AssertionError("必须终止拒绝写入：" + rejected);
            } catch (IllegalStateException expected) {
                int failed = rejected.equals("commit") ? 1 : 2;
                if (sink.events.size() != failed) throw new AssertionError("拒绝后继续写入");
            }
        }
        SelectionEchoTracker tracker = new SelectionEchoTracker();
        tracker.reset(0, 0); tracker.compose(2); tracker.expect();
        if (!tracker.acknowledge(2, 2, 0, 2)) throw new AssertionError("自身组字回声");
        if (!tracker.matchesCurrent(2, 2, 0, 2)) throw new AssertionError("重复现状");
        if (tracker.matchesCurrent(2, 2, -1, -1)) throw new AssertionError("同位置移除组字必须取消");
        if (tracker.acknowledge(2, 2, -1, -1)) throw new AssertionError("外部 finish 不算回声");
        tracker.reset(0, 0); tracker.compose(2); tracker.expect();
        tracker.commit(1); tracker.finish(); tracker.expect();
        tracker.compose(1); tracker.expect();
        if (!tracker.acknowledge(1, 1, -1, -1)) throw new AssertionError("迟到的上屏回声");
        if (!tracker.matchesCurrent(2, 2, 1, 2)) throw new AssertionError("新组字保持有效");
        if (!tracker.acknowledge(2, 2, 1, 2)) throw new AssertionError("新组字回声");
        tracker.reset(0, 0);
        for (int i = 1; i <= 100; i++) { tracker.compose(i); tracker.expect(); }
        if (!tracker.acknowledge(0, 0, -1, -1)) throw new AssertionError("百次快速写入仍保留首字母前的回报");
        if (!tracker.acknowledge(1, 1, 0, 1)) throw new AssertionError("百次写入后识别首字母的迟到回报");
        if (!tracker.acknowledge(100, 100, 0, 100)) throw new AssertionError("合并回报确认最新组字");
        if (tracker.acknowledge(2, 2, -1, -1)) throw new AssertionError("快速输入后仍识别真实外部选区移动");
        tracker.reset(0, 0); tracker.compose(2); tracker.expect();
        tracker.commit(1); tracker.expect(); tracker.compose(3); tracker.expect();
        tracker.compose(4); tracker.expect();
        if (!tracker.acknowledge(1, 1, -1, -1)) throw new AssertionError("不合并批量编辑的宿主可以回报中间 commit");
        if (!tracker.acknowledge(4, 4, 1, 4)) throw new AssertionError("分段选词后的新首字母回报有效");
        if (!tracker.acknowledge(5, 5, 1, 5)) throw new AssertionError("后续输入保持有效");
        tracker.finish(); tracker.expect(); tracker.compose(1); tracker.expect();
        if (!tracker.acknowledge(5, 5, -1, -1)) throw new AssertionError("重开键盘识别迟到 finish 回报");
        if (!tracker.acknowledge(6, 6, 5, 6)) throw new AssertionError("重开后的首字母组字有效");
        System.out.println("EditorTransitionSmoke: PASS");
    }
}
