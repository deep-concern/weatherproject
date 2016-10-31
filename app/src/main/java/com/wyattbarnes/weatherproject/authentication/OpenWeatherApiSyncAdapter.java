package com.wyattbarnes.weatherproject.authentication;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by wyatt.barnes on 2016/10/31.
 */

public class OpenWeatherApiSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = OpenWeatherApiSyncAdapter.class.getSimpleName();
    ContentResolver mContentResolver;

    public OpenWeatherApiSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account,
                              Bundle bundle,
                              String s,
                              ContentProviderClient contentProviderClient,
                              SyncResult syncResult) {
        Location location = null;
        
        // Nothing is set, so just quit
        if (location == null) {
            return;
        }

        // URL connection to use
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Data
        String jsonStr = null;

        String units = "metric";
        
        try {
            // Going to be getting a 5day/3hour forecast
            final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?";
            final String LATITUDE_PARAM = "lat";
            final String LONGITUDE_PARAM = "lon";
            final String UNITS_PARAM = "units";
            final String APPID_PARAM = "APPID";

            // Build URI using the current location and settings
            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(LATITUDE_PARAM, Double.toString(location.getLatitude()))
                    .appendQueryParameter(LONGITUDE_PARAM, Double.toString(location.getLongitude()))
                    .appendQueryParameter(UNITS_PARAM, units)
                    .build();

            URL url = new URL(builtUri.toString());
            Log.d(LOG_TAG, "Query url: " + url);

            // Query
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the response
            InputStream in = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();

            if (in == null) {
                return;
            }

            reader = new BufferedReader(new InputStreamReader(in));

            // Read from the reader
            String line;
            while ((line = reader.readLine()) != null) {
                // Adding a new line char for debugging purposes
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return;
            }

            jsonStr = buffer.toString();
            getWeatherDataFromJson(jsonStr, location);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
    }

    private void getWeatherDataFromJson(String jsonStr, Location location) throws JSONException {
        final String CITY = "city";
        final String CITY_ID = "id";
        final String CITY_NAME = "name";
        final String COORDINATES = "coord";
        final String COORDINATES_LATITUDE = "lat";
        final String COORDINATES_LONGITUDE = "lat";
        final String COUNTRY = "country";
        final String CONDITION = "cod";
        final String MESSAGE = "message";
        final String COUNT = "cnt";
        final String LIST = "list";
        final String LIST_ITEM_TIME = "dt";
        final String LIST_ITEM_MAIN = "main";
        final String LIST_ITEM_MAIN_TEMP = "temp";
        final String LIST_ITEM_MAIN_TEMP_MIN = "temp_min";
        final String LIST_ITEM_MAIN_TEMP_MAX = "temp_max";
        final String LIST_ITEM_WEATHER = "weather";
        final String LIST_ITEM_WEATHER_ID = "id";
        final String LIST_ITEM_WEATHER_MAIN = "main";
        final String LIST_ITEM_WEATHER_DESCRIPTION = "description";
        final String LIST_ITEM_WEATHER_ICON = "icon";
        final String LIST_ITEM_TIME_TEXT = "dt_txt";

        try {
            // Parse response
            JSONObject weatherJson = new JSONObject(jsonStr);

            // Parse city information
            JSONObject cityJson = weatherJson.getJSONObject(CITY);
            City city = new City();
            city.id = cityJson.getInt(CITY_ID);
            city.name = cityJson.getString(CITY_NAME);

            // Get hourly weather
            JSONArray listJson = weatherJson.getJSONArray(LIST);
            List<HourlyWeather> forecast = new ArrayList<>();

            for (int i = 0; i < listJson.length(); i++) {
                HourlyWeather weather = new HourlyWeather();
                ti
            }



        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    public static class City {
        public int id;
        public String name;
        public Coordinates coordinates;
        public String country;
    }

    public static class Coordinates {
        public float latitude;
        public float longitude;
    }

    public static class HourlyWeather {
        public Date time;
    }
}
