package com.cankurttekin.andmenu.dataStore.persistent;

public class NoteEntry {
    public int id;
    public String content = "";
    public boolean isCompleted = false;
    public long timestamp = 0;

    public NoteEntry() {}

    public NoteEntry(int id, String content, boolean isCompleted, long timestamp) {
        this.id = id;
        this.content = content;
        this.isCompleted = isCompleted;
        this.timestamp = timestamp;
    }
}
