package app.msime.offline;

import android.content.Context;
import app.msime.android.NativeClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** 只从 APK 解包并校验本地词库，不包含下载、账户或网络入口。 */
final class EngineInstall {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static volatile String options;
    interface Result { void done(String options, String error); }
    static JSONObject value(String response) throws Exception {
        JSONObject envelope = new JSONObject(response);
        if (!envelope.getBoolean("ok")) throw new IllegalStateException("本地引擎：" + envelope.optString("error"));
        return envelope.getJSONObject("value");
    }
    static void prepare(Context context, Result result) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            try {
                if (options == null) options = install(app);
                result.done(options, "");
            } catch (Exception | LinkageError error) {
                android.util.Log.e("NopeEngine", "Resource/bootstrap preparation failed", error);
                result.done(null, "本地词库准备失败，点击重试；可先切换英文或 Coding");
            }
        });
    }
    private static String install(Context app) throws Exception {
        File resources = new File(app.getFilesDir(), "resources-v031");
        File state = new File(app.getFilesDir(), "local-state");
        if ((!resources.isDirectory() && !resources.mkdirs()) || (!state.isDirectory() && !state.mkdirs()))
            throw new IllegalStateException("无法创建本地词库目录");
        JSONObject lock;
        try (InputStream in = app.getAssets().open("runtime-dictionary.lock.json")) {
            lock = new JSONObject(readSmall(in));
        }
        JSONObject japanese;
        try (InputStream in = app.getAssets().open("japanese-dictionary.lock.json")) {
            japanese = new JSONObject(readSmall(in));
        }
        String identity = hex(MessageDigest.getInstance("SHA-256").digest((lock.toString() + japanese.toString()).getBytes(StandardCharsets.UTF_8)));
        File marker = new File(app.getFilesDir(), "resources-v031.verified");
        boolean verified = false;
        if (marker.isFile()) {
            try (InputStream in = new java.io.FileInputStream(marker)) { verified = identity.equals(readSmall(in)); }
        }
        JSONArray artifacts = lock.getJSONArray("artifacts");
        JSONArray japaneseArtifacts = japanese.getJSONArray("artifacts");
        for (int i = 0; i < japaneseArtifacts.length(); i++) artifacts.put(japaneseArtifacts.getJSONObject(i));
        for (int i = 0; i < artifacts.length(); i++) {
            JSONObject entry = artifacts.getJSONObject(i);
            String name = entry.getString("name");
            if (!name.matches("[A-Za-z0-9_.-]+") || name.contains("..")) throw new IllegalArgumentException("非法词库文件名");
            File file = new File(resources, name);
            long size = entry.getLong("size");
            if (verified && file.isFile() && file.length() == size) continue;
            File staging = new File(resources, name + ".part");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long copied = 0;
            try (InputStream in = app.getAssets().open("dictionary/" + name);
                    FileOutputStream out = new FileOutputStream(staging)) {
                byte[] buffer = new byte[65536];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    copied += count;
                    if (copied > size) throw new IllegalStateException("词库超过约定大小");
                    digest.update(buffer, 0, count);
                    out.write(buffer, 0, count);
                }
                out.getFD().sync();
            }
            if (copied != size || !hex(digest.digest()).equals(entry.getString("sha256"))) {
                staging.delete();
                throw new IllegalStateException("词库校验失败");
            }
            if (file.exists() && !file.delete()) throw new IllegalStateException("词库替换失败");
            if (!staging.renameTo(file)) throw new IllegalStateException("词库发布失败");
        }
        JSONObject ready = value(NativeClient.prepareHost(new JSONObject()
            .put("resources", resources.getAbsolutePath()).put("state_root", state.getAbsolutePath()).toString()));
        // 始终使用原生的完整偏好默认值，再显式关闭服务器相关能力。
        JSONObject preferences = value(NativeClient.defaultPreferences());
        preferences.put("scheme", "quanpin").put("learning", true)
            .put("cloud_candidates", false).put("usage_reporting", false);
        preferences.getJSONObject("sentence_association").put("neural_keyboard", false).put("neural_desktop", false);
        ready.put("preferences", preferences);
        // 原生完整清单校验与宿主准备成功后才发布校验标记。
        try (FileOutputStream out = new FileOutputStream(marker)) { out.write(identity.getBytes(StandardCharsets.UTF_8)); }
        return ready.toString();
    }
    private static String readSmall(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int count;
        while ((count = in.read(buffer)) != -1) {
            if (out.size() + count > 65536) throw new IllegalStateException("元数据过大");
            out.write(buffer, 0, count);
        }
        return out.toString("UTF-8");
    }
    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }
}
