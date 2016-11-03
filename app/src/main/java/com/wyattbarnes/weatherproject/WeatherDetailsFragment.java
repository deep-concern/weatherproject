package com.wyattbarnes.weatherproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.wyattbarnes.weatherproject.authentication.OpenWeatherMapApiSyncAdapter;
import com.wyattbarnes.weatherproject.data.HourlyWeatherContract;

import java.util.Date;

/**
 * Main activity's fragment. Displays the main UI parts.
 */
public class WeatherDetailsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String LOG_TAG = WeatherDetailsFragment.class.getSimpleName();

    private static final int HOURLY_WEATHER_LOADER = 0;
    private static final int REQUEST_CHECK_SETTINGS = 400;
    private static final int LOCATION_PERMISSIONS_REQUEST = 500;

    // SaveState keys
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting_location_updates_key";
    private static final String LAST_UPDATE_TIME_KEY = "last_update_time_string_key";

    private GoogleApiClient mGoogleApiClient;
    private long mCityId;
    // TODO: make into local var?
    private ListView mHourlyWeatherListView;
    // TODO: make into local var?
    private LocationRequest mLocationRequest;
    // TODO: remove if not needed
    private Date mLastUpdateTime;
    private boolean mRequestingLocationUpdates;
    private State mCurrentState;
    private WeatherDetailsAdapter mAdapter;
    private ContentObserver mContentObserver;

    // These rows and columns are what we'll be getting from each query to the cursor adapter.
    private static final String[] HOURLY_WEATHER_COLUMNS = {
            HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME + "." + HourlyWeatherContract.HourlyWeatherEntry._ID,
            HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE,
            HourlyWeatherContract.HourlyWeatherEntry.COLUMN_WEATHER_DESCRIPTION,
            HourlyWeatherContract.HourlyWeatherEntry.COLUMN_TEMP_MAX,
            HourlyWeatherContract.HourlyWeatherEntry.COLUMN_TEMP_MIN,
            HourlyWeatherContract.CityEntry.COLUMN_CITY_LATITUDE,
            HourlyWeatherContract.CityEntry.COLUMN_CITY_LONGITUDE,
            HourlyWeatherContract.CityEntry.COLUMN_CITY_NAME,
            HourlyWeatherContract.CityEntry.COLUMN_CITY_COUNTRY
    };

    // UPDATE THE ORDER IF THE ABOVE ARRAY CHANGES
    public static final int COLUMN_HOURLY_WEATHER_ID = 0;
    public static final int COLUMN_HOURLY_WEATHER_DATE = 1;
    public static final int COLUMN_HOURLY_WEATHER_DESCRIPTION = 2;
    public static final int COLUMN_HOURLY_WEATHER_TEMP_MAX = 3;
    public static final int COLUMN_HOURLY_WEATHER_TEMP_MIN = 4;
    public static final int COLUMN_CITY_LATITUDE = 5;
    public static final int COLUMN_CITY_LONGITUDE = 6;
    public static final int COLUMN_CITY_NAME = 7;
    public static final int COLUMN_CITY_COUNTRY = 8;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateValuesFromBundle(savedInstanceState);

        // Content observer to listen to changes from our sync adapter
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);

                // Getting the newly added city ID for reasons
                Cursor c = getContext().getContentResolver().query(
                        HourlyWeatherContract.HourlyWeatherEntry.CONTENT_URI,
                        new String[] {HourlyWeatherContract.HourlyWeatherEntry.COLUMN_CITY_KEY},
                        null,
                        null,
                        HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE + " DESC "
                );
                if (c.moveToFirst()) {
                    int cityIdIndex = c.getColumnIndex(HourlyWeatherContract.HourlyWeatherEntry.COLUMN_CITY_KEY);
                    WeatherDetailsFragment.this.mCityId = c.getLong(cityIdIndex);
                } else {
                    WeatherDetailsFragment.this.mCityId = -1;
                }

                // Restart our cursor loader to fetch new data
                WeatherDetailsFragment.this.getLoaderManager().restartLoader(WeatherDetailsFragment.HOURLY_WEATHER_LOADER, null, WeatherDetailsFragment.this);
            }
        };

        // Regestering our content observer
        getContext().getContentResolver().registerContentObserver(
                HourlyWeatherContract.HourlyWeatherEntry.CONTENT_URI,
                true,
                mContentObserver
        );

        // Get permissions for checking location
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Failed, so we need to request permissions
            String[] permissions = {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            ActivityCompat.requestPermissions(getActivity(), permissions, LOCATION_PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        if (mLastUpdateTime == null) {
            outState.putLong(LAST_UPDATE_TIME_KEY, -1L);
        } else {
            outState.putLong(LAST_UPDATE_TIME_KEY, mLastUpdateTime.getTime());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mAdapter = new WeatherDetailsAdapter(getContext(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_weather_details, container, false);

        // Get list and attach adapter
        mHourlyWeatherListView = (ListView) rootView.findViewById(R.id.hourlyweather_listview);
        mHourlyWeatherListView.setAdapter(mAdapter);

        // TODO: Add click events

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        // Initialize our loader for our adapters
        getLoaderManager().initLoader(HOURLY_WEATHER_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Connect if we can
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Start looking for location updates
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Get ready for activity to go bye bye and turn of updates
        stopLocationUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Disconnect
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // For now, unregister content observer
        if (mContentObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mContentObserver);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        switch(mCurrentState) {
            case STARTING_UPDATES:
                startLocationUpdates();
                return;
            case STOPING_UPDATES:
                stopLocationUpdates();
                return;
            default:
                return;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // TODO: make toast for this
        Log.d(LOG_TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // TODO: make toast for this
        Log.d(LOG_TAG, "Connection failed");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Since we have permission, start listening for location updates
                    startLocationUpdates();
                }
                return;
            default:
                return;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastUpdateTime = new Date();

        // Force new info to be added (since our location changed, our weather info may be different)
        OpenWeatherMapApiSyncAdapter.syncImmediately(getContext(), location.getLatitude(), location.getLongitude());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE + " ASC";
        Uri uri = HourlyWeatherContract.HourlyWeatherEntry.buildHourlyWeatherWithCityIdAndStartDate(
                mCityId,
                System.currentTimeMillis()
        );
        return new CursorLoader(getContext(),
                uri,
                HOURLY_WEATHER_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private void updateValuesFromBundle(Bundle inState) {
        if (inState != null) {
            if (inState.containsKey(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = inState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }

            if (inState.containsKey(LAST_UPDATE_TIME_KEY)
                    && inState.getLong(LAST_UPDATE_TIME_KEY) != -1L) {
                mLastUpdateTime = new Date(inState.getLong(LAST_UPDATE_TIME_KEY));
            }
        }
    }

    private void startLocationUpdates() {
        mCurrentState = State.STARTING_UPDATES;

        // Don't run this if we're already checking
        if (mRequestingLocationUpdates) {
            return;
        }

        // We need to be connected, so connect to google api if we haven't
        if (mGoogleApiClient == null) {
            connectGoogleApiClient();
            return;
        }
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            return;
        }

        // Basic location request to send
        mLocationRequest = new LocationRequest()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mRequestingLocationUpdates = true;
        } catch (SecurityException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        mCurrentState = State.NONE;
    }

    private void stopLocationUpdates() {
        mCurrentState = State.STOPING_UPDATES;
        // Already ran this, so quit
        if (!mRequestingLocationUpdates) {
            return;
        }

        // Disconnect from google api if needed
        if (mGoogleApiClient == null) {
            connectGoogleApiClient();
            return;
        }
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            return;
        }

        try {
            // Stop checking location
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mRequestingLocationUpdates = false;
        } catch (SecurityException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        mCurrentState = State.NONE;
    }

    private void connectGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .enableAutoManage(getActivity(), this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private enum State {
        NONE,
        STARTING_UPDATES,
        STOPING_UPDATES


    }
}
