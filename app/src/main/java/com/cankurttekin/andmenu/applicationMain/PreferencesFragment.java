package com.cankurttekin.andmenu.applicationMain;

import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.cankurttekin.andmenu.BuildConfig;
import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.applicationMain.lib.ConfigManager;
import com.cankurttekin.andmenu.applicationMain.lib.EditTextConfigurations;
import com.cankurttekin.andmenu.applicationMain.theming.AppThemeDirectory;
import com.cankurttekin.andmenu.commandSearchers.lib.StringMatchStrategy;
import com.cankurttekin.andmenu.commands.urls.WebSearchEngine;
import com.cankurttekin.andmenu.commands.urls.WebSearchEnginesDatabase;
import com.cankurttekin.andmenu.widget.LauncherWidgetProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS;

public class PreferencesFragment extends PreferenceFragmentCompat {
    private final ActivityResultLauncher<String> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    exportToFile(uri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> openDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    importFromFile(uri);
                }
            }
    );

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        final List<WebSearchEngine> urlListForLocale = new WebSearchEnginesDatabase(PreferencesFragment.this.getContext()).getURLListForLocale(PreferencesFragment.this.getContext().getResources().getConfiguration().locale, true);

        int webSearchEngineCount = 0;
        for (WebSearchEngine e : urlListForLocale) {
            if (e.has_query) {
                ++webSearchEngineCount;
            }
        }

        CharSequence[] search_engine_entries = new CharSequence[webSearchEngineCount + 1];
        search_engine_entries[0] = getString(R.string.preferences_item_default_search_option_none);

        CharSequence[] search_engine_entry_values = new CharSequence[webSearchEngineCount + 1];
        search_engine_entry_values[0] = "none";

        int searchEnginePos = 1;

        for (WebSearchEngine e : urlListForLocale) {
            if (e.has_query) {
                search_engine_entry_values[searchEnginePos] = e.id_for_preference_value;
                search_engine_entries[searchEnginePos] = e.display_name_locale_independent;
                ++searchEnginePos;
            }
        }

        ((ListPreference) findPreference(WebSearchEnginesDatabase.PREF_KEY_DEFAULT_SEARCH)).setEntries(search_engine_entries);
        ((ListPreference) findPreference(WebSearchEnginesDatabase.PREF_KEY_DEFAULT_SEARCH)).setEntryValues(search_engine_entry_values);

        findPreference("dummy_pref_app_info").setSummary(String.format(this.getString(R.string.displayedFullVersionString), BuildConfig.VERSION_NAME));
        findPreference("dummy_pref_app_info").setSelectable(false);

        int stringMatchStrategySize = StringMatchStrategy.STRATEGY_LIST.length;

        CharSequence[] string_match_strategy_entries = new CharSequence[stringMatchStrategySize];
        CharSequence[] string_match_strategy_entry_values = new CharSequence[stringMatchStrategySize];

        for (int i = 0; i < stringMatchStrategySize; ++i) {
            string_match_strategy_entries[i] = StringMatchStrategy.getStrategyName(this.getActivity(), StringMatchStrategy.STRATEGY_LIST[i]);
            string_match_strategy_entry_values[i] = StringMatchStrategy.getStrategyPrefValue(StringMatchStrategy.STRATEGY_LIST[i]);
        }

        ((ListPreference) findPreference(StringMatchStrategy.PREF_NAME)).setEntries(string_match_strategy_entries);
        ((ListPreference) findPreference(StringMatchStrategy.PREF_NAME)).setEntryValues(string_match_strategy_entry_values);

        findPreference("pref_default_assist_app").setOnPreferenceClickListener(
                preference -> {
                    // Not a perfect behavior, main window disappears
                    // This config is not to be used everyday, it is enough if just not too confusing
                    ((PreferencesActivity) PreferencesFragment.this.getActivity()).setComingBackFlag();
                    Intent intent = new Intent(ACTION_VOICE_INPUT_SETTINGS);
                    PreferencesFragment.this.startActivity(intent);
                    return true;
                }
        );

        findPreference("pref_default_home_app").setOnPreferenceClickListener(
                preference -> {
                    ((PreferencesActivity) PreferencesFragment.this.getActivity()).setComingBackFlag();
                    Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                    PreferencesFragment.this.startActivity(intent);
                    return true;
                }
        );

        findPreference("pref_home_set_solid_color_wallpaper").setOnPreferenceClickListener(
                preference -> {
                    final EditText input = new EditText(getContext());
                    input.setHint("#000000");
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.preferences_item_home_set_solid_color_wallpaper_title)
                            .setView(input)
                            .setPositiveButton(R.string.button_ok, (dialog, which) -> {
                                try {
                                    int color = Color.parseColor(input.getText().toString());
                                    WallpaperManager wm = WallpaperManager.getInstance(getContext());
                                    int width = wm.getDesiredMinimumWidth();
                                    int height = wm.getDesiredMinimumHeight();
                                    if (width <= 0 || height <= 0) {
                                        width = getResources().getDisplayMetrics().widthPixels;
                                        height = getResources().getDisplayMetrics().heightPixels;
                                    }
                                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                                    Canvas canvas = new Canvas(bitmap);
                                    canvas.drawColor(color);
                                    wm.setBitmap(bitmap);
                                    Toast.makeText(getContext(), "Wallpaper set", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(getContext(), "Invalid color", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.button_cancel, null)
                            .show();
                    return true;
                }
        );

        findPreference("pref_home_set_wallpaper").setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                    startActivity(Intent.createChooser(intent, getString(R.string.preferences_item_home_set_wallpaper_title)));
                    return true;
                }
        );

        findPreference("pref_advanced_text_config").setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(getContext(), ConfigEditorActivity.class);
                    startActivity(intent);
                    return true;
                }
        );

        findPreference("pref_advanced_export_config").setOnPreferenceClickListener(
                preference -> {
                    createDocumentLauncher.launch("andmenu_config.json");
                    return true;
                }
        );

        findPreference("pref_advanced_import_config").setOnPreferenceClickListener(
                preference -> {
                    openDocumentLauncher.launch(new String[]{"application/json", "text/*", "*/*"});
                    return true;
                }
        );

        ((ListPreference) findPreference(AppThemeDirectory.PREF_NAME_THEME)).setEntries(AppThemeDirectory.getThemePreferenceTitles(this.getContext()));
        ((ListPreference) findPreference(AppThemeDirectory.PREF_NAME_THEME)).setEntryValues(AppThemeDirectory.getThemePreferenceKeys());

        findPreference(AppThemeDirectory.PREF_NAME_THEME).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    LauncherWidgetProvider.updateTheme(PreferencesFragment.this.getContext(), (String)newValue);
                    PreferencesFragment.this.getActivity().finish();
                    return true;
                }
        );

        if (Build.VERSION.SDK_INT < 24) {
            findPreference(EditTextConfigurations.PREF_KEY_MAIN_EDITTEXT_HINT_LOCALE_ENGLISH).setVisible(false);
        }
    }

    private void exportToFile(Uri uri) {
        try (OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                String json = ConfigManager.exportConfig(getContext());
                outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(getContext(), "Config exported to file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importFromFile(Uri uri) {
        try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            if (ConfigManager.importConfig(getContext(), sb.toString())) {
                Toast.makeText(getContext(), "Import successful from file", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Import failed: Invalid JSON", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
