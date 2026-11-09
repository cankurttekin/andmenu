package com.cankurttekin.andmenu.applicationMain.theming.eachTheme;

import android.content.Context;

import androidx.annotation.StringRes;

import com.cankurttekin.andmenu.R;

public class DmenuPastelTheme extends DmenuTheme {
    private static final String THEME_ID = "dmenu_pastel";
    private static final @StringRes int THEME_TITLE_STRING_RES = R.string.theme_name_dmenu_pastel;

    @Override
    protected int getDarkThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemeDmenuPastelDarkHome : R.style.AppThemeDmenuPastelDark;
    }

    @Override
    protected int getLightThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemeDmenuPastelHome : R.style.AppThemeDmenuPastel;
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
