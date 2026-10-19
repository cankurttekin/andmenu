package com.cankurttekin.andmenu.commandSearchers;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.cankurttekin.andmenu.commandSearchers.eachSearcher.BookmarkCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.NoteCommandSearcher;
import com.cankurttekin.andmenu.dataStore.persistent.BookmarkPreferences;
import com.cankurttekin.andmenu.dataStore.persistent.NotePreferences;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BookmarkAndNoteCommandSearcherTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        BookmarkPreferences.destroyFilesForCleanTest(context);
        NotePreferences.destroyFilesForCleanTest(context);
    }

    @Test
    public void testBookmarkSearcherAddAndSearch() {
        BookmarkPreferences db = BookmarkPreferences.getInstance(context);
        db.addBookmark("GitHub", "https://github.com");

        BookmarkCommandSearcher searcher = new BookmarkCommandSearcher();

        // Search "b" without space should yield no results
        List<CandidateEntry> noResults = searcher.searchCandidateEntries("b", context);
        assertEquals(0, noResults.size());

        // Search "b " with space should list bookmarks
        List<CandidateEntry> results = searcher.searchCandidateEntries("b ", context);
        assertTrue(results.size() >= 2); // Bookmark item + instruction
        assertTrue(results.get(0).getTitle().contains("GitHub"));

        // Search "b git"
        List<CandidateEntry> filtered = searcher.searchCandidateEntries("b git", context);
        assertFalse(filtered.isEmpty());
        assertTrue(filtered.get(0).getTitle().contains("GitHub"));
    }

    @Test
    public void testNoteSearcherAddAndSearch() {
        NotePreferences db = NotePreferences.getInstance(context);
        db.addNote("Buy milk");

        NoteCommandSearcher searcher = new NoteCommandSearcher();

        // Search "n" without space should yield no results
        List<CandidateEntry> noResults = searcher.searchCandidateEntries("n", context);
        assertEquals(0, noResults.size());

        // Search "n " with space should list notes
        List<CandidateEntry> results = searcher.searchCandidateEntries("n ", context);
        assertTrue(results.size() >= 2); // Note item + instruction
        assertTrue(results.get(0).getTitle().contains("Buy milk"));

        // Search "n buy"
        List<CandidateEntry> filtered = searcher.searchCandidateEntries("n buy", context);
        assertFalse(filtered.isEmpty());
        assertTrue(filtered.get(0).getTitle().contains("Buy milk"));
    }
}
