package com.wyattbarnes.weatherproject;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

import java.util.Date;

// TODO: refactor to cleanly and consistently get location
// TODO: consider best practice for getting location (maybe move to a background task?)
/**
 * Created by wyatt.barnes on 2016/10/26.
 */
public class WeatherDetailsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String LOG_TAG = WeatherDetailsFragment.class.getSimpleName();

    private static final int REQUEST_CHECK_SETTINGS = 400;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int LOCATION_PERMISSIONS_REQUEST = 500;

    // SaveState keys
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting_location_updates_key";
    private static final String CURRENT_LOCATION_KEY = "current_location_key";
    private static final String LAST_UPDATE_TIME_KEY = "last_update_time_string_key";

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Location mCurrentLocation;
    private LocationRequest mLocationRequest;
    private TextView mLocationView;
    private Date mLastUpdateTime;
    private boolean mRequestingLocationUpdates;
    private State mCurrentState;

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
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        outState.putParcelable(CURRENT_LOCATION_KEY, mCurrentLocation);
        if (mLastUpdateTime == null) {
            outState.putLong(LAST_UPDATE_TIME_KEY, -1L);
        } else {
            outState.putLong(LAST_UPDATE_TIME_KEY, mLastUpdateTime.getTime());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_weather_details, container, false);

        mLocationView = (TextView) rootView.findViewById(R.id.location_textview);

        return rootView;
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
        if (mLocationView != null) {
            mLocationView.setText("Connection suspended - " + i);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mLocationView != null) {
            mLocationView.setText("Connection failed");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                }
                return;
            default:
                return;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = new Date();
        updateUI();
    }

    private void updateUI() {
        if (mLocationView != null && mCurrentLocation != null) {
            mLocationView.setText(mCurrentLocation.toString());
        }
    }

    private void updateValuesFromBundle(Bundle inState) {
        if (inState != null) {
            if (inState.containsKey(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = inState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }

            if (inState.containsKey(CURRENT_LOCATION_KEY)) {
                mCurrentLocation = inState.getParcelable(CURRENT_LOCATION_KEY);
            }

            if (inState.containsKey(LAST_UPDATE_TIME_KEY)
                    && inState.getLong(LAST_UPDATE_TIME_KEY) != -1L) {
                mLastUpdateTime = new Date(inState.getLong(LAST_UPDATE_TIME_KEY));
            }

            updateUI();
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
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
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
