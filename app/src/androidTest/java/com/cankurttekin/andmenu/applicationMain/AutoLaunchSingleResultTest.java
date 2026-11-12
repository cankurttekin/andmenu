package com.cankurttekin.andmenu.applicationMain;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.cankurttekin.andmenu.commandSearchers.CommandSearchAggregator;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class AutoLaunchSingleResultTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefEdit.putBoolean("pref_apps_auto_launch_single_result", true);
        prefEdit.apply();
    }

    @Test
    public void testPrimaryCandidatesExcludesDefaultSearchEngine() {
        CommandSearchAggregator aggregator = new CommandSearchAggregator(context);
        aggregator.waitUntilPrepared();

        // Search engine default candidate is separated in searchCandidateEntriesForLast
        List<CandidateEntry> primary = aggregator.searchCandidateEntries("randomnonexistentquery123", context);
        List<CandidateEntry> last = aggregator.searchCandidateEntriesForLast("randomnonexistentquery123", context);

        assertEquals(0, primary.size());
        assertFalse(last.isEmpty());

        aggregator.close();
    }
}
