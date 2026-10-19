package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import androidx.preference.PreferenceManager;

import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.applicationMain.MainActivity;
import com.cankurttekin.andmenu.commandSearchers.lib.StringMatchStrategy;
import com.cankurttekin.andmenu.dataStore.persistent.BookmarkEntry;
import com.cankurttekin.andmenu.dataStore.persistent.BookmarkPreferences;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;
import com.cankurttekin.andmenu.lib.StringValidator;

import java.util.ArrayList;
import java.util.List;

public class BookmarkCommandSearcher implements CommandSearcher {
    public static final String PREF_BOOKMARK_SEARCH_ENABLED_KEY = "pref_bookmark_search_enabled";

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

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_BOOKMARK_SEARCH_ENABLED_KEY, true)) {
            return candidates;
        }

        if (!query.toLowerCase().startsWith("b ")) {
            return candidates;
        }

        String subQuery = query.substring(2).trim();

        BookmarkPreferences db = BookmarkPreferences.getInstance(context);
        List<BookmarkEntry> allBookmarks = db.getAllBookmarks();

        if (subQuery.isEmpty()) {
            for (BookmarkEntry entry : allBookmarks) {
                candidates.add(new BookmarkItemCandidateEntry(entry));
            }
            candidates.add(new BookmarkAddInstructionCandidateEntry());
            return candidates;
        }

        String lowerSub = subQuery.toLowerCase();

        if (lowerSub.startsWith("add ")) {
            String rest = subQuery.substring(4).trim();
            if (!rest.isEmpty()) {
                String name = rest;
                String url = "";

                int lastSpace = rest.lastIndexOf(' ');
                if (lastSpace != -1) {
                    String candidateUrl = rest.substring(lastSpace + 1).trim();
                    if (candidateUrl.contains(".") || candidateUrl.startsWith("http")) {
                        name = rest.substring(0, lastSpace).trim();
                        url = candidateUrl;
                    }
                } else if (rest.startsWith("http://") || rest.startsWith("https://") || rest.contains(".")) {
                    name = rest;
                    url = rest;
                }

                if (!url.startsWith("http://") && !url.startsWith("https://") && !url.isEmpty()) {
                    url = "https://" + url;
                }

                if (name.isEmpty()) {
                    name = url;
                }

                candidates.add(new AddBookmarkCandidateEntry(name, url));
            }
            return candidates;
        }

        if (lowerSub.startsWith("del ") || lowerSub.startsWith("delete ")) {
            String targetName = subQuery.substring(subQuery.indexOf(' ') + 1).trim().toLowerCase();
            for (BookmarkEntry entry : allBookmarks) {
                if (entry.name.toLowerCase().contains(targetName) || entry.url.toLowerCase().contains(targetName)) {
                    candidates.add(new DeleteBookmarkCandidateEntry(entry));
                }
            }
            return candidates;
        }

        for (BookmarkEntry entry : allBookmarks) {
            if (StringMatchStrategy.match(context, subQuery, entry.name, false) != -1 ||
                    StringMatchStrategy.match(context, subQuery, entry.url, false) != -1) {
                candidates.add(new BookmarkItemCandidateEntry(entry));
            }
        }

        if (StringValidator.isValidURLAccepted(subQuery, false, context)) {
            String formattedUrl = subQuery;
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://" + formattedUrl;
            }
            candidates.add(new AddBookmarkCandidateEntry(subQuery, formattedUrl));
        } else {
            String name = subQuery;
            String url = "https://" + subQuery;
            candidates.add(new AddBookmarkCandidateEntry(name, url));
        }

        return candidates;
    }

    private static class BookmarkItemCandidateEntry implements CandidateEntry {
        private final BookmarkEntry bookmark;

        BookmarkItemCandidateEntry(BookmarkEntry bookmark) {
            this.bookmark = bookmark;
        }

        @NonNull
        @Override
        public String getTitle() {
            return "★ " + bookmark.name + " (" + bookmark.url + ")";
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)));
                    activity.finishIfNotHome();
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.error_failure_could_not_open_url, Toast.LENGTH_LONG).show();
                }
            };
        }

        @Override
        public void onLongClick(MainActivity activity) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(bookmark.name);
            builder.setItems(new CharSequence[]{"Open", "Edit", "Delete"}, (dialog, which) -> {
                if (which == 0) { // Open
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)));
                        activity.finishIfNotHome();
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(activity, R.string.error_failure_could_not_open_url, Toast.LENGTH_LONG).show();
                    }
                } else if (which == 1) { // Edit
                    AlertDialog.Builder editBuilder = new AlertDialog.Builder(activity);
                    editBuilder.setTitle("Edit Bookmark");

                    LinearLayout layout = new LinearLayout(activity);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(32, 16, 32, 16);

                    final EditText nameInput = new EditText(activity);
                    nameInput.setHint("Name");
                    nameInput.setText(bookmark.name);
                    layout.addView(nameInput);

                    final EditText urlInput = new EditText(activity);
                    urlInput.setHint("URL");
                    urlInput.setText(bookmark.url);
                    layout.addView(urlInput);

                    editBuilder.setView(layout);
                    editBuilder.setPositiveButton(R.string.button_ok, (dialog1, which1) -> {
                        String newName = nameInput.getText().toString().trim();
                        String newUrl = urlInput.getText().toString().trim();
                        if (!newName.isEmpty() && !newUrl.isEmpty()) {
                            BookmarkPreferences.getInstance(activity).updateBookmark(bookmark.id, newName, newUrl);
                            activity.refreshSearch();
                        }
                    });
                    editBuilder.setNegativeButton(R.string.button_cancel, (dialog1, which1) -> dialog1.cancel());
                    editBuilder.show();
                } else if (which == 2) { // Delete
                    BookmarkPreferences.getInstance(activity).deleteBookmark(bookmark.id);
                    Toast.makeText(activity, "Bookmark deleted", Toast.LENGTH_SHORT).show();
                    activity.refreshSearch();
                }
            });
            builder.show();
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
            return ContextCompat.getDrawable(context, android.R.drawable.btn_star_big_on);
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

    private static class AddBookmarkCandidateEntry implements CandidateEntry {
        private final String name;
        private final String url;

        AddBookmarkCandidateEntry(String name, String url) {
            this.name = name;
            this.url = url;
        }

        @NonNull
        @Override
        public String getTitle() {
            if (!url.isEmpty() && !url.equals(name)) {
                return "+ Add Bookmark: " + name + " -> " + url;
            }
            return "+ Add Bookmark: " + name;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                String targetUrl = url;
                if (targetUrl.isEmpty()) {
                    targetUrl = "https://" + name;
                }
                BookmarkPreferences.getInstance(activity).addBookmark(name, targetUrl);
                Toast.makeText(activity, "Bookmark added: " + name, Toast.LENGTH_SHORT).show();
                activity.refreshSearch();
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
            return ContextCompat.getDrawable(context, android.R.drawable.ic_input_add);
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

    private static class DeleteBookmarkCandidateEntry implements CandidateEntry {
        private final BookmarkEntry bookmark;

        DeleteBookmarkCandidateEntry(BookmarkEntry bookmark) {
            this.bookmark = bookmark;
        }

        @NonNull
        @Override
        public String getTitle() {
            return "- Delete Bookmark: " + bookmark.name;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                BookmarkPreferences.getInstance(activity).deleteBookmark(bookmark.id);
                Toast.makeText(activity, "Bookmark deleted: " + bookmark.name, Toast.LENGTH_SHORT).show();
                activity.refreshSearch();
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
            return ContextCompat.getDrawable(context, android.R.drawable.ic_delete);
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

    private static class BookmarkAddInstructionCandidateEntry implements CandidateEntry {
        @NonNull
        @Override
        public String getTitle() {
            return "To add bookmark: b add <name> <url>";
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
