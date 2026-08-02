package com.cankurttekin.andmenu.applicationMain.theming;

import android.content.Context;
import android.preference.PreferenceManager;

import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.AndmenuDarkTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.AndmenuDefaultTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.AndmenuLightTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.AmoledTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.DmenuDarkTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.DmenuLightTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.DmenuTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.GruvboxTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.MarineTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.OldComputerTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.RosePineTheme;

import java.util.HashMap;
import java.util.Map;


public class AppThemeDirectory {
    public static final String PREF_NAME_THEME = "pref_appearance_theme";

    private static final AppTheme[] THEMES = {
            new DmenuTheme(),
            new DmenuLightTheme(),
            new DmenuDarkTheme(),
            new AndmenuDefaultTheme(),
            new AndmenuLightTheme(),
            new AndmenuDarkTheme(),
            new MarineTheme(),
            new OldComputerTheme(),
            new GruvboxTheme(),
            new RosePineTheme(),
            new AmoledTheme(),
    };

    private static Map<String, AppTheme> themeMap;


    public static CharSequence[] getThemePreferenceKeys() {
        CharSequence[] ret = new CharSequence[THEMES.length];
        for (int i = 0; i < THEMES.length; ++i) {
            ret[i] = THEMES[i].getThemeID();
        }
        return ret;
    }

    public static CharSequence[] getThemePreferenceTitles(Context context) {
        CharSequence[] ret = new CharSequence[THEMES.length];
        for (int i = 0; i < THEMES.length; ++i) {
            ret[i] = THEMES[i].getThemeTitle(context);
        }
        return ret;
    }

    public static AppTheme loadAppTheme(Context context) {
        String themeName = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_NAME_THEME, "");
        return loadAppTheme(themeName);
    }

    public static AppTheme loadAppTheme(String themeName) {
        if (themeMap == null) {
            themeMap = new HashMap<>();
            for (AppTheme theme: THEMES) {
                themeMap.put(theme.getThemeID(), theme);
            }
        }

        if (themeName == null || themeName.isEmpty() || themeName.equals("default")) {
            return THEMES[0];
        }

        AppTheme ret = themeMap.get(themeName);
        if (ret != null) {
            return ret;
        }
        return THEMES[0];
    }
}
