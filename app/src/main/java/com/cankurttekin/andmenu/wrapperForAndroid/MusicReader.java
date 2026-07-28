package com.cankurttekin.andmenu.wrapperForAndroid;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MusicReader {
    public static class Song {
        public long id;
        public String title;
        public String artist;
        public String album;
        public Uri uri;
    }

    public static class MusicReadPermissionDenied extends Exception {}

    public static boolean appHasReadMusicPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{Manifest.permission.READ_MEDIA_AUDIO};
        } else {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    public static List<Song> fetchAllSongs(Context context) throws MusicReadPermissionDenied {
        if (!appHasReadMusicPermission(context)) {
            throw new MusicReadPermissionDenied();
        }

        List<Song> songs = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);

            do {
                Song song = new Song();
                song.id = cursor.getLong(idColumn);
                song.title = cursor.getString(titleColumn);
                song.artist = cursor.getString(artistColumn);
                song.album = cursor.getString(albumColumn);
                song.uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(song.id));
                songs.add(song);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return songs;
    }
}
