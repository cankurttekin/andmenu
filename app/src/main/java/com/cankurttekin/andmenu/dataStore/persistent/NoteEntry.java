package com.cankurttekin.andmenu.dataStore.persistent;

public class NoteEntry {
    public int id;
    public String content = "";
    public boolean isCompleted = false;
    public int isCompletedStatus = -1;
    public long timestamp = 0;

    public NoteEntry() {}

    public NoteEntry(int id, String content, boolean isCompleted, long timestamp) {
        this.id = id;
        this.content = content;
        this.isCompleted = isCompleted;
        this.isCompletedStatus = isCompleted ? 1 : 0;
        this.timestamp = timestamp;
    }

    public NoteEntry(int id, String content, int isCompletedStatus, long timestamp) {
        this.id = id;
        this.content = content;
        this.isCompletedStatus = isCompletedStatus;
        this.isCompleted = isCompletedStatus == 1;
        this.timestamp = timestamp;
    }

    public boolean isRegularNote() {
        return isCompletedStatus == -1;
    }
}
