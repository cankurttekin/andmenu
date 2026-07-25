package com.cankurttekin.andmenu.commandSearchers;

import android.content.Context;

import com.cankurttekin.andmenu.commandSearchers.eachSearcher.ApplicationCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.CalendarCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.CalculatorCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.ColorDisplayCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.ContactSearchCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.DateCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.FactorCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.HelpCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.NetUtilCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.PhoneNumberCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.PreferencesCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.RunModeCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.SearchEngineCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.SearchEngineDefaultCommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.URICommandSearcher;
import com.cankurttekin.andmenu.commandSearchers.eachSearcher.WidgetCommandSearcher;
import com.cankurttekin.andmenu.dataStore.persistent.HomeScreenSetting;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.wrapperForAndroid.AppWidgetsHostManager;

import java.util.List;
import java.util.ArrayList;

public class CommandSearchAggregator {
    private final List <CommandSearcher> commandSearcherList = new ArrayList<>();
    private final List <CommandSearcher> commandSearcherListAlwaysLast = new ArrayList<>();

    private AppWidgetsHostManager appWidgetsHostManager = null;


    public CommandSearchAggregator(Context context) {
        // Starting with specific string
        commandSearcherList.add(new HelpCommandSearcher());
        commandSearcherList.add(new PreferencesCommandSearcher());
        commandSearcherList.add(new RunModeCommandSearcher());
        commandSearcherList.add(new DateCommandSearcher());
        commandSearcherList.add(new URICommandSearcher());
        commandSearcherList.add(new PhoneNumberCommandSearcher());
        commandSearcherList.add(new NetUtilCommandSearcher());
        commandSearcherList.add(new CalendarCommandSearcher());
        commandSearcherList.add(new FactorCommandSearcher());

        // Fully user-defined
        commandSearcherList.add(new WidgetCommandSearcher());

        // First character is limited
        commandSearcherList.add(new CalculatorCommandSearcher());
        commandSearcherList.add(new SearchEngineCommandSearcher(context));
        commandSearcherList.add(new ColorDisplayCommandSearcher());

        // Command searchers which may return tons candidate should comes to the last of "search result"
        commandSearcherList.add(new ContactSearchCommandSearcher());
        commandSearcherList.add(new ApplicationCommandSearcher());

        // This should be separately called and order does not matter, and results are placed at last.
        commandSearcherListAlwaysLast.add(new SearchEngineDefaultCommandSearcher(context));

        refresh(context);
    }

    public void close() {
        for (CommandSearcher cs : commandSearcherList) {
            cs.close();
        }
    }

    // May just start thread. In sequential execution it may be better to call this earlier.
    // Returns so early that users can wait.
    public void refresh(Context context) {
        for (CommandSearcher cs : commandSearcherList) {
            cs.refresh(context);
        }
        this.appWidgetsHostManager = new AppWidgetsHostManager(context);
    }

    public boolean isPrepared() {
        for (CommandSearcher cs : commandSearcherList) {
            if (!cs.isPrepared()) {
                return false;
            }
        }
        return true;
    }

    public void waitUntilPrepared() {
        for (CommandSearcher cs : commandSearcherList) {
            cs.waitUntilPrepared();
        }
    }

    public List<CandidateEntry> searchCandidateEntries(String s, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();
        if (s.isEmpty()) {
            return candidates;
        }

        for (CommandSearcher cs : commandSearcherList) {
            candidates.addAll(cs.searchCandidateEntries(s, context));
        }

        return candidates;
    }

    public List<CandidateEntry> searchCandidateEntriesForLast(String s, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();
        if (s.isEmpty()) {
            return candidates;
        }

        for (CommandSearcher cs : commandSearcherListAlwaysLast) {
            candidates.addAll(cs.searchCandidateEntries(s, context));
        }

        return candidates;
    }

    public List<CandidateEntry> homeScreenDefaultCandidateEntries(Context context) {
        List<HomeScreenSetting.HomeScreenDefaultItem> homeScreenDefaultItemList = HomeScreenSetting.getInstance(context).getAllHomeScreenDefaultItems();

        List<CandidateEntry> ret = new ArrayList<>();

        List<AppWidgetsHostManager.HomeScreenWidgetViewItem> widgetInfoList = this.appWidgetsHostManager.createHomeScreenWidgets();

        int widgetInfoListId = 0;

        for (HomeScreenSetting.HomeScreenDefaultItem item: homeScreenDefaultItemList) {
            while (widgetInfoListId < widgetInfoList.size() && widgetInfoList.get(widgetInfoListId).afterDefaultItem < item.id) {
                ret.add(new WidgetCommandSearcher.WidgetCandidateEntry(widgetInfoList.get(widgetInfoListId).widgetView));
                ++widgetInfoListId;
            }

            ret.addAll(this.searchCandidateEntries(item.data, context));
        }

        while (widgetInfoListId < widgetInfoList.size()) {
            ret.add(new WidgetCommandSearcher.WidgetCandidateEntry(widgetInfoList.get(widgetInfoListId).widgetView));
            ++widgetInfoListId;
        }

        return ret;
    }
}

