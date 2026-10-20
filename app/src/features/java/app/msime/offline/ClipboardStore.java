package app.msime.offline;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Private history, optional expiry, pinned entries retained. Worker-thread access only. */
final class ClipboardStore extends SQLiteOpenHelper {
    static final class Item {
        final long id; final String text; final boolean pinned;
        Item(long id, String text, boolean pinned) { this.id = id; this.text = text; this.pinned = pinned; }
    }
    ClipboardStore(Context context) { super(context, "clipboard.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE clips (id INTEGER PRIMARY KEY, text TEXT NOT NULL UNIQUE, copied INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int old, int next) { }
    void add(String text,long copied) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long now=System.currentTimeMillis();
            ContentValues values = new ContentValues(); values.put("copied", copied>0 && copied<=now?copied:now);
            if (db.update("clips", values, "text=?", new String[]{text}) == 0) {
                values.put("text", text); db.insertOrThrow("clips", null, values);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    List<Item> list(int limit) {
        List<Item> result = new ArrayList<>();
        try (Cursor rows = getReadableDatabase().query("clips", new String[]{"id", "text", "pinned"}, null, null, null, null,
                "pinned DESC, copied DESC, id DESC", String.valueOf(limit))) {
            while (rows.moveToNext()) result.add(new Item(rows.getLong(0), rows.getString(1), rows.getInt(2) != 0));
        }
        return result;
    }
    Item latest() {
        try (Cursor rows = getReadableDatabase().query("clips", new String[]{"id", "text", "pinned"}, null, null, null, null,
                "copied DESC, id DESC", "1")) {
            return rows.moveToFirst() ? new Item(rows.getLong(0), rows.getString(1), rows.getInt(2) != 0) : null;
        }
    }
    void expire(long now, long retention) {
        if (retention > 0) getWritableDatabase().delete("clips", "pinned=0 AND copied<=?",
            new String[]{String.valueOf(now-retention)});
    }
    void pin(long id, boolean pinned) {
        ContentValues values = new ContentValues(); values.put("pinned", pinned ? 1 : 0);
        getWritableDatabase().update("clips", values, "id=?", new String[]{String.valueOf(id)});
    }
    void delete(long id) { getWritableDatabase().delete("clips", "id=?", new String[]{String.valueOf(id)}); }
}
