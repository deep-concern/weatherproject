package com.wyattbarnes.weatherproject.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.wyattbarnes.weatherproject.BuildConfig;
import com.wyattbarnes.weatherproject.R;
import com.wyattbarnes.weatherproject.data.HourlyWeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * SyncAdapter subclass for querying OWM API.
 *
 * Allows us to have updated information (currently set to hourly only).
 */
public class OpenWeatherMapApiSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = OpenWeatherMapApiSyncAdapter.class.getSimpleName();

    // Intervals to run at
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    // Keys for storing items in bundles
    private static final String LATITUDE_KEY = "latitude_key";
    private static final String LONGITUDE_KEY = "longitude_key";

    ContentResolver mContentResolver;

    /**
     * Constructor.
     *
     * @param context - The request from which we get a content resolver.
     * @param autoInitialize - Auto initialization boolean.
     */
    public OpenWeatherMapApiSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account,
                              Bundle bundle,
                              String authority,
                              ContentProviderClient provider,
                              SyncResult syncResult) {
        // Get stored values to use in query (in this case, coords)
        double latitude = bundle.getDouble(LATITUDE_KEY);
        double longitude = bundle.getDouble(LONGITUDE_KEY);

        // URL connection to use
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Data
        String jsonStr = null;

        // TODO: Move this to options
        // Some hard coded values
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
                    .appendQueryParameter(LATITUDE_PARAM, Double.toString(latitude))
                    .appendQueryParameter(LONGITUDE_PARAM, Double.toString(longitude))
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            URL url = new URL(builtUri.toString());

            // For debugging purposes
            Log.d(LOG_TAG, "Query url: " + url);

            // Query
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the response
            InputStream in = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();

            // Return if nothing
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

            // Nothing received
            if (buffer.length() == 0) {
                return;
            }

            jsonStr = buffer.toString();
            getWeatherDataFromJson(jsonStr);
        } catch (IOException|JSONException e) {
            // TODO: Handle this in a user friendly way
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

    /**
     * Basic function for activity to call to get most recent data. Will initialize Sync Adapter
     * if none exists, and then will force an info update from query.
     *
     * @param context
     * @param latitude
     * @param longitude
     */
    public static void syncImmediately(Context context, double latitude, double longitude) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putDouble(LATITUDE_KEY, latitude);
        bundle.putDouble(LONGITUDE_KEY, longitude);
        ContentResolver.requestSync(
                getSyncAccount(context, latitude, longitude),
                context.getString(R.string.content_authority),
                bundle
        );
    }

    /**
     * Helper function for parsing JSON.
     *
     * @param jsonStr
     * @throws JSONException
     */
    private void getWeatherDataFromJson(String jsonStr) throws JSONException {
        final String CITY = "city";
        final String CITY_NAME = "name";
        final String CITY_COUNTRY = "country";
        final String CITY_COORDINATES = "coord";
        final String CITY_COORDINATES_LATITUDE = "lat";
        final String CITY_COORDINATES_LONGITUDE = "lat";
        final String LIST = "list";
        final String LIST_ITEM_DATETIME = "dt";
        final String LIST_ITEM_MAIN = "main";
        final String LIST_ITEM_MAIN_TEMP_MIN = "temp_min";
        final String LIST_ITEM_MAIN_TEMP_MAX = "temp_max";
        final String LIST_ITEM_WEATHER = "weather";
        final String LIST_ITEM_WEATHER_ID = "id";
        final String LIST_ITEM_WEATHER_DESCRIPTION = "description";

        try {
            // Parse response
            JSONObject hourlyWeatherJson = new JSONObject(jsonStr);
            JSONArray hourlyWeatherArray = hourlyWeatherJson.getJSONArray(LIST);

            // Parse city information
            JSONObject cityJson = hourlyWeatherJson.getJSONObject(CITY);
            String cityName = cityJson.getString(CITY_NAME);
            String cityCountry = cityJson.getString(CITY_COUNTRY);
            JSONObject cityCoordinates = cityJson.getJSONObject(CITY_COORDINATES);
            double cityLatitude = cityCoordinates.getDouble(CITY_COORDINATES_LATITUDE);
            double cityLongitude = cityCoordinates.getDouble(CITY_COORDINATES_LONGITUDE);

            // Add city to db
            long cityId = addCity(cityLatitude, cityLongitude, cityName, cityCountry);

            // Vector to store weather data
            Vector<ContentValues> contentValuesVector = new Vector<>(hourlyWeatherArray.length());

            for (int i = 0; i < hourlyWeatherArray.length(); i++) {
                long date;
                int weatherId;
                String weatherDescription;
                double maxTemp;
                double minTemp;

                JSONObject hourlyJson = hourlyWeatherArray.getJSONObject(i);

                // Hourly date
                date = hourlyJson.getLong(LIST_ITEM_DATETIME) * 1000L;

                // Get weather data
                JSONArray weatherArray = hourlyJson.getJSONArray(LIST_ITEM_WEATHER);
                JSONObject weatherJson = weatherArray.getJSONObject(0);
                weatherId = weatherJson.getInt(LIST_ITEM_WEATHER_ID);
                weatherDescription = weatherJson.getString(LIST_ITEM_WEATHER_DESCRIPTION);

                // Tempurature
                JSONObject mainJson = hourlyJson.getJSONObject(LIST_ITEM_MAIN);
                maxTemp = mainJson.getDouble(LIST_ITEM_MAIN_TEMP_MAX);
                minTemp = mainJson.getDouble(LIST_ITEM_MAIN_TEMP_MIN);

                // Push values
                ContentValues weatherValues = new ContentValues();
                weatherValues.put(HourlyWeatherContract.HourlyWeatherEntry.COLUMN_CITY_KEY,
                        cityId);
                weatherValues.put(HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE,
                        date);
                weatherValues.put(HourlyWeatherContract.HourlyWeatherEntry.COLUMN_WEATHER_ID,
                        weatherId);
                weatherValues.put(HourlyWeatherContract.HourlyWeatherEntry.COLUMN_WEATHER_DESCRIPTION,
                        weatherDescription);
                weatherValues.put(HourlyWeatherContract.HourlyWeatherEntry.COLUMN_TEMP_MAX,
                        maxTemp);
                weatherValues.put(HourlyWeatherContract.HourlyWeatherEntry.COLUMN_TEMP_MIN,
                        minTemp);
                contentValuesVector.add(weatherValues);
            }

            // Insert if any values received
            if (contentValuesVector.size() > 0) {
                ContentValues[] contentValuesArray = new ContentValues[contentValuesVector.size()];
                contentValuesVector.toArray(contentValuesArray);
                getContext().getContentResolver().bulkInsert(
                        HourlyWeatherContract.HourlyWeatherEntry.CONTENT_URI,
                        contentValuesArray
                );

                // delete old data
                getContext().getContentResolver().delete(
                        HourlyWeatherContract.HourlyWeatherEntry.CONTENT_URI,
                        HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE + " < ?",
                        new String[] {Long.toString(System.currentTimeMillis())}
                );

                getContext().getContentResolver().notifyChange(HourlyWeatherContract.HourlyWeatherEntry.CONTENT_URI, null, false);
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    /**
     * Helper function for adding cities to their own table.
     *
     * @param latitude
     * @param longitude
     * @param cityName
     * @param cityCountry
     * @return ID of the (new) city.
     */
    private long addCity(double latitude, double longitude, String cityName, String cityCountry) {
        long cityId;

        Cursor cityCursor = getContext().getContentResolver().query(
                HourlyWeatherContract.CityEntry.CONTENT_URI,
                new String[] {HourlyWeatherContract.CityEntry._ID},
                HourlyWeatherContract.CityEntry.COLUMN_CITY_NAME + " = ? AND "
                    + HourlyWeatherContract.CityEntry.COLUMN_CITY_COUNTRY + " = ?",
                new String[] {cityName, cityCountry},
                null
        );

        if (cityCursor != null && cityCursor.moveToFirst()) {
            int cityIdIndex = cityCursor.getColumnIndex(HourlyWeatherContract.CityEntry._ID);
            cityId = cityCursor.getLong(cityIdIndex);
            cityCursor.close();
        } else {
            ContentValues cityValues = new ContentValues();

            cityValues.put(HourlyWeatherContract.CityEntry.COLUMN_CITY_LATITUDE, latitude);
            cityValues.put(HourlyWeatherContract.CityEntry.COLUMN_CITY_LONGITUDE, longitude);
            cityValues.put(HourlyWeatherContract.CityEntry.COLUMN_CITY_NAME, cityName);
            cityValues.put(HourlyWeatherContract.CityEntry.COLUMN_CITY_COUNTRY, cityCountry);

            Uri insertedUri = getContext().getContentResolver().insert(
                    HourlyWeatherContract.CityEntry.CONTENT_URI,
                    cityValues
            );

            cityId = ContentUris.parseId(insertedUri);
        }

        return cityId;
    }

    /**
     * Helper function for configuring periodic syncs.
     *
     * @param context
     * @param syncInterval
     * @param flexTime
     * @param latitude
     * @param longitude
     */
    private static void configurePeriodicSync(Context context, int syncInterval, int flexTime, double latitude, double longitude) {
        Account account = getSyncAccount(context, latitude, longitude);
        String authority = context.getString(R.string.content_authority);
        Bundle bundle = new Bundle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SyncRequest request = new SyncRequest.Builder()
                    .syncPeriodic(syncInterval, flexTime)
                    .setSyncAdapter(account, authority)
                    .setExtras(new Bundle())
                    .build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper function for creating/getting the account for syncing.
     *
     * @param context
     * @param latitude
     * @param longitude
     * @return
     */
    private static Account getSyncAccount(Context context, double latitude, double longitude) {
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        Account newAccount = new Account(
                context.getString(R.string.app_name),
                context.getString(R.string.sync_account_type)
        );

        if (accountManager.getPassword(newAccount) == null) {
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }

            onAccountCreated(newAccount, context, latitude, longitude);
        }
        return newAccount;
    }

    /**
     * Callback for newly created accounts.
     *
     * @param account
     * @param context
     * @param latitude
     * @param longitude
     */
    private static void onAccountCreated(Account account, Context context, double latitude, double longitude) {
        OpenWeatherMapApiSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME, latitude, longitude);

        ContentResolver.setSyncAutomatically(account, context.getString(R.string.content_authority), true);

        syncImmediately(context, latitude, longitude);
    }
}
