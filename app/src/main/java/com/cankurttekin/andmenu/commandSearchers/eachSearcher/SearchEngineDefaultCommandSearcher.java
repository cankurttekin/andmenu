package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cankurttekin.andmenu.commands.urls.WebSearchEngine;
import com.cankurttekin.andmenu.commands.urls.WebSearchEnginesDatabase;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.lib.StringValidator;

import java.util.ArrayList;
import java.util.List;

public class SearchEngineDefaultCommandSearcher implements CommandSearcher {
    private final WebSearchEnginesDatabase _searchEngineDB;

    public SearchEngineDefaultCommandSearcher(Context context) {
        this._searchEngineDB = new WebSearchEnginesDatabase(context);
        this.refresh(context);
    }

    @Override
    public void refresh(Context context) {
        this._searchEngineDB.refresh(context);
    }

    @Override
    public void close() {}

    @Override
    public void waitUntilPrepared() {}

    @Override
    public boolean isPrepared() {
        return true;
    }

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();

        // If it's a full URL with scheme, URICommandSearcher already handled it and suppressed search.
        if (StringValidator.isValidURLAccepted(query, false, context)) {
            return candidates;
        }

        // If it looks like a URL but has no scheme (e.g. reddit.com), 
        // we show an "Open website" option but DON'T suppress the search engine.
        if (StringValidator.isWebURLWithoutScheme(query)) {
            candidates.add(new URICommandSearcher.URLOpenCandidateEntry("https://" + query, "https://" + query));
        }

        WebSearchEngine searchEngine = this._searchEngineDB.getDefaultEngineByPreference(context);

        if (searchEngine != null) {
            candidates.add(new SearchEngineCommandSearcher.SearchEngineCandidateEntry(context, query, searchEngine.display_name, searchEngine.url_base));
        }
        return candidates;
    }
}
