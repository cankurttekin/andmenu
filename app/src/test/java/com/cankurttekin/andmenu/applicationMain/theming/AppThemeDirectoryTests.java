package com.cankurttekin.andmenu.applicationMain.theming;

import static org.junit.Assert.assertTrue;

import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.DynamicDarkTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.DynamicLightTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.DynamicTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.PastelDarkTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.PastelLightTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.PastelPinkDarkTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.PastelPinkLightTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.PastelPinkTheme;
import com.cankurttekin.andmenu.applicationMain.theming.eachTheme.PastelTheme;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class AppThemeDirectoryTests {
    @Test
    public void minimumAppThemeDirectoryFunctionalityTest() {
        CharSequence[] keys = AppThemeDirectory.getThemePreferenceKeys();
        Set<CharSequence> tmp = new HashSet<>();

        for (CharSequence key: keys) {
            if (tmp.contains(key)) {
                throw new RuntimeException("Theme ID conflicts");
            }
            tmp.add(key);
        }

        assertTrue("Pastel theme should be loaded", AppThemeDirectory.loadAppTheme("pastel") instanceof PastelTheme);
        assertTrue("Pastel light theme should be loaded", AppThemeDirectory.loadAppTheme("pastel_light") instanceof PastelLightTheme);
        assertTrue("Pastel dark theme should be loaded", AppThemeDirectory.loadAppTheme("pastel_dark") instanceof PastelDarkTheme);

        assertTrue("Pastel pink theme should be loaded", AppThemeDirectory.loadAppTheme("pastel_pink") instanceof PastelPinkTheme);
        assertTrue("Pastel pink light theme should be loaded", AppThemeDirectory.loadAppTheme("pastel_pink_light") instanceof PastelPinkLightTheme);
        assertTrue("Pastel pink dark theme should be loaded", AppThemeDirectory.loadAppTheme("pastel_pink_dark") instanceof PastelPinkDarkTheme);

        assertTrue("Dynamic theme should be loaded", AppThemeDirectory.loadAppTheme("dynamic") instanceof DynamicTheme);
        assertTrue("Dynamic light theme should be loaded", AppThemeDirectory.loadAppTheme("dynamic_light") instanceof DynamicLightTheme);
        assertTrue("Dynamic dark theme should be loaded", AppThemeDirectory.loadAppTheme("dynamic_dark") instanceof DynamicDarkTheme);
        assertTrue("Material You alias should resolve to DynamicTheme", AppThemeDirectory.loadAppTheme("material_you") instanceof DynamicTheme);
    }
}
