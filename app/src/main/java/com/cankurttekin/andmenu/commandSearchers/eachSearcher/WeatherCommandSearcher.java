package com.cankurttekin.andmenu.commandSearchers.eachSearcher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherCommandSearcher implements CommandSearcher {
    private static final String TARGET_COMMAND = "w";
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

        if (query.toLowerCase().startsWith(TARGET_COMMAND + " ")) {
            String city = query.substring(TARGET_COMMAND.length() + 1).trim();
            if (!city.isEmpty()) {
                candidates.add(new WeatherCandidateEntry(city, context));
            }
        } else if (query.equalsIgnoreCase(TARGET_COMMAND)) {
            candidates.add(new WeatherCandidateEntry("", context));
        }

        return candidates;
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
            return TARGET_COMMAND + (city.isEmpty() ? "" : " " + city);
        }

        @Override
        public View getView(MainActivity mainActivity) {
            TextView textView = new TextView(mainActivity);
            TypedValue baseTextColor = new TypedValue();
            mainActivity.getTheme().resolveAttribute(R.attr.andmenuBaseTextColor, baseTextColor, true);
            textView.setTextColor(baseTextColor.data);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

            String result = cache.get(city);
            if (result != null) {
                textView.setText(result);
            } else {
                textView.setText(loadingText);
                fetchWeather(mainActivity);
            }
            return textView;
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
                } catch (Exception e) {
                    result = errorText + ": " + e.getClass().getSimpleName();
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

            JSONArray currentArr = json.getJSONArray("current_condition");
            if (currentArr.length() > 0) {
                JSONObject current = currentArr.getJSONObject(0);
                String temp = current.getString("temp_C");
                String code = current.getString("weatherCode");
                String desc = current.getJSONArray("weatherDesc").getJSONObject(0).getString("value");
                String feelsLike = current.getString("FeelsLikeC");

                sb.append(getEmojiForCode(code)).append(" ").append(temp).append("°C (").append(desc).append(")\n");
                sb.append("Feels like: ").append(feelsLike).append("°C\n\n");
            }

            JSONArray weatherArr = json.getJSONArray("weather");
            if (weatherArr.length() > 0) {
                sb.append("Hourly:\n");
                JSONArray hourlyArr = weatherArr.getJSONObject(0).getJSONArray("hourly");
                for (int i = 0; i < Math.min(8, hourlyArr.length()); i ++) {
                    JSONObject h = hourlyArr.getJSONObject(i);
                    String time = h.getString("time");

                    int hour = Integer.parseInt(time);
                    time = String.format("%02d:00", hour / 100);

                    String hTemp = h.getString("tempC");
                    String hCode = h.getString("weatherCode");

                    sb.append(time)
                        .append(": ")
                        .append(getEmojiForCode(hCode))
                        .append(" ")
                        .append(hTemp)
                        .append("°C\n");

                }
                sb.append("\nForecast:\n");
                for (int i = 0; i < Math.min(3, weatherArr.length()); i++) {
                    JSONObject day = weatherArr.getJSONObject(i);
                    String date = day.getString("date");
                    String max = day.getString("maxtempC");
                    String min = day.getString("mintempC");
                    String dayCode = day.getJSONArray("hourly").getJSONObject(4).getString("weatherCode");

                    sb.append(date).append(": ").append(getEmojiForCode(dayCode)).append(" ").append(min).append("/").append(max).append("°C\n");
                }
            }
            return sb.toString();
        }

        private String getEmojiForCode(String code) {
            switch (code) {
                case "113": return "☀️";
                case "116": return "⛅️";
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
