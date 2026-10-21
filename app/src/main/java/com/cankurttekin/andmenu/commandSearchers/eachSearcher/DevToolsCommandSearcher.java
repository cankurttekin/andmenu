package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.cankurttekin.andmenu.applicationMain.MainActivity;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DevToolsCommandSearcher implements CommandSearcher {
    public static final String PREF_DEVTOOLS_SEARCH_ENABLED_KEY = "pref_devtools_search_enabled";
    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";

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

        if (!androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_DEVTOOLS_SEARCH_ENABLED_KEY, false)) {
            return candidates;
        }

        String trimmed = query.trim();
        String lower = trimmed.toLowerCase();

        if (trimmed.isEmpty()) {
            return candidates;
        }

        // 1. Password Generator: "gen pass <length>", "genpass", "pass"
        boolean isPass = lower.equals("pass") || lower.startsWith("pass ")
                || lower.equals("genpass") || lower.startsWith("genpass ")
                || lower.equals("gen pass") || lower.startsWith("gen pass ");
        if (isPass) {
            int length = 16;
            String[] parts = trimmed.split("\\s+");
            if (parts.length > 1) {
                String lastPart = parts[parts.length - 1];
                try {
                    length = Integer.parseInt(lastPart);
                    if (length < 1) length = 16;
                    if (length > 256) length = 256;
                } catch (NumberFormatException ignored) {}
            }
            String password = generatePassword(length);
            candidates.add(new CopyResultCandidateEntry("Generated Password (" + length + " chars)", password));
            return candidates;
        }

        // 2. Base64 Encoder: "b64enc <text>", "base64enc <text>", "b64 <text>"
        if (lower.equals("b64enc") || lower.equals("base64enc") || lower.equals("b64")) {
            candidates.add(new CopyResultCandidateEntry("Base64 Encoder", "Usage: b64enc <text>", false));
            return candidates;
        }
        if (lower.startsWith("b64enc ") || lower.startsWith("base64enc ") || lower.startsWith("b64 ")) {
            int spaceIndex = trimmed.indexOf(' ');
            String text = trimmed.substring(spaceIndex + 1).trim();
            if (!text.isEmpty()) {
                String encoded = Base64.encodeToString(text.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                candidates.add(new CopyResultCandidateEntry("Base64 Encoded", encoded));
            } else {
                candidates.add(new CopyResultCandidateEntry("Base64 Encoder", "Usage: b64enc <text>", false));
            }
            return candidates;
        }

        // 3. Base64 Decoder: "b64dec <text>", "base64dec <text>"
        if (lower.equals("b64dec") || lower.equals("base64dec")) {
            candidates.add(new CopyResultCandidateEntry("Base64 Decoder", "Usage: b64dec <text>", false));
            return candidates;
        }
        if (lower.startsWith("b64dec ") || lower.startsWith("base64dec ")) {
            int spaceIndex = trimmed.indexOf(' ');
            String text = trimmed.substring(spaceIndex + 1).trim();
            if (!text.isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(text, Base64.DEFAULT);
                    String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
                    candidates.add(new CopyResultCandidateEntry("Base64 Decoded", decoded));
                } catch (Exception e) {
                    candidates.add(new CopyResultCandidateEntry("Base64 Decoded", "Invalid Base64 string", false));
                }
            } else {
                candidates.add(new CopyResultCandidateEntry("Base64 Decoder", "Usage: b64dec <text>", false));
            }
            return candidates;
        }

        // 4. UUID Generator: "uuid"
        if (lower.equals("uuid") || lower.startsWith("uuid ") || lower.startsWith("uuidv4")) {
            String uuid = UUID.randomUUID().toString();
            candidates.add(new CopyResultCandidateEntry("Generated UUID", uuid));
            return candidates;
        }

        // 5. Hash Generator: "hash <text>", "md5 <text>", "sha256 <text>", "sha1 <text>", "sha512 <text>"
        if (lower.equals("hash") || lower.equals("md5") || lower.equals("sha256") || lower.equals("sha1") || lower.equals("sha512")) {
            candidates.add(new CopyResultCandidateEntry("Hash Generator", "Usage: hash <text> | md5 <text> | sha256 <text>", false));
            return candidates;
        }
        if (lower.startsWith("hash ") || lower.startsWith("md5 ") || lower.startsWith("sha256 ") || lower.startsWith("sha1 ") || lower.startsWith("sha512 ")) {
            int spaceIndex = trimmed.indexOf(' ');
            String text = trimmed.substring(spaceIndex + 1).trim();
            if (!text.isEmpty()) {
                if (lower.startsWith("md5 ")) {
                    String md5 = computeHash(text, "MD5");
                    if (md5 != null) candidates.add(new CopyResultCandidateEntry("MD5 Hash", md5));
                } else if (lower.startsWith("sha256 ")) {
                    String sha256 = computeHash(text, "SHA-256");
                    if (sha256 != null) candidates.add(new CopyResultCandidateEntry("SHA-256 Hash", sha256));
                } else if (lower.startsWith("sha1 ")) {
                    String sha1 = computeHash(text, "SHA-1");
                    if (sha1 != null) candidates.add(new CopyResultCandidateEntry("SHA-1 Hash", sha1));
                } else if (lower.startsWith("sha512 ")) {
                    String sha512 = computeHash(text, "SHA-512");
                    if (sha512 != null) candidates.add(new CopyResultCandidateEntry("SHA-512 Hash", sha512));
                } else {
                    String md5 = computeHash(text, "MD5");
                    String sha1 = computeHash(text, "SHA-1");
                    String sha256 = computeHash(text, "SHA-256");
                    String sha512 = computeHash(text, "SHA-512");

                    if (md5 != null) candidates.add(new CopyResultCandidateEntry("MD5 Hash", md5));
                    if (sha1 != null) candidates.add(new CopyResultCandidateEntry("SHA-1 Hash", sha1));
                    if (sha256 != null) candidates.add(new CopyResultCandidateEntry("SHA-256 Hash", sha256));
                    if (sha512 != null) candidates.add(new CopyResultCandidateEntry("SHA-512 Hash", sha512));
                }
            } else {
                candidates.add(new CopyResultCandidateEntry("Hash Generator", "Usage: hash <text>", false));
            }
            return candidates;
        }

        return candidates;
    }

    private static String generatePassword(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(PASSWORD_CHARS.length());
            sb.append(PASSWORD_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    private static String computeHash(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static class CopyResultCandidateEntry implements CandidateEntry {
        private final String label;
        private final String value;
        private final boolean executable;

        CopyResultCandidateEntry(String label, String value) {
            this(label, value, true);
        }

        CopyResultCandidateEntry(String label, String value, boolean executable) {
            this.label = label;
            this.value = value;
            this.executable = executable;
        }

        @NonNull
        @Override
        public String getTitle() {
            if (value == null || value.isEmpty()) {
                return label;
            }
            return label + ": " + value;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            if (!executable) return null;
            return activity -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText(label, value);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Copied: " + value, Toast.LENGTH_SHORT).show();
                }
            };
        }

        @Override
        public boolean hasEvent() {
            return executable;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_edit);
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
