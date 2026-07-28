package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.preference.PreferenceManager;

import com.cankurttekin.andmenu.applicationMain.MainActivity;
import com.cankurttekin.andmenu.commandSearchers.lib.StringMatchStrategy;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;
import com.cankurttekin.andmenu.wrapperForAndroid.MusicReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicCommandSearcher implements CommandSearcher {
    public static final String PREF_MUSIC_SEARCH_ENABLED_KEY = "pref_music_search_enabled";

    private List<MusicReader.Song> songList = null;
    private boolean preparationCompleted = false;
    private final List<Thread> waitingThreads = new ArrayList<>();
    private Thread loader;

    @Override
    public void refresh(final Context context) {
        this.cancelAnyRefreshJob();

        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_MUSIC_SEARCH_ENABLED_KEY, false)) {
            this.songList = new ArrayList<>();
            this.setPreparationCompleted();
            return;
        }

        this.preparationCompleted = false;

        loader = new Thread() {
            @Override
            public void run() {
                refreshDatabase(context);
            }
        };
        loader.start();
    }

    private void refreshDatabase(Context context) {
        try {
            this.songList = MusicReader.fetchAllSongs(context);
        } catch (MusicReader.MusicReadPermissionDenied e) {
            this.songList = new ArrayList<>();
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            prefEdit.putBoolean(PREF_MUSIC_SEARCH_ENABLED_KEY, false);
            prefEdit.apply();
        }
        this.setPreparationCompleted();
    }

    private synchronized void cancelAnyRefreshJob() {
        for (Thread th : waitingThreads) {
            th.interrupt();
        }
        this.waitingThreads.clear();

        if (loader != null) {
            loader.interrupt();
            loader = null;
        }
    }

    private synchronized void setPreparationCompleted() {
        this.preparationCompleted = true;
        for (Thread th : waitingThreads) {
            th.interrupt();
        }
        this.waitingThreads.clear();
    }

    private synchronized void registerWaitingThread(Thread thread) {
        if (this.preparationCompleted) {
            thread.interrupt();
            return;
        }
        waitingThreads.add(thread);
    }

    @Override
    public void close() {
        this.cancelAnyRefreshJob();
        this.songList = null;
    }

    @Override
    public boolean isPrepared() {
        return preparationCompleted;
    }

    @Override
    public void waitUntilPrepared() {
        Thread th = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        th.start();
        registerWaitingThread(th);
        try {
            th.join();
        } catch (InterruptedException e) {
        }
    }

    @NonNull
    @Override
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_MUSIC_SEARCH_ENABLED_KEY, false)) {
            return new ArrayList<>();
        }

        if (!query.toLowerCase().startsWith("m ")) {
            return new ArrayList<>();
        }

        String actualQuery = query.substring(2).trim();
        if (actualQuery.isEmpty()) {
            return new ArrayList<>();
        }

        List<Pair<Integer, MusicReader.Song>> resultList = new ArrayList<>();
        for (MusicReader.Song song : songList) {
            int match = judgeQueryForSong(context, actualQuery, song);
            if (match >= 0) {
                resultList.add(new Pair<>(match, song));
            }
        }

        Collections.sort(resultList, (o1, o2) -> o1.first.compareTo(o2.first));

        List<CandidateEntry> ret = new ArrayList<>();
        for (Pair<Integer, MusicReader.Song> songPair : resultList) {
            ret.add(new MusicCandidateEntry(songPair.second));
        }
        return ret;
    }

    private static int judgeQueryForSong(Context context, String query, MusicReader.Song song) {
        int titleMatch = StringMatchStrategy.match(context, query, song.title, false);
        if (titleMatch >= 0) {
            return titleMatch;
        }

        int artistMatch = StringMatchStrategy.match(context, query, song.artist, false);
        if (artistMatch >= 0) {
            return artistMatch + 1000000;
        }

        int albumMatch = StringMatchStrategy.match(context, query, song.album, false);
        if (albumMatch >= 0) {
            return albumMatch + 2000000;
        }

        return -1;
    }

    private static class MusicCandidateEntry implements CandidateEntry {
        private final MusicReader.Song song;

        private MusicCandidateEntry(MusicReader.Song song) {
            this.song = song;
        }

        @NonNull
        @Override
        public String getTitle() {
            return this.song.title + " - " + this.song.artist;
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
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(song.uri, "audio/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
