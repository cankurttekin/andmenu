package com.cankurttekin.andmenu.commands.bookmarks;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class BookmarkUsageHistory {
    private static final String PREF_USAGE_PREFIX = "bookmark_usage_count_";

    public static void recordUsage(Context context, String bookmarkName) {
        if (bookmarkName == null || bookmarkName.isEmpty()) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int count = prefs.getInt(PREF_USAGE_PREFIX + bookmarkName, 0);
        prefs.edit().putInt(PREF_USAGE_PREFIX + bookmarkName, count + 1).apply();
    }

    public static int getUsageCount(Context context, String bookmarkName) {
        if (bookmarkName == null || bookmarkName.isEmpty()) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_USAGE_PREFIX + bookmarkName, 0);
    }
}
