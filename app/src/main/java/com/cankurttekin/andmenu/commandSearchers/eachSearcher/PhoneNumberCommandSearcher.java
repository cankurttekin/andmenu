package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;

import androidx.annotation.NonNull;

import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.applicationMain.MainActivity;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;

import java.util.ArrayList;
import java.util.List;

public class PhoneNumberCommandSearcher implements CommandSearcher {
    @Override
    public void refresh(Context context) {
    }

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();

        if (isPhoneNumber(query)) {
            candidates.add(new CallCandidateEntry(query, context));
            candidates.add(new AddContactCandidateEntry(query, context));
        }

        return candidates;
    }

    private boolean isPhoneNumber(String query) {
        if (query.length() < 3) return false;
        int digitCount = 0;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (Character.isDigit(c)) {
                digitCount++;
            } else if (c == '+') {
                if (i != 0) return false;
            } else if (c != '-' && c != '(' && c != ')' && c != ' ' && c != '.') {
                return false;
            }
        }
        // At least 3 digits to be considered a phone number
        return digitCount >= 3;
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

    private static class CallCandidateEntry implements CandidateEntry {
        private final String phoneNumber;
        private final String title;

        public CallCandidateEntry(String phoneNumber, Context context) {
            this.phoneNumber = phoneNumber;
            this.title = String.format(context.getString(R.string.contacts_action_dial_phone_number), phoneNumber);
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
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phoneNumber)));
                activity.startActivity(intent);
                activity.finish();
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

    private static class AddContactCandidateEntry implements CandidateEntry {
        private final String phoneNumber;
        private final String title;

        public AddContactCandidateEntry(String phoneNumber, Context context) {
            this.phoneNumber = phoneNumber;
            this.title = String.format(context.getString(R.string.contacts_action_add_contact), phoneNumber);
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
                Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber);
                activity.startActivity(intent);
                activity.finish();
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
