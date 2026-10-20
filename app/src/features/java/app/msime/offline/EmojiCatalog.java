package app.msime.offline;

import android.content.Context;
import android.graphics.Paint;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Full Unicode 17 catalog, rendered with the device's offline emoji font. */
final class EmojiCatalog {
    record Item(String text, String description) { }
    record Category(String title, List<Item> items) { }
    private EmojiCatalog() { }
    static List<Category> load(Context context) throws Exception {
        JSONObject catalog;
        try (var stream = context.getAssets().open("emoji/catalog.json")) {
            var data = new java.io.ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
            while ((count = stream.read(buffer)) != -1) data.write(buffer, 0, count);
            catalog = new JSONObject(data.toString(StandardCharsets.UTF_8.name()));
        }
        String[] titles = {"表情", "人物", "动物", "食物", "旅行", "活动", "物品", "符号", "旗帜"};
        JSONArray groups = catalog.getJSONArray("groups");
        List<Category> result = new ArrayList<>();
        Paint font = new Paint(Paint.ANTI_ALIAS_FLAG); font.setTextSize(32); font.setTypeface(android.graphics.Typeface.DEFAULT);
        android.graphics.Rect ink = new android.graphics.Rect();
        for (int i = 0; i < groups.length(); i++) {
            JSONArray items = groups.getJSONObject(i).getJSONArray("items");
            List<Item> supported = new ArrayList<>();
            for (int j = 0; j < items.length(); j++) {
                JSONArray item = items.getJSONArray(j);
                String text = item.getString(0);
                // Unsupported sequences otherwise appear as boxes or disconnected components.
                font.getTextBounds(text, 0, text.length(), ink);
                if (font.hasGlyph(text) && font.measureText(text) > 0 && !ink.isEmpty())
                    supported.add(new Item(text, item.getString(1)));
            }
            result.add(new Category(titles[i], List.copyOf(supported)));
        }
        return List.copyOf(result);
    }
}
