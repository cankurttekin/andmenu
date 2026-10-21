package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.cankurttekin.andmenu.R;
import com.cankurttekin.andmenu.applicationMain.MainActivity;
import com.cankurttekin.andmenu.interfaces.CandidateEntry;
import com.cankurttekin.andmenu.interfaces.CommandSearcher;
import com.cankurttekin.andmenu.interfaces.EventLauncher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherCommandSearcher implements CommandSearcher {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Map<String, String> cache = new HashMap<>();
    private static final Map<String, Boolean> loadingMap = new HashMap<>();

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

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();
        String queryLower = query.toLowerCase();

        if (queryLower.startsWith("weather ")) {
            String city = query.substring("weather ".length()).trim();
            candidates.add(new WeatherCandidateEntry(city, context));
        } else if (queryLower.startsWith("w ")) {
            String city = query.substring("w ".length()).trim();
            candidates.add(new WeatherCandidateEntry(city, context));
        }

        return candidates;
    }

    private static class HourlyForecastItem {
        int dayOffset;
        int hour;
        String tempC;
        String weatherCode;
    }

    private static class WeatherCandidateEntry implements CandidateEntry {
        private final String city;
        private final String loadingText;
        private final String errorText;

        public WeatherCandidateEntry(String city, Context context) {
            this.city = city;
            this.loadingText = String.format(context.getString(R.string.weather_loading), city.isEmpty() ? "current location" : city);
            this.errorText = context.getString(R.string.weather_error);
        }

        @NonNull
        @Override
        public String getTitle() {
            return "weather" + (city.isEmpty() ? "" : " " + city);
        }

        @Override
        public View getView(MainActivity mainActivity) {
            LinearLayout layout = new LinearLayout(mainActivity);
            layout.setOrientation(LinearLayout.VERTICAL);

            TextView textView = new TextView(mainActivity);
            TypedValue baseTextColor = new TypedValue();
            mainActivity.getTheme().resolveAttribute(R.attr.andmenuBaseTextColor, baseTextColor, true);
            textView.setTextColor(baseTextColor.data);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

            String result = cache.get(city);
            if (result != null) {
                textView.setText(result);
            } else {
                textView.setText(loadingText);
                fetchWeather(mainActivity);
            }
            layout.addView(textView);

            Button refreshButton = new Button(mainActivity);
            refreshButton.setText(R.string.button_update);
            refreshButton.setOnClickListener(v -> {
                cache.remove(city);
                textView.setText(loadingText);
                fetchWeather(mainActivity);
            });
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mainActivity.getResources().getDisplayMetrics());
            refreshButton.setLayoutParams(params);
            
            layout.addView(refreshButton);

            return layout;
        }

        private void fetchWeather(MainActivity activity) {
            if (Boolean.TRUE.equals(loadingMap.get(city))) return;
            loadingMap.put(city, true);

            executor.execute(() -> {
                String result;
                HttpURLConnection connection = null;
                try {
                    String urlString = "https://wttr.in/" + (city.isEmpty() ? "" : city.replace(" ", "+")) + "?format=j1";
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        result = parseWeatherJson(response.toString());
                    } else {
                        result = errorText + " (HTTP " + responseCode + ")";
                    }
                } catch (java.net.UnknownHostException e) {
                    result = errorText + ": No internet connection";
                } catch (java.net.SocketTimeoutException e) {
                    result = errorText + ": Connection timed out";
                } catch (Exception e) {
                    result = errorText + ": Network error";
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }

                final String finalResult = result;
                new Handler(Looper.getMainLooper()).post(() -> {
                    cache.put(city, finalResult);
                    loadingMap.put(city, false);
                    activity.refreshSearch();
                });
            });
        }

        private String parseWeatherJson(String jsonStr) throws Exception {
            JSONObject json = new JSONObject(jsonStr);
            StringBuilder sb = new StringBuilder();

            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            String nowString = null;
            JSONArray currentArr = json.getJSONArray("current_condition");
            if (currentArr.length() > 0) {
                JSONObject current = currentArr.getJSONObject(0);
                String temp = current.getString("temp_C");
                String code = current.getString("weatherCode");
                String feelsLike = current.getString("FeelsLikeC");
                nowString = "Now: " + getEmojiForCode(code, currentHour) + " " + temp + "°C (Feels like " + feelsLike + "°C)";
            }

            JSONArray weatherArr = json.getJSONArray("weather");
            if (weatherArr.length() > 0) {
                List<HourlyForecastItem> allHourly = new ArrayList<>();
                for (int day = 0; day < Math.min(2, weatherArr.length()); day++) {
                    JSONArray hArr = weatherArr.getJSONObject(day).getJSONArray("hourly");
                    for (int i = 0; i < hArr.length(); i++) {
                        JSONObject h = hArr.getJSONObject(i);
                        HourlyForecastItem item = new HourlyForecastItem();
                        item.dayOffset = day;
                        item.hour = Integer.parseInt(h.getString("time")) / 100;
                        item.tempC = h.getString("tempC");
                        item.weatherCode = h.getString("weatherCode");
                        allHourly.add(item);
                    }
                }

                int currentIntervalIdx = 0;
                for (int i = 0; i < allHourly.size(); i++) {
                    HourlyForecastItem item = allHourly.get(i);
                    if (item.dayOffset == 0 && currentHour >= item.hour) {
                        currentIntervalIdx = i;
                    }
                }

                int pastIdx = currentIntervalIdx;
                HourlyForecastItem pastItem = allHourly.get(pastIdx);
                String pastTimeStr = String.format(Locale.US, "%02d:00: ", pastItem.hour)
                        + getEmojiForCode(pastItem.weatherCode, pastItem.hour) + " " + pastItem.tempC + "°C";
                sb.append(pastTimeStr).append("\n");

                if (nowString != null) {
                    sb.append(nowString).append("\n");
                }

                int count = 0;
                for (int i = pastIdx + 1; i < allHourly.size() && count < 6; i++, count++) {
                    HourlyForecastItem item = allHourly.get(i);
                    String timeFormatted = String.format(Locale.US, "%02d:00", item.hour);
                    sb.append(timeFormatted)
                            .append(": ")
                            .append(getEmojiForCode(item.weatherCode, item.hour))
                            .append(" ")
                            .append(item.tempC)
                            .append("°C\n");
                }

                sb.append("\n3-Day Forecast:\n");
                for (int i = 0; i < Math.min(3, weatherArr.length()); i++) {
                    JSONObject day = weatherArr.getJSONObject(i);
                    String date = day.getString("date");
                    String max = day.getString("maxtempC");
                    String min = day.getString("mintempC");
                    String dayCode = day.getJSONArray("hourly").getJSONObject(4).getString("weatherCode");

                    sb.append(date).append(": ").append(getEmojiForCode(dayCode, 12)).append(" ").append(min).append("/").append(max).append("°C\n");
                }
            }
            return sb.toString();
        }

        private String getEmojiForCode(String code, int hour) {
            boolean isNight = hour >= 21 || hour < 6;
            switch (code) {
                case "113": return isNight ? "🌙" : "☀️";
                case "116": return isNight ? "☁️" : "⛅️";
                case "119":
                case "122": return "☁️";
                case "143":
                case "248":
                case "260": return "🌫";
                case "176":
                case "263":
                case "266":
                case "293":
                case "296":
                case "353": return "🌦";
                case "179":
                case "182":
                case "185":
                case "227":
                case "281":
                case "284":
                case "323":
                case "326":
                case "329":
                case "332":
                case "335":
                case "338":
                case "350":
                case "368":
                case "371":
                case "374":
                case "377": return "🌨";
                case "200":
                case "386":
                case "389":
                case "392":
                case "395": return "⛈";
                case "230": return "❄️";
                case "299":
                case "302":
                case "305":
                case "308":
                case "311":
                case "314":
                case "317":
                case "320":
                case "356":
                case "359":
                case "362":
                case "365": return "🌧";
                default: return "✨";
            }
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
            return false;
        }
    }
}
