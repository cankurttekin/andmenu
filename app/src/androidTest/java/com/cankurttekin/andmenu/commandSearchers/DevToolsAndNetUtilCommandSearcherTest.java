package com.cankurttekin.andmenu.commandSearchers;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.cankurttekin.andmenu.commandSearchers.eachSearcher.DevToolsCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.NetUtilCommandSearcher;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DevToolsAndNetUtilCommandSearcherTest {
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(DevToolsCommandSearcher.PREF_DEVTOOLS_SEARCH_ENABLED_KEY, true)
                .apply();
    }

    @Test
    public void testDevToolsToggleDisabled() {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(DevToolsCommandSearcher.PREF_DEVTOOLS_SEARCH_ENABLED_KEY, false)
                .apply();

        DevToolsCommandSearcher searcher = new DevToolsCommandSearcher();
        List<CandidateEntry> results = searcher.searchCandidateEntries("gen pass", context);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testDevToolsPasswordGenerator() {
        DevToolsCommandSearcher searcher = new DevToolsCommandSearcher();

        List<CandidateEntry> results16 = searcher.searchCandidateEntries("gen pass", context);
        assertEquals(1, results16.size());
        assertTrue(results16.get(0).getTitle().startsWith("Generated Password (16 chars):"));

        List<CandidateEntry> results24 = searcher.searchCandidateEntries("gen pass 24", context);
        assertEquals(1, results24.size());
        assertTrue(results24.get(0).getTitle().startsWith("Generated Password (24 chars):"));

        List<CandidateEntry> resultsPass = searcher.searchCandidateEntries("pass 10", context);
        assertEquals(1, resultsPass.size());
        assertTrue(resultsPass.get(0).getTitle().startsWith("Generated Password (10 chars):"));
    }

    @Test
    public void testDevToolsBase64() {
        DevToolsCommandSearcher searcher = new DevToolsCommandSearcher();

        List<CandidateEntry> encResults = searcher.searchCandidateEntries("b64enc Hello World!", context);
        assertEquals(1, encResults.size());
        assertEquals("Base64 Encoded: SGVsbG8gV29ybGQh", encResults.get(0).getTitle());

        List<CandidateEntry> decResults = searcher.searchCandidateEntries("b64dec SGVsbG8gV29ybGQh", context);
        assertEquals(1, decResults.size());
        assertEquals("Base64 Decoded: Hello World!", decResults.get(0).getTitle());

        List<CandidateEntry> invResults = searcher.searchCandidateEntries("b64dec !!!notbase64!!!", context);
        assertEquals(1, invResults.size());
        assertEquals("Base64 Decoded: Invalid Base64 string", invResults.get(0).getTitle());
    }

    @Test
    public void testDevToolsUuid() {
        DevToolsCommandSearcher searcher = new DevToolsCommandSearcher();

        List<CandidateEntry> uuidResults = searcher.searchCandidateEntries("uuid", context);
        assertEquals(1, uuidResults.size());
        assertTrue(uuidResults.get(0).getTitle().startsWith("Generated UUID: "));
    }

    @Test
    public void testDevToolsHash() {
        DevToolsCommandSearcher searcher = new DevToolsCommandSearcher();

        List<CandidateEntry> md5Results = searcher.searchCandidateEntries("md5 test", context);
        assertEquals(1, md5Results.size());
        assertEquals("MD5 Hash: 098f6bcd4621d373cade4e832627b4f6", md5Results.get(0).getTitle());

        List<CandidateEntry> hashResults = searcher.searchCandidateEntries("hash test", context);
        assertEquals(4, hashResults.size());
        assertTrue(hashResults.get(0).getTitle().startsWith("MD5 Hash: "));
        assertTrue(hashResults.get(1).getTitle().startsWith("SHA-1 Hash: "));
        assertTrue(hashResults.get(2).getTitle().startsWith("SHA-256 Hash: "));
        assertTrue(hashResults.get(3).getTitle().startsWith("SHA-512 Hash: "));
    }

    @Test
    public void testNetUtilIpCommandSpaceTriggerOnly() {
        NetUtilCommandSearcher searcher = new NetUtilCommandSearcher();

        // Typing "ip" without trailing space -> no results
        List<CandidateEntry> noResults = searcher.searchCandidateEntries("ip", context);
        assertTrue(noResults.isEmpty());

        // Typing "ip " with trailing space -> returns results
        List<CandidateEntry> ipResults = searcher.searchCandidateEntries("ip ", context);
        assertNotNull(ipResults);
        assertFalse(ipResults.isEmpty());

        // Check that IP result entry has long view enabled like weather card
        assertEquals("ip", ipResults.get(0).getTitle());
        assertTrue(ipResults.get(0).hasLongView());
    }

    @Test
    public void testNetUtilDnsCommand() {
        NetUtilCommandSearcher searcher = new NetUtilCommandSearcher();

        List<CandidateEntry> dnsUsage = searcher.searchCandidateEntries("dns", context);
        assertNotNull(dnsUsage);
        assertFalse(dnsUsage.isEmpty());

        List<CandidateEntry> dnsLookup = searcher.searchCandidateEntries("dns google.com", context);
        assertNotNull(dnsLookup);
        assertEquals(1, dnsLookup.size());
        assertTrue(dnsLookup.get(0).getTitle().contains("DNS (google.com)"));
    }
}
