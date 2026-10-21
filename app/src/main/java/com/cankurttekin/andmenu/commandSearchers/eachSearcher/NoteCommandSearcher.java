package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
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
import com.cankurttekin.andmenu.dataStore.persistent.NoteEntry;
import com.cankurttekin.andmenu.dataStore.persistent.NotePreferences;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;

import java.util.ArrayList;
import java.util.List;

public class NoteCommandSearcher implements CommandSearcher {
    public static final String PREF_NOTE_SEARCH_ENABLED_KEY = "pref_note_search_enabled";

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

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_NOTE_SEARCH_ENABLED_KEY, true)) {
            return candidates;
        }

        if (!query.toLowerCase().startsWith("n ")) {
            return candidates;
        }

        String subQuery = query.substring(2).trim();

        NotePreferences db = NotePreferences.getInstance(context);
        List<NoteEntry> allNotes = db.getAllNotes();

        if (subQuery.isEmpty()) {
            boolean hasCompleted = false;
            for (NoteEntry note : allNotes) {
                candidates.add(new NoteItemCandidateEntry(note));
                if (note.isCompleted) {
                    hasCompleted = true;
                }
            }
            if (hasCompleted) {
                candidates.add(new ClearCompletedNotesCandidateEntry());
            }
            candidates.add(new NoteAddInstructionCandidateEntry());
            return candidates;
        }

        String lowerSub = subQuery.toLowerCase();

        if (lowerSub.equalsIgnoreCase("clear")) {
            candidates.add(new ClearCompletedNotesCandidateEntry());
            return candidates;
        }

        if (lowerSub.startsWith("del ") || lowerSub.startsWith("delete ")) {
            String targetText = subQuery.substring(subQuery.indexOf(' ') + 1).trim().toLowerCase();
            for (NoteEntry note : allNotes) {
                if (note.content.toLowerCase().contains(targetText)) {
                    candidates.add(new DeleteNoteCandidateEntry(note));
                }
            }
            return candidates;
        }

        String contentToAdd = subQuery;
        if (lowerSub.startsWith("add ")) {
            contentToAdd = subQuery.substring(4).trim();
        }

        for (NoteEntry note : allNotes) {
            if (StringMatchStrategy.match(context, subQuery, note.content, false) != -1) {
                candidates.add(new NoteItemCandidateEntry(note));
            }
        }

        if (!contentToAdd.isEmpty()) {
            candidates.add(new AddNoteCandidateEntry(contentToAdd, -1));
            candidates.add(new AddNoteCandidateEntry(contentToAdd, 0));
        }

        return candidates;
    }

    private static class NoteItemCandidateEntry implements CandidateEntry {
        private final NoteEntry note;

        NoteItemCandidateEntry(NoteEntry note) {
            this.note = note;
        }

        @NonNull
        @Override
        public String getTitle() {
            if (note.isRegularNote()) {
                return "• " + note.content;
            }
            String checkMark = note.isCompleted ? "[✓] " : "[ ] ";
            return checkMark + note.content;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                if (note.isRegularNote()) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Note", note.content);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, "Copied note to clipboard", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    NotePreferences.getInstance(activity).toggleNoteCompleted(note.id, note.isCompleted);
                    activity.refreshSearch();
                }
            };
        }

        @Override
        public void onLongClick(MainActivity activity) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(note.content);

            if (note.isRegularNote()) {
                builder.setItems(new CharSequence[]{"Edit note", "Convert to To-Do", "Delete note"}, (dialog, which) -> {
                    if (which == 0) { // Edit
                        showEditDialog(activity);
                    } else if (which == 1) { // Convert to To-Do
                        NotePreferences.getInstance(activity).updateNoteType(note.id, 0);
                        activity.refreshSearch();
                    } else if (which == 2) { // Delete
                        NotePreferences.getInstance(activity).deleteNote(note.id);
                        Toast.makeText(activity, "Note deleted", Toast.LENGTH_SHORT).show();
                        activity.refreshSearch();
                    }
                });
            } else {
                CharSequence toggleText = note.isCompleted ? "Mark as incomplete" : "Mark as completed";
                builder.setItems(new CharSequence[]{toggleText, "Convert to Regular Note", "Edit note", "Delete note"}, (dialog, which) -> {
                    if (which == 0) { // Toggle
                        NotePreferences.getInstance(activity).toggleNoteCompleted(note.id, note.isCompleted);
                        activity.refreshSearch();
                    } else if (which == 1) { // Convert to Regular Note
                        NotePreferences.getInstance(activity).updateNoteType(note.id, -1);
                        activity.refreshSearch();
                    } else if (which == 2) { // Edit
                        showEditDialog(activity);
                    } else if (which == 3) { // Delete
                        NotePreferences.getInstance(activity).deleteNote(note.id);
                        Toast.makeText(activity, "Note deleted", Toast.LENGTH_SHORT).show();
                        activity.refreshSearch();
                    }
                });
            }
            builder.show();
        }

        private void showEditDialog(MainActivity activity) {
            AlertDialog.Builder editBuilder = new AlertDialog.Builder(activity);
            editBuilder.setTitle("Edit Note");

            final EditText input = new EditText(activity);
            input.setText(note.content);
            LinearLayout layout = new LinearLayout(activity);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 16, 32, 16);
            layout.addView(input);

            editBuilder.setView(layout);
            editBuilder.setPositiveButton(R.string.button_ok, (dialog1, which1) -> {
                String newContent = input.getText().toString().trim();
                if (!newContent.isEmpty()) {
                    NotePreferences.getInstance(activity).updateNoteContent(note.id, newContent);
                    activity.refreshSearch();
                }
            });
            editBuilder.setNegativeButton(R.string.button_cancel, (dialog1, which1) -> dialog1.cancel());
            editBuilder.show();
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
            if (note.isRegularNote()) {
                return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_edit);
            }
            int iconRes = note.isCompleted ? android.R.drawable.checkbox_on_background : android.R.drawable.checkbox_off_background;
            return ContextCompat.getDrawable(context, iconRes);
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

    private static class AddNoteCandidateEntry implements CandidateEntry {
        private final String content;
        private final int isCompletedStatus; // -1 for regular note, 0 for to-do

        AddNoteCandidateEntry(String content, int isCompletedStatus) {
            this.content = content;
            this.isCompletedStatus = isCompletedStatus;
        }

        @NonNull
        @Override
        public String getTitle() {
            if (isCompletedStatus == -1) {
                return "+ Add regular note: " + content;
            }
            return "+ Add to-do item: " + content;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                NotePreferences.getInstance(activity).addNote(content, isCompletedStatus);
                String msg = isCompletedStatus == -1 ? "Note added" : "To-do added";
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
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

    private static class DeleteNoteCandidateEntry implements CandidateEntry {
        private final NoteEntry note;

        DeleteNoteCandidateEntry(NoteEntry note) {
            this.note = note;
        }

        @NonNull
        @Override
        public String getTitle() {
            return "- Delete note: " + note.content;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                NotePreferences.getInstance(activity).deleteNote(note.id);
                Toast.makeText(activity, "Note deleted", Toast.LENGTH_SHORT).show();
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

    private static class ClearCompletedNotesCandidateEntry implements CandidateEntry {
        @NonNull
        @Override
        public String getTitle() {
            return "- Clear completed to-dos";
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                NotePreferences.getInstance(activity).clearCompletedNotes();
                Toast.makeText(activity, "Cleared completed to-dos", Toast.LENGTH_SHORT).show();
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
            return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
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

    private static class NoteAddInstructionCandidateEntry implements CandidateEntry {
        @NonNull
        @Override
        public String getTitle() {
            return "To add note/to-do: n <text>";
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
