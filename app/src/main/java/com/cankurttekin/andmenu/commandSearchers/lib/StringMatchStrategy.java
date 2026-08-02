package com.cankurttekin.andmenu.commandSearchers.lib;

import android.content.Context;
import android.preference.PreferenceManager;

import com.cankurttekin.andmenu.R;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringMatchStrategy {
    public static final int SUBSTRING = 1;
    public static final int SKIPPED_SUBSTRING = 2;
    public static final int FUZZY = 3;

    public static final int[] STRATEGY_LIST = new int[]{ SUBSTRING, SKIPPED_SUBSTRING, FUZZY };

    public static final String PREF_NAME = "pref_text_match_strategy";

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static String normalize(String s) {
        if (s == null) return "";
        String nfdNormalizedString = Normalizer.normalize(s, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(nfdNormalizedString).replaceAll("");
    }

    public static CharSequence getStrategyName(Context context, int strategy) {
        switch (strategy) {
            case SUBSTRING:
                return context.getString(R.string.string_match_strategy_substring);

            case SKIPPED_SUBSTRING:
                return context.getString(R.string.string_match_strategy_skipped_substring);

            case FUZZY:
                return context.getString(R.string.string_match_strategy_fuzzy);

            default:
                throw new RuntimeException("Strategy not found");
        }
    }

    public static String getStrategyPrefValue(int strategy) {
        switch (strategy) {
            case SUBSTRING:
                return "substring";

            case SKIPPED_SUBSTRING:
                return "skipped_substring";

            case FUZZY:
                return "fuzzy";

            default:
                throw new RuntimeException("Strategy not found");
        }
    }

    public static int getStrategyPreference(Context context) {
        String name = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_NAME, "substring");

        if (name.equals("skipped_substring")) {
            return SKIPPED_SUBSTRING;
        }

        if (name.equals("fuzzy")) {
            return FUZZY;
        }

        return SUBSTRING; // default
    }

    /**
     * @return score that smaller is better, or -1 if not match
     */
    public static int match(Context context, String query, String target, boolean startingMustMatch) {
        return match(query, target, startingMustMatch, getStrategyPreference(context));
    }

    /**
     * @return score that smaller is better, or -1 if not match
     */
    public static int match(String query, String target, boolean startingMustMatch, int strategy) {
        if (query.length() == 0) {
            return -1;
        }

        String query_lower = normalize(query.toLowerCase());
        String target_lower = normalize(target.toLowerCase());

        if (strategy == SUBSTRING) {
            boolean ok = startingMustMatch ? target_lower.startsWith(query_lower)
                                           : target_lower.contains(query_lower);

            if (ok) {
                return target_lower.indexOf(query_lower);
            } else {
                return -1;
            }

        } else if(strategy == SKIPPED_SUBSTRING) {
            int query_cur = 0;
            int score = -1;

            if (startingMustMatch) {
                if (query_lower.charAt(0) != target_lower.charAt(0)) {
                    return -1;
                }
            }

            for (int target_cur = 0; target_cur < target_lower.length(); ++target_cur) {
                if (query_lower.charAt(query_cur) == target_lower.charAt(target_cur)) {
                    if (score == -1) {
                        score = target_cur;
                    }

                    ++query_cur;

                    if (query_cur == query_lower.length()) {
                        return score;
                    }
                }
            }
            return -1;

        } else if (strategy == FUZZY) {
            int query_cur = 0;
            int score = 100; // Base score for fuzzy match
            int last_match_index = -1;

            final int target_len = target.length();
            final int target_lower_len = target_lower.length();
            final int query_lower_len = query_lower.length();

            for (int target_cur = 0; target_cur < target_lower_len; ++target_cur) {
                if (query_lower.charAt(query_cur) == target_lower.charAt(target_cur)) {
                    // Weighted scoring (lower is better):
                    
                    // 1. Penalty for match position (prefer matches earlier in string)
                    score += target_cur;

                    // 2. Penalty for gaps between matched characters
                    if (last_match_index != -1) {
                        int gap = target_cur - last_match_index - 1;
                        score += gap * 10;
                    }

                    // 3. Bonus for matching start of words or uppercase letters
                    // Safely check original target bounds as toLowerCase() might change string length in some locales
                    if (target_cur < target_len) {
                        boolean isWordStart = (target_cur == 0) || (target.charAt(target_cur - 1) == ' ') || (target.charAt(target_cur - 1) == '.') || (target.charAt(target_cur - 1) == '_');
                        boolean isCamelCaseStart = Character.isUpperCase(target.charAt(target_cur)) && (target_cur > 0 && Character.isLowerCase(target.charAt(target_cur - 1)));

                        if (isWordStart || isCamelCaseStart) {
                            score -= 20;
                        }
                    }

                    last_match_index = target_cur;
                    ++query_cur;

                    if (query_cur == query_lower_len) {
                        return Math.max(0, score);
                    }
                }
            }
            return -1;

        } else {
            // Never happens
            throw new RuntimeException("Strategy not found");
        }
    }
}
