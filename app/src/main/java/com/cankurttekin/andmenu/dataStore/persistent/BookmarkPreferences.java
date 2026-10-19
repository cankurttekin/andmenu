package com.cankurttekin.andmenu.dataStore.persistent;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BookmarkPreferences extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "bookmark_preferences.sqlite";
    private static final int DATABASE_VERSION = 1;
    private static final String[] columnsInDB = {"id", "name", "url"};

    private static BookmarkPreferences singleton = null;

    private BookmarkPreferences(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public synchronized static BookmarkPreferences getInstance(Context context) {
        if (singleton == null) {
            singleton = new BookmarkPreferences(context.getApplicationContext());
        }
        return singleton;
    }

    public static void destroyFilesForCleanTest(Context context) {
        if (singleton != null) {
            singleton.close();
            singleton = null;
        }
        context.getDatabasePath(DATABASE_NAME).delete();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE bookmarks (" +
                "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "  name TEXT NOT NULL," +
                "  url TEXT NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public void close() {
        singleton = null;
        super.close();
    }

    public long addBookmark(String name, String url) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("url", url);
        return this.getWritableDatabase().insert("bookmarks", null, cv);
    }

    @SuppressLint("Range")
    public List<BookmarkEntry> getAllBookmarks() {
        List<BookmarkEntry> ret = new ArrayList<>();
        try (Cursor cursor = this.getReadableDatabase().query("bookmarks", columnsInDB, null, null, null, null, "id DESC")) {
            while (cursor.moveToNext()) {
                BookmarkEntry entry = new BookmarkEntry();
                entry.id = cursor.getInt(cursor.getColumnIndex("id"));
                entry.name = cursor.getString(cursor.getColumnIndex("name"));
                entry.url = cursor.getString(cursor.getColumnIndex("url"));
                ret.add(entry);
            }
        }
        return ret;
    }

    public void deleteBookmark(int id) {
        this.getWritableDatabase().delete("bookmarks", "id = ?", new String[]{String.valueOf(id)});
    }

    public void updateBookmark(int id, String name, String url) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("url", url);
        this.getWritableDatabase().update("bookmarks", cv, "id = ?", new String[]{String.valueOf(id)});
    }
}
