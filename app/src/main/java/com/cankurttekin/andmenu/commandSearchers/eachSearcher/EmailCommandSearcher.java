package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.NonNull;

import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.applicationMain.MainActivity;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;

import java.util.ArrayList;
import java.util.List;

public class EmailCommandSearcher implements CommandSearcher {
    @Override
    public void refresh(Context context) {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isPrepared() {
        return true;
    }

    @Override
    public void waitUntilPrepared() {
    }

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();

        if (Patterns.EMAIL_ADDRESS.matcher(query).matches()) {
            candidates.add(new EmailSendCandidateEntry(query, context));
        }

        return candidates;
    }

    private static class EmailSendCandidateEntry implements CandidateEntry {
        private final String email;
        private final String title;

        public EmailSendCandidateEntry(String email, Context context) {
            this.email = email;
            this.title = String.format(context.getString(R.string.contacts_action_email), email);
        }

        @NonNull
        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + Uri.encode(email)));
                activity.startActivity(intent);
                activity.finishIfNotHome();
            };
        }

        @Override
        public Drawable getIcon(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return true;
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
}
