package com.cankurttekin.andmenu.dataStore.persistent;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class NotePreferences extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "note_preferences.sqlite";
    private static final int DATABASE_VERSION = 1;
    private static final String[] columnsInDB = {"id", "content", "is_completed", "timestamp"};

    private static NotePreferences singleton = null;

    private NotePreferences(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public synchronized static NotePreferences getInstance(Context context) {
        if (singleton == null) {
            singleton = new NotePreferences(context.getApplicationContext());
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
        db.execSQL("CREATE TABLE notes (" +
                "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "  content TEXT NOT NULL," +
                "  is_completed INTEGER NOT NULL DEFAULT 0," +
                "  timestamp INTEGER NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public void close() {
        singleton = null;
        super.close();
    }

    public long addNote(String content) {
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        cv.put("is_completed", 0);
        cv.put("timestamp", System.currentTimeMillis());
        return this.getWritableDatabase().insert("notes", null, cv);
    }

    @SuppressLint("Range")
    public List<NoteEntry> getAllNotes() {
        List<NoteEntry> ret = new ArrayList<>();
        try (Cursor cursor = this.getReadableDatabase().query("notes", columnsInDB, null, null, null, null, "is_completed ASC, id DESC")) {
            while (cursor.moveToNext()) {
                NoteEntry entry = new NoteEntry();
                entry.id = cursor.getInt(cursor.getColumnIndex("id"));
                entry.content = cursor.getString(cursor.getColumnIndex("content"));
                entry.isCompleted = cursor.getInt(cursor.getColumnIndex("is_completed")) != 0;
                entry.timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
                ret.add(entry);
            }
        }
        return ret;
    }

    public void deleteNote(int id) {
        this.getWritableDatabase().delete("notes", "id = ?", new String[]{String.valueOf(id)});
    }

    public void toggleNoteCompleted(int id, boolean currentStatus) {
        ContentValues cv = new ContentValues();
        cv.put("is_completed", currentStatus ? 0 : 1);
        this.getWritableDatabase().update("notes", cv, "id = ?", new String[]{String.valueOf(id)});
    }

    public void updateNoteContent(int id, String newContent) {
        ContentValues cv = new ContentValues();
        cv.put("content", newContent);
        this.getWritableDatabase().update("notes", cv, "id = ?", new String[]{String.valueOf(id)});
    }

    public void clearCompletedNotes() {
        this.getWritableDatabase().delete("notes", "is_completed != 0", null);
    }
}
