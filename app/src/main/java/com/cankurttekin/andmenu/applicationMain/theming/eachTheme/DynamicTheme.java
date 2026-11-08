package com.cankurttekin.andmenu.applicationMain.theming.eachTheme;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.StringRes;

import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.applicationMain.BaseWindowActivity;

public class DynamicTheme extends DmenuTheme {
    private static final String THEME_ID = "dynamic";
    private static final @StringRes int THEME_TITLE_STRING_RES = R.string.theme_name_dynamic;

    @Override
    protected int getDarkThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemeDynamicDarkHome : R.style.AppThemeDynamicDark;
    }

    @Override
    protected int getLightThemeStyleRes(boolean isHome) {
        return isHome ? R.style.AppThemeDynamicHome : R.style.AppThemeDynamic;
    }

    @Override
    public void apply(BaseWindowActivity activity) {
        super.apply(activity);

        // On API 27-30 (Android 8.1 - 11), wallpaper colors can be queried via WallpaperColors
        // and dynamically applied to the header and footer accent bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(activity);
                WallpaperColors colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                if (colors != null && colors.getPrimaryColor() != null) {
                    int primary = colors.getPrimaryColor().toArgb();
                    View header = activity.findViewById(R.id.baseWindowHeaderWrapper);
                    if (header != null) {
                        header.setBackgroundColor(primary);
                    }
                    View footer = activity.findViewById(R.id.baseWindowFooterWrapper);
                    if (footer != null) {
                        footer.setBackgroundColor(primary);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void onCreateFinal(BaseWindowActivity activity) {
        super.onCreateFinal(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(activity);
                WallpaperManager.OnColorsChangedListener listener = (colors, which) -> {
                    if ((which & WallpaperManager.FLAG_SYSTEM) != 0) {
                        activity.runOnUiThread(activity::recreate);
                    }
                };
                wm.addOnColorsChangedListener(listener, new Handler(Looper.getMainLooper()));
            } catch (Throwable ignored) {
            }
        }
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
