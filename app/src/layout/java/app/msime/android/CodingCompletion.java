package app.msime.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 离线片段选择器：只补当前连续输入的后缀，不读终端内容、不改写已有文字。 */
public final class CodingCompletion {
    public enum Profile { SHELL, PYTHON, JAVASCRIPT }

    public static final class Entry {
        public final String text;
        public final String description;
        Entry(String text, String description) {
            this.text = text;
            this.description = description;
        }
    }

    public static final class Suggestion {
        public final Entry entry;
        public final String suffix;
        private final long generation;
        private final String prefix;
        private Suggestion(Entry entry, String suffix, long generation, String prefix) {
            this.entry = entry;
            this.suffix = suffix;
            this.generation = generation;
            this.prefix = prefix;
        }
    }

    private static Entry e(String text, String description) { return new Entry(text, description); }

    private static final List<Entry> SHELL = List.of(
        e("git status", "查看工作区"), e("git diff", "查看改动"), e("git log --oneline", "提交记录"),
        e("git add ", "暂存指定文件"), e("git commit -m ", "提交说明"),
        e("git switch ", "切换分支"), e("git branch", "列出分支"), e("git fetch", "获取远端更新"),
        e("git pull", "拉取更新"), e("git push", "推送提交"), e("git clone ", "克隆仓库"),
        e("ls -la", "列出文件"), e("cd ", "切换目录"), e("pwd", "当前目录"),
        e("cat ", "查看文件"), e("less ", "分页查看"), e("grep -n ", "搜索文本"),
        e("rg ", "搜索代码"), e("find . -name ", "查找文件"), e("mkdir -p ", "创建目录"),
        e("cp ", "复制文件"), e("mv ", "移动文件"), e("chmod +x ", "添加执行权限"),
        e("ssh ", "连接远端"), e("scp ", "传输文件"), e("rsync -av ", "同步文件"),
        e("curl -I ", "查看响应头"), e("curl ", "请求地址"), e("ping ", "检查连接"),
        e("ps aux", "查看进程"), e("top", "查看负载"), e("df -h", "磁盘空间"),
        e("du -sh ", "目录大小"), e("tail -f ", "跟随日志"), e("head -n ", "查看开头"),
        e("vim ", "编辑文件"), e("nano ", "编辑文件"), e("tmux", "终端复用"),
        e("python ", "运行 Python"), e("python -m venv .venv", "创建虚拟环境"),
        e("source .venv/bin/activate", "激活虚拟环境"), e("pip install ", "安装 Python 包"),
        e("node ", "运行 JavaScript"), e("npm install", "安装依赖"), e("npm run ", "运行脚本"),
        e("npm run dev", "启动开发服务"), e("npm test", "运行测试"),
        e("pnpm install", "安装依赖"), e("pnpm dev", "启动开发服务"),
        e("docker ps", "查看容器"), e("docker logs ", "查看容器日志"),
        e("docker compose up -d", "启动容器服务"), e("pkg update", "更新 Termux 索引"),
        e("pkg install ", "安装 Termux 包"), e("apt search ", "搜索软件包"),
        e("echo ", "输出文本"), e("export ", "设置环境变量"), e("which ", "查找命令"));

    private static final List<Entry> PYTHON = List.of(
        e("print(", "输出"), e("import ", "导入模块"), e("from ", "从模块导入"),
        e("def ", "定义函数"), e("class ", "定义类"), e("return ", "返回值"),
        e("for ", "循环"), e("while ", "循环"), e("if ", "条件"), e("elif ", "条件分支"),
        e("else:", "其他情况"), e("try:", "异常处理"), e("except ", "捕获异常"),
        e("with ", "上下文管理"), e("async def ", "异步函数"), e("await ", "等待结果"),
        e("self.", "实例成员"), e("None", "空值"), e("True", "真"), e("False", "假"),
        e("range(", "整数序列"), e("len(", "长度"), e("enumerate(", "枚举"));

    private static final List<Entry> JAVASCRIPT = List.of(
        e("console.log(", "输出日志"), e("const ", "常量"), e("let ", "变量"),
        e("function ", "定义函数"), e("return ", "返回值"), e("import ", "导入模块"),
        e("export ", "导出"), e("async ", "异步"), e("await ", "等待结果"),
        e("if (", "条件"), e("for (", "循环"), e("while (", "循环"),
        e("try {", "异常处理"), e("catch (", "捕获异常"), e("class ", "定义类"),
        e("interface ", "TypeScript 接口"), e("type ", "TypeScript 类型"),
        e("Promise.", "异步结果"), e("JSON.stringify(", "序列化"), e("JSON.parse(", "解析 JSON"),
        e("document.querySelector(", "选择元素"), e("fetch(", "请求数据"),
        e("undefined", "未定义"), e("null", "空值"), e("true", "真"), e("false", "假"));

    private Profile profile = Profile.SHELL;
    private String prefix = "";
    private long generation;
    private boolean enabled = true;

    public Profile profile() { return profile; }
    public String prefix() { return prefix; }
    public void setProfile(Profile value) {
        if (value == null) throw new IllegalArgumentException("profile");
        profile = value;
        clear();
    }
    public void setEnabled(boolean value) {
        if (enabled != value) { enabled = value; clear(); }
    }
    public void clear() { prefix = ""; generation++; }

    /** 只保留仍可能属于内置片段的前缀，路径、参数和其他输入随即丢弃。 */
    public void typed(String text) {
        if (!enabled || text == null) { clear(); return; }
        generation++;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 32 || c > 126) { prefix = ""; continue; }
            String next = prefix + c;
            if (isPrefix(next)) prefix = next;
            else prefix = isPrefix(String.valueOf(c)) ? String.valueOf(c) : "";
        }
    }

    private boolean isPrefix(String value) {
        for (Entry entry : catalog(profile)) if (entry.text.startsWith(value)) return true;
        return false;
    }

    public List<Suggestion> suggestions() {
        if (!enabled) return List.of();
        List<Suggestion> result = new ArrayList<>();
        for (Entry entry : catalog(profile)) {
            if (entry.text.startsWith(prefix) && !entry.text.equals(prefix)) {
                result.add(new Suggestion(entry, entry.text.substring(prefix.length()), generation, prefix));
                if (result.size() == 8) break;
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** 返回空值表示这颗候选已过期；调用者成功上屏后才调用 typed。 */
    public String suffix(Suggestion suggestion) {
        if (!enabled || suggestion == null || suggestion.generation != generation
                || !suggestion.prefix.equals(prefix)) return null;
        return suggestion.suffix;
    }

    public static List<Entry> catalog(Profile profile) {
        switch (profile) {
            case PYTHON: return PYTHON;
            case JAVASCRIPT: return JAVASCRIPT;
            default: return SHELL;
        }
    }
}
