package com.cankurttekin.andmenu.applicationMain;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.cankurttekin.andmenu.commandSearchers.CommandSearchAggregator;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class TrimLeadingWhitespaceTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testLeadingWhitespaceTrimming() {
        CommandSearchAggregator aggregator = new CommandSearchAggregator(context);
        aggregator.waitUntilPrepared();

        String rawQuery = "   b ";
        int i = 0;
        while (i < rawQuery.length() && Character.isWhitespace(rawQuery.charAt(i))) {
            i++;
        }
        String trimmedQuery = rawQuery.substring(i);

        List<CandidateEntry> results = aggregator.searchCandidateEntries(trimmedQuery, context);
        assertFalse(results.isEmpty());

        aggregator.close();
    }
}
