package com.cankurttekin.andmenu.applicationMain.lib;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

public class StyleManager {
    public static final String PREF_CUSTOM_CSS = "pref_custom_css";

    private static JSONObject getStyles(Context context) {
        try {
            String json = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_CUSTOM_CSS, "{}");
            return new JSONObject(json);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static void applyTextViewStyles(TextView textView, String scope) {
        try {
            JSONObject styles = getStyles(textView.getContext());
            if (!styles.has("ui")) return;
            JSONObject ui = styles.getJSONObject("ui");

            String fontSizeKey = scope + "_font_size";
            if (ui.has(fontSizeKey)) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) ui.getDouble(fontSizeKey));
            }

            if (ui.has("font_family")) {
                String family = ui.getString("font_family");
                textView.setTypeface(Typeface.create(family, Typeface.NORMAL));
            }
            
            if (styles.has("colors")) {
                JSONObject colors = styles.getJSONObject("colors");
                String colorKey = scope + "_text_color";
                if (colors.has(colorKey)) {
                    textView.setTextColor(Color.parseColor(colors.getString(colorKey)));
                }
            }
        } catch (Exception ignored) {}
    }

    public static void applyViewPadding(View view, String scope) {
        try {
            JSONObject styles = getStyles(view.getContext());
            if (!styles.has("ui")) return;
            JSONObject ui = styles.getJSONObject("ui");

            float density = view.getContext().getResources().getDisplayMetrics().density;
            
            int pL = view.getPaddingLeft();
            int pT = view.getPaddingTop();
            int pR = view.getPaddingRight();
            int pB = view.getPaddingBottom();

            if (ui.has(scope + "_padding_v")) {
                int p = (int) (ui.getDouble(scope + "_padding_v") * density);
                pT = p; pB = p;
            }
            if (ui.has(scope + "_padding_h")) {
                int p = (int) (ui.getDouble(scope + "_padding_h") * density);
                pL = p; pR = p;
            }

            view.setPadding(pL, pT, pR, pB);

            if (ui.has(scope + "_height")) {
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                lp.height = (int) (ui.getDouble(scope + "_height") * density);
                view.setLayoutParams(lp);
            }
        } catch (Exception ignored) {}
    }

    public static Integer getCustomColor(Context context, String attrName) {
        try {
            JSONObject styles = getStyles(context);
            if (!styles.has("colors")) return null;
            JSONObject colors = styles.getJSONObject("colors");
            if (colors.has(attrName)) {
                return Color.parseColor(colors.getString(attrName));
            }
        } catch (Exception ignored) {}
        return null;
    }
}
