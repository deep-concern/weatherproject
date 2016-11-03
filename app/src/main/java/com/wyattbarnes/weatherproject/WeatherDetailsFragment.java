package com.wyattbarnes.weatherproject;

import android.Manifest;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.wyattbarnes.weatherproject.authentication.OpenWeatherApiSyncAdapter;
import com.wyattbarnes.weatherproject.data.HourlyWeatherContract;

import java.util.Date;

// TODO: refactor to cleanly and consistently get location
// TODO: consider best practice for getting location (maybe move to a background task?)
/**
 * Created by wyatt.barnes on 2016/10/26.
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
    private String mCityName;
    private ListView mHourlyWeatherListView;
    private LocationRequest mLocationRequest;
    private Date mLastUpdateTime;
    private boolean mRequestingLocationUpdates;
    private State mCurrentState;
    private WeatherDetailsAdapter mAdapter;
    private ContentObserver mContentObserver;

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

        // Get permissions for checking location
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            ActivityCompat.requestPermissions(getActivity(), permissions, LOCATION_PERMISSIONS_REQUEST);
        } else {
            OpenWeatherApiSyncAdapter.initializeSyncAdapter(getContext());
        }

        /*mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                if (uri != null) {
                    mCityName = null; // HourlyWeatherContract.CityEntry.getCityNameFromUri(uri);
                }
            }
        }

        getContext().getContentResolver().registerContentObserver(
                HourlyWeatherContract.HourlyWeatherEntry.CONTENT_URI,
                true,
                mContentObserver
        );*/
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

        mHourlyWeatherListView = (ListView) rootView.findViewById(R.id.hourlyweather_listview);
        mHourlyWeatherListView.setAdapter(mAdapter);

        // TODO: Add click events

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(HOURLY_WEATHER_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
                    OpenWeatherApiSyncAdapter.initializeSyncAdapter(getContext());
                    startLocationUpdates();
                }
                return;
            default:
                return;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = preferences.edit();

        editor.putFloat(getString(R.string.pref_latitude_key), (float) location.getLatitude());
        editor.putFloat(getString(R.string.pref_longitude_key), (float) location.getLongitude());
        editor.apply();

        mLastUpdateTime = new Date();
        OpenWeatherApiSyncAdapter.syncImmediately(getContext());
        getLoaderManager().restartLoader(HOURLY_WEATHER_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE + " ASC";
        long cityId = Utility.getStoredCityId(getContext());
        Uri uri = HourlyWeatherContract.HourlyWeatherEntry.buildHourlyWeatherWithCityIdAndStartDate(
                cityId,
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

        if (mRequestingLocationUpdates) {
            return;
        }

        if (mGoogleApiClient == null) {
            connectGoogleApiClient();
            return;
        }

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            return;
        }

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
        if (!mRequestingLocationUpdates) {
            return;
        }

        if (mGoogleApiClient == null) {
            connectGoogleApiClient();
            return;
        }

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            return;
        }

        try {
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

    private void getCurrentLocationSettings() {
        if (mLocationRequest == null) {
            return;
        }

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates states =
                        locationSettingsResult.getLocationSettingsStates();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // TODO:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(WeatherDetailsFragment.this.getActivity(),
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // TODO
                        break;
                }
            }
        });
    }

    private enum State {
        NONE,
        STARTING_UPDATES,
        STOPING_UPDATES


    }
}
