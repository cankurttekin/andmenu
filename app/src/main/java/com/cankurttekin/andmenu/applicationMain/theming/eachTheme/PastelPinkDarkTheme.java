package com.cankurttekin.andmenu.applicationMain.theming.eachTheme;

import android.content.Context;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.cankurttekin.andmenu.R;

public class PastelPinkDarkTheme extends PastelPinkTheme {
    private static final String THEME_ID = "pastel_pink_dark";
    private static final @StringRes int THEME_TITLE_STRING_RES = R.string.theme_name_pastel_pink_dark;

    @Override
    protected void configureDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    @Override
    protected boolean isDarkMode(Context context) {
        return true;
    }

    @Override
    public String getThemeID() {
        return THEME_ID;
    }

    @Override
    public CharSequence getThemeTitle(Context context) {
        return context.getString(THEME_TITLE_STRING_RES);
    }
}
