package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.LocaleList;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.applicationMain.MainActivity;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class CalendarCommandSearcher implements CommandSearcher {
    @Override
    public void refresh(Context context) {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isPrepared() {
        return true;
    }

    @Override
    public void waitUntilPrepared() {
    }

    // The day of switching to Gregorian calendar differs on regions, while it is available since 1582/10/15.
    // Command "cal" for Linux seems to switch from Julian calendar to Gregorian calendar on 1752/09/14 by default,
    // which is based on Kingdom of Great Britain.
    // For now "cal" command on andmenu supports only since 1873/01/01, which Japan switched to Gregorian calendar on.
    // Before further survey for libraries, upper limit is 9999.
    public static boolean isSupported(int year) {
        return year >= 1873 && year <= 9999;
    }

    @NonNull
    @Override
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List <CandidateEntry> ret = new ArrayList<>();

        if (query.startsWith("cal ") || query.equals("cal")) {
            final String[] querySplit = query.split(" ");
            Calendar calendar = null;

            if (querySplit.length == 1) {
                calendar = Calendar.getInstance();

            } else if (querySplit.length == 3){
                try {
                    int month = Integer.parseInt(querySplit[1]);
                    int year = Integer.parseInt(querySplit[2]);

                    if (isSupported(year)) {
                        calendar = Calendar.getInstance();
                        calendar.set(Calendar.DAY_OF_MONTH, 1);  // for case of current day of month is 29 or more
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month - 1);
                    }

                } catch (NumberFormatException e) {
                    // Simply keep @calendar null
                }
            }

            if (calendar != null) {
                ret.add(new CalCandidateEntry(calendar));
            }
        }

        return ret;
    }

    static class CalCandidateEntry implements CandidateEntry {
        private final Calendar calendar;

        public CalCandidateEntry(Calendar calendar) {
            this.calendar = calendar;
        }

        @Override
        public String getTitle() {
            return null;
        }

        private static boolean isLeapYearGregorian (int year) {
            if (year % 4 == 0) {
                if (year % 100 == 0) {
                    return year % 400 == 0;
                }
                return true;
            }
            return false;
        }

        private static final int[] MONTH_SIZES_NORMAL_YEAR = {-1, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        private static int getMonthSize(int year, int month) {
            while (month < 1) {
                month += 12;
                year -= 1;
            }

            int ret = MONTH_SIZES_NORMAL_YEAR[month];

            if (month == 2 && isLeapYearGregorian(year)) {
                ret += 1;
            }
            return ret;
        }

        @Override
        public View getView(final MainActivity mainActivity) {
            final double pixelsPerSp = mainActivity.getResources().getDisplayMetrics().scaledDensity;
            Locale locale;
            if (Build.VERSION.SDK_INT >= 24) {
                LocaleList localeList = mainActivity.getResources().getConfiguration().getLocales();
                if (! localeList.isEmpty()) {
                    locale = localeList.get(0);
                } else {
                    locale = new Locale("en");
                }
            } else {
                locale = mainActivity.getResources().getConfiguration().locale;
            }

            final TypedValue baseTextColor = new TypedValue();
            mainActivity.getTheme().resolveAttribute(R.attr.andmenuBaseTextColor, baseTextColor, true);
            final TypedValue disabledTextColor = new TypedValue();
            mainActivity.getTheme().resolveAttribute(R.attr.andmenuDisabledTextColor, disabledTextColor, true);

            LinearLayout ret = new LinearLayout(mainActivity);
            ret.setOrientation(LinearLayout.VERTICAL);

            LinearLayout header = new LinearLayout(mainActivity);
            header.setOrientation(LinearLayout.HORIZONTAL);
            Button prevButton = new Button(mainActivity);
            prevButton.setText(R.string.button_decrement_minimal);
            prevButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            ViewGroup.LayoutParams prevButtonLayoutParam = new LinearLayout.LayoutParams(0, 0);
            prevButtonLayoutParam.width = (int)(32.0 * pixelsPerSp);
            prevButtonLayoutParam.height = (int)(32.0 * pixelsPerSp);
            prevButton.setLayoutParams(prevButtonLayoutParam);
            prevButton.setPadding(0, 0, 0, 0);
            final int prevMonth = ((calendar.get(Calendar.MONTH) + 11) % 12) + 1;
            final int prevYear = calendar.get(Calendar.YEAR) + (calendar.get(Calendar.MONTH) == Calendar.JANUARY ? -1 : 0);
            if (isSupported(prevYear)) {
                prevButton.setOnClickListener(v -> mainActivity.changeInputText("cal " + prevMonth + " " + prevYear)
                );
                prevButton.setEnabled(true);
            } else {
                prevButton.setEnabled(false);
            }
            header.addView(prevButton);
            TextView headerTitle = new TextView(mainActivity);
            headerTitle.setTextColor(baseTextColor.data);
            // TODO: better way for languages with month names like English
            headerTitle.setText(String.format(locale, "%d/%02d", calendar.get(Calendar.YEAR), (calendar.get(Calendar.MONTH) + 1)));
            headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            headerTitle.setPaddingRelative((int)(8.0 * pixelsPerSp), 0, (int)(8.0 * pixelsPerSp), 0);
            header.addView(headerTitle);
            Button nextButton = new Button(mainActivity);
            nextButton.setText(R.string.button_increment_minimal);
            nextButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            ViewGroup.LayoutParams nextButtonLayoutParam = new LinearLayout.LayoutParams(0, 0);
            nextButtonLayoutParam.width = (int)(32.0 * pixelsPerSp);
            nextButtonLayoutParam.height = (int)(32.0 * pixelsPerSp);
            nextButton.setLayoutParams(nextButtonLayoutParam);
            nextButton.setPadding(0, 0, 0, 0);
            final int nextMonth = ((calendar.get(Calendar.MONTH) + 1) % 12) + 1;
            final int nextYear = calendar.get(Calendar.YEAR) + (calendar.get(Calendar.MONTH) == Calendar.DECEMBER ? 1 : 0);
            if (isSupported(nextYear)) {
                nextButton.setOnClickListener(v -> mainActivity.changeInputText("cal " + nextMonth + " " + nextYear)
                );
                nextButton.setEnabled(true);
            } else {
                nextButton.setEnabled(false);
            }
            header.addView(nextButton);
            header.setPadding(0, 0, 0, (int)(8.0 * pixelsPerSp));

            ret.addView(header);


            TableLayout calendarTable = new TableLayout(mainActivity);

            Calendar calForFirstDay = (Calendar) this.calendar.clone();
            calForFirstDay.set(Calendar.DAY_OF_MONTH, 1);

            int firstDayOfWeek = this.calendar.getFirstDayOfWeek();
            int startCol = (calForFirstDay.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7;

            int monthSize = getMonthSize(this.calendar.get(Calendar.YEAR), this.calendar.get(Calendar.MONTH) + 1);
            int prevMonthSize = getMonthSize(this.calendar.get(Calendar.YEAR), this.calendar.get(Calendar.MONTH));

            final String[] dayOfWeekNames = new DateFormatSymbols(locale).getShortWeekdays();
            Calendar today = Calendar.getInstance();

            // Header row (weekdays)
            TableRow headerRow = new TableRow(mainActivity);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 7; ++col) {
                int actualDayOfWeek = (firstDayOfWeek - 1 + col) % 7 + 1;
                TextView dayTextView = new TextView(mainActivity);
                dayTextView.setTypeface(Typeface.MONOSPACE);
                dayTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                dayTextView.setTextColor(baseTextColor.data);
                dayTextView.setText(dayOfWeekNames[actualDayOfWeek]);
                dayTextView.setPaddingRelative((int) (8.0 * pixelsPerSp), 0, (int) (8.0 * pixelsPerSp), 0);
                headerRow.addView(dayTextView);
            }
            calendarTable.addView(headerRow);

            // Month days grid
            for (int row = 0; row < 6; ++row) {
                int firstCellOfDay = row * 7 - startCol + 1;
                if (row > 0 && firstCellOfDay > monthSize) {
                    break;
                }

                TableRow weekRow = new TableRow(mainActivity);
                weekRow.setOrientation(LinearLayout.HORIZONTAL);

                for (int col = 0; col < 7; ++col) {
                    int dayOfMonth = row * 7 + col - startCol + 1;
                    TextView dayTextView = new TextView(mainActivity);
                    dayTextView.setTypeface(Typeface.MONOSPACE);

                    int textColor = baseTextColor.data;
                    Calendar calOfThisDay = Calendar.getInstance();

                    if (dayOfMonth >= 1 && dayOfMonth <= monthSize) {
                        dayTextView.setText(String.format(locale, "%d", dayOfMonth));
                        calOfThisDay.set(this.calendar.get(Calendar.YEAR), this.calendar.get(Calendar.MONTH), dayOfMonth);
                    } else if (dayOfMonth < 1) {
                        dayTextView.setText(String.format(locale, "%d", dayOfMonth + prevMonthSize));
                        textColor = disabledTextColor.data;
                        int ym = this.calendar.get(Calendar.YEAR) * 12 + this.calendar.get(Calendar.MONTH) - 1;
                        calOfThisDay.set(ym / 12, ym % 12, dayOfMonth + prevMonthSize);
                    } else {
                        dayTextView.setText(String.format(locale, "%d", dayOfMonth - monthSize));
                        textColor = disabledTextColor.data;
                        int ym = this.calendar.get(Calendar.YEAR) * 12 + this.calendar.get(Calendar.MONTH) + 1;
                        calOfThisDay.set(ym / 12, ym % 12, dayOfMonth - monthSize);
                    }

                    if (today.get(Calendar.YEAR) == calOfThisDay.get(Calendar.YEAR) &&
                            today.get(Calendar.MONTH) == calOfThisDay.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) == calOfThisDay.get(Calendar.DAY_OF_MONTH)) {

                        final TypedValue backgroundColor = new TypedValue();
                        mainActivity.getTheme().resolveAttribute(android.R.attr.windowBackground, backgroundColor, true);
                        dayTextView.setTextColor(backgroundColor.data);
                        dayTextView.setBackgroundColor(textColor);
                    } else {
                        dayTextView.setTextColor(textColor);
                    }

                    dayTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                    dayTextView.setPaddingRelative((int) (8.0 * pixelsPerSp), 0, (int) (8.0 * pixelsPerSp), 0);
                    weekRow.addView(dayTextView);
                }
                calendarTable.addView(weekRow);
            }

            ret.addView(calendarTable);
            return ret;
        }

        @Override
        public boolean hasLongView() {
            return true;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return null;
        }

        @Override
        public Drawable getIcon(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return false;
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }
}
