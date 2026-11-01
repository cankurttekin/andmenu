package com.cankurttekin.andmenu.commandSearchers;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.cankurttekin.andmenu.commandSearchers.eachSearcher.DictionaryCommandSearcher;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DictionaryCommandSearcherTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testDictionaryCommandSearcherActionOnEnter() {
        DictionaryCommandSearcher searcher = new DictionaryCommandSearcher();

        // Typing "define" without space -> no results
        List<CandidateEntry> noResults = searcher.searchCandidateEntries("define", context);
        assertEquals(0, noResults.size());

        // Typing "define " with space -> instruction entry
        List<CandidateEntry> instructions = searcher.searchCandidateEntries("define ", context);
        assertEquals(1, instructions.size());
        assertTrue(instructions.get(0).getTitle().contains("define <word>"));

        // Typing "define hello" -> returns Define "hello" action entry without network request
        List<CandidateEntry> actionResults = searcher.searchCandidateEntries("define hello", context);
        assertEquals(1, actionResults.size());
        assertTrue(actionResults.get(0).getTitle().contains("Define \"hello\""));
        assertTrue(actionResults.get(0).hasEvent());
    }
}
