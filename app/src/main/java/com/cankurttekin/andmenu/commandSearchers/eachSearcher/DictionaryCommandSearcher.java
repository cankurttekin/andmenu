package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.cankurttekin.andmenu.applicationMain.MainActivity;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DictionaryCommandSearcher implements CommandSearcher {
    public static final String PREF_DICTIONARY_SEARCH_ENABLED_KEY = "pref_dictionary_search_enabled";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Map<String, List<DefinitionItem>> cache = new HashMap<>();
    private static final Map<String, Boolean> loadingMap = new HashMap<>();

    @Override
    public void refresh(Context context) {}

    @Override
    public void close() {}

    @Override
    public boolean isPrepared() {
        return true;
    }

    @Override
    public void waitUntilPrepared() {}

    @NonNull
    @Override
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_DICTIONARY_SEARCH_ENABLED_KEY, true)) {
            return candidates;
        }

        if (!query.toLowerCase().startsWith("define ")) {
            return candidates;
        }

        String word = query.substring(7).trim();

        if (word.isEmpty()) {
            candidates.add(new DictionaryInstructionCandidateEntry());
            return candidates;
        }

        String lowerWord = word.toLowerCase();

        if (cache.containsKey(lowerWord)) {
            List<DefinitionItem> defs = cache.get(lowerWord);
            if (defs == null || defs.isEmpty()) {
                candidates.add(new DictionaryMessageCandidateEntry("No definitions found for \"" + word + "\""));
            } else {
                for (int i = 0; i < defs.size() && i < 3; i++) {
                    candidates.add(new DefinitionCandidateEntry(defs.get(i), i + 1));
                }
            }
        } else if (Boolean.TRUE.equals(loadingMap.get(lowerWord))) {
            candidates.add(new DictionaryMessageCandidateEntry("Loading definition for \"" + word + "\"..."));
        } else {
            candidates.add(new DefineActionCandidateEntry(word, lowerWord));
        }

        return candidates;
    }

    private static void fetchDefinitionAsync(String word, String lowerWord, Context context) {
        if (Boolean.TRUE.equals(loadingMap.get(lowerWord))) return;
        loadingMap.put(lowerWord, true);

        executor.execute(() -> {
            List<DefinitionItem> definitions = new ArrayList<>();
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en_US/" + Uri.encode(word));
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        definitions = parseDictionaryJson(sb.toString(), word);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            final List<DefinitionItem> finalDefs = definitions;
            new Handler(Looper.getMainLooper()).post(() -> {
                cache.put(lowerWord, finalDefs);
                loadingMap.put(lowerWord, false);
                if (context instanceof MainActivity) {
                    ((MainActivity) context).refreshSearch();
                }
            });
        });
    }

    private static List<DefinitionItem> parseDictionaryJson(String jsonStr, String fallbackWord) {
        List<DefinitionItem> results = new ArrayList<>();
        try {
            JSONArray rootArr = new JSONArray(jsonStr);
            for (int i = 0; i < rootArr.length() && results.size() < 3; i++) {
                JSONObject entryObj = rootArr.getJSONObject(i);
                String wordName = entryObj.optString("word", fallbackWord);

                JSONArray meaningsArr = entryObj.optJSONArray("meanings");
                if (meaningsArr != null) {
                    for (int m = 0; m < meaningsArr.length() && results.size() < 3; m++) {
                        JSONObject meaningObj = meaningsArr.getJSONObject(m);
                        String pos = meaningObj.optString("partOfSpeech", "");

                        JSONArray defsArr = meaningObj.optJSONArray("definitions");
                        if (defsArr != null) {
                            for (int d = 0; d < defsArr.length() && results.size() < 3; d++) {
                                JSONObject defObj = defsArr.getJSONObject(d);
                                String defText = defObj.optString("definition", "");
                                if (!defText.isEmpty()) {
                                    DefinitionItem item = new DefinitionItem();
                                    item.word = wordName;
                                    item.partOfSpeech = pos;
                                    item.definition = defText;
                                    results.add(item);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return results;
    }

    public static class DefinitionItem {
        public String word = "";
        public String partOfSpeech = "";
        public String definition = "";
    }

    private static class DefineActionCandidateEntry implements CandidateEntry {
        private final String word;
        private final String lowerWord;

        DefineActionCandidateEntry(String word, String lowerWord) {
            this.word = word;
            this.lowerWord = lowerWord;
        }

        @NonNull
        @Override
        public String getTitle() {
            return "Define \"" + word + "\"";
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                Toast.makeText(context, "Fetching definition for \"" + word + "\"...", Toast.LENGTH_SHORT).show();
                fetchDefinitionAsync(word, lowerWord, context);
                if (activity != null) {
                    activity.refreshSearch();
                }
            };
        }

        @Override
        public boolean hasEvent() {
            return true;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_search);
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }

    private static class DefinitionCandidateEntry implements CandidateEntry {
        private final DefinitionItem item;
        private final int index;

        DefinitionCandidateEntry(DefinitionItem item, int index) {
            this.item = item;
            this.index = index;
        }

        @NonNull
        @Override
        public String getTitle() {
            String pos = item.partOfSpeech.isEmpty() ? "" : " (" + item.partOfSpeech + ")";
            return index + ". " + item.word + pos + ": " + item.definition;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    String textToCopy = item.word + ": " + item.definition;
                    ClipData clip = ClipData.newPlainText("definition", textToCopy);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Copied definition to clipboard", Toast.LENGTH_SHORT).show();
                }
            };
        }

        @Override
        public boolean hasEvent() {
            return true;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_info_details);
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }

    private static class DictionaryMessageCandidateEntry implements CandidateEntry {
        private final String message;

        DictionaryMessageCandidateEntry(String message) {
            this.message = message;
        }

        @NonNull
        @Override
        public String getTitle() {
            return message;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return false;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_help);
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }

    private static class DictionaryInstructionCandidateEntry implements CandidateEntry {
        @NonNull
        @Override
        public String getTitle() {
            return "To define a word: define <word>";
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return false;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_info_details);
        }

        @Override
        public boolean isSubItem() {
            return true;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }
}
