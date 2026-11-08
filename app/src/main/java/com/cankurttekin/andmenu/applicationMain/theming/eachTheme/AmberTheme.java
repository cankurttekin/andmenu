package com.cankurttekin.andmenu.applicationMain.theming.eachTheme;

import android.content.Context;

import androidx.annotation.StringRes;

import com.cankurttekin.andmenu.R;

public class AmberTheme extends DmenuTheme {
    private static final String THEME_ID = "amber";
    private static final @StringRes int THEME_TITLE_STRING_RES = R.string.theme_name_amber;

    @Override
    protected int getDarkThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemeAmberDarkHome : R.style.AppThemeAmberDark;
    }

    @Override
    protected int getLightThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemeAmberHome : R.style.AppThemeAmber;
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
