package com.cankurttekin.andmenu.applicationMain.theming.eachTheme;

import android.content.Context;

import androidx.annotation.StringRes;

import com.cankurttekin.andmenu.R;

public class PastelTheme extends DmenuTheme {
    private static final String THEME_ID = "pastel";
    private static final @StringRes int THEME_TITLE_STRING_RES = R.string.theme_name_pastel;

    @Override
    protected int getDarkThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemePastelDarkHome : R.style.AppThemePastelDark;
    }

    @Override
    protected int getLightThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemePastelHome : R.style.AppThemePastel;
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
