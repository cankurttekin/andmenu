package com.cankurttekin.andmenu.commands.contacts;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class ContactUsageHistory {
    private static final String PREF_USAGE_PREFIX = "contact_usage_count_";

    public static void recordUsage(Context context, String contactIdentifier) {
        if (contactIdentifier == null || contactIdentifier.isEmpty()) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int count = prefs.getInt(PREF_USAGE_PREFIX + contactIdentifier, 0);
        prefs.edit().putInt(PREF_USAGE_PREFIX + contactIdentifier, count + 1).apply();
    }

    public static int getUsageCount(Context context, String contactIdentifier) {
        if (contactIdentifier == null || contactIdentifier.isEmpty()) {
            return 0;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_USAGE_PREFIX + contactIdentifier, 0);
    }
}
