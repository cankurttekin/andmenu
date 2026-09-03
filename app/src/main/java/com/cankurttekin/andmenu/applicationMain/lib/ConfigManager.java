package com.cankurttekin.andmenu.applicationMain.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cankurttekin.andmenu.dataStore.persistent.HomeScreenSetting;
import com.cankurttekin.andmenu.dataStore.persistent.URLEntry;
import com.cankurttekin.andmenu.dataStore.persistent.URLPreferences;
import com.cankurttekin.andmenu.dataStore.deviceLocal.WidgetsSetting;
import com.cankurttekin.andmenu.wrapperForAndroid.AppWidgetsHostManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class ConfigManager {
    public static String exportConfig(Context context) {
        try {
            JSONObject root = new JSONObject();

            // SharedPreferences
            JSONObject prefs = new JSONObject();
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            Map<String, ?> allEntries = sharedPrefs.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                prefs.put(entry.getKey(), entry.getValue());
            }
            root.put("preferences", prefs);

            // URLPreferences
            JSONArray urls = new JSONArray();
            for (URLEntry entry : URLPreferences.getInstance(context).getAllEntries()) {
                JSONObject obj = new JSONObject();
                obj.put("name", entry.name);
                obj.put("display_name", entry.display_name);
                obj.put("url_base", entry.url_base);
                obj.put("has_query", entry.has_query);
                urls.put(obj);
            }
            root.put("urls", urls);

            // HomeScreenSetting
            JSONArray homeItems = new JSONArray();
            for (HomeScreenSetting.HomeScreenDefaultItem entry : HomeScreenSetting.getInstance(context).getAllHomeScreenDefaultItems()) {
                JSONObject obj = new JSONObject();
                obj.put("type", entry.type);
                obj.put("data", entry.data);
                homeItems.put(obj);
            }
            root.put("home_items", homeItems);

            // Widget Commands (WidgetsSetting)
            JSONArray widgetCommands = new JSONArray();
            AppWidgetsHostManager hostManager = new AppWidgetsHostManager(context);
            for (AppWidgetsHostManager.WidgetCommand entry : WidgetsSetting.getInstance(context).getAllWidgetCommands(hostManager)) {
                JSONObject obj = new JSONObject();
                obj.put("command", entry.command);
                obj.put("abbreviation", entry.abbreviation);
                obj.put("app_widget_id", entry.appWidgetId);
                obj.put("height_px", entry.heightPx);
                widgetCommands.put(obj);
            }
            root.put("widget_commands", widgetCommands);

            return root.toString(2);
        } catch (Exception e) {
            return "Error exporting config: " + e.getMessage();
        }
    }

    public static boolean importConfig(Context context, String json) {
        try {
            JSONObject root = new JSONObject(json);

            // SharedPreferences
            if (root.has("preferences")) {
                JSONObject prefs = root.getJSONObject("preferences");
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPrefs.edit();

                // Remove keys not present in the new JSON
                for (String key : sharedPrefs.getAll().keySet()) {
                    if (!prefs.has(key)) {
                        editor.remove(key);
                    }
                }

                // Update or add keys from JSON
                for (java.util.Iterator<String> it = prefs.keys(); it.hasNext(); ) {
                    String key = it.next();
                    Object value = prefs.get(key);
                    if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
                    else if (value instanceof Integer) editor.putInt(key, (Integer) value);
                    else if (value instanceof Long) editor.putLong(key, (Long) value);
                    else if (value instanceof Float) editor.putFloat(key, (Float) value);
                    else if (value instanceof String) editor.putString(key, (String) value);
                }
                editor.apply();
            }

            // URLPreferences
            if (root.has("urls")) {
                URLPreferences db = URLPreferences.getInstance(context);
                // Clear current
                for (URLEntry entry : db.getAllEntries()) {
                    db.deleteById(entry.id);
                }
                JSONArray urls = root.getJSONArray("urls");
                for (int i = 0; i < urls.length(); i++) {
                    JSONObject obj = urls.getJSONObject(i);
                    URLEntry entry = new URLEntry();
                    entry.name = obj.getString("name");
                    entry.display_name = obj.getString("display_name");
                    entry.url_base = obj.getString("url_base");
                    entry.has_query = obj.getBoolean("has_query");
                    db.add(entry);
                }
            }

            // HomeScreenSetting
            if (root.has("home_items")) {
                HomeScreenSetting db = HomeScreenSetting.getInstance(context);
                for (HomeScreenSetting.HomeScreenDefaultItem entry : db.getAllHomeScreenDefaultItems()) {
                    db.deleteHomeScreenDefaultItem(entry.id);
                }
                JSONArray items = root.getJSONArray("home_items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject obj = items.getJSONObject(i);
                    HomeScreenSetting.HomeScreenDefaultItem entry = new HomeScreenSetting.HomeScreenDefaultItem();
                    entry.type = obj.getInt("type");
                    entry.data = obj.getString("data");
                    db.addHomeScreenDefaultItem(entry);
                }
            }

            // Widget Commands
            if (root.has("widget_commands")) {
                WidgetsSetting db = WidgetsSetting.getInstance(context);
                AppWidgetsHostManager hostManager = new AppWidgetsHostManager(context);
                for (AppWidgetsHostManager.WidgetCommand entry : db.getAllWidgetCommands(hostManager)) {
                    db.deleteWidgetCommandById(entry.id);
                }
                JSONArray items = root.getJSONArray("widget_commands");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject obj = items.getJSONObject(i);
                    AppWidgetsHostManager.WidgetCommand entry = new AppWidgetsHostManager.WidgetCommand(-1, null, obj.getInt("app_widget_id"));
                    entry.command = obj.getString("command");
                    entry.abbreviation = obj.getBoolean("abbreviation");
                    entry.heightPx = obj.getInt("height_px");
                    db.addWidgetCommand(entry);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
