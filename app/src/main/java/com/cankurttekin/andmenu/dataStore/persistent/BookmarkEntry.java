package com.cankurttekin.andmenu.dataStore.persistent;

public class BookmarkEntry {
    public int id;
    public String name = "";
    public String url = "";

    public BookmarkEntry() {}

    public BookmarkEntry(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public BookmarkEntry(int id, String name, String url) {
        this.id = id;
        this.name = name;
        this.url = url;
    }
}
