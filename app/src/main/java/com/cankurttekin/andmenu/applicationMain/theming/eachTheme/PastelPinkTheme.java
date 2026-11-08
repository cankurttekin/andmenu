package com.cankurttekin.andmenu.applicationMain.theming.eachTheme;

import android.content.Context;

import androidx.annotation.StringRes;

import com.cankurttekin.andmenu.R;

public class PastelPinkTheme extends DmenuTheme {
    private static final String THEME_ID = "pastel_pink";
    private static final @StringRes int THEME_TITLE_STRING_RES = R.string.theme_name_pastel_pink;

    @Override
    protected int getDarkThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemePastelPinkDarkHome : R.style.AppThemePastelPinkDark;
    }

    @Override
    protected int getLightThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemePastelPinkHome : R.style.AppThemePastelPink;
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
