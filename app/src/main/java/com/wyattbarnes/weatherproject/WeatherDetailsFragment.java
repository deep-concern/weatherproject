package com.wyattbarnes.weatherproject;

import android.Manifest;
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
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationServices;

// TODO: refactor to cleanly and consistently get location
// TODO: consider best practice for getting location (maybe move to a background task?)
/**
 * Created by wyatt.barnes on 2016/10/26.
 */
public class WeatherDetailsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final static int LOCATION_PERMISSIONS_REQUEST = 500;

    private GoogleApiClient mGoogleAPiClient;
    private Location mLastLocation;
    private TextView mLocationView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectGoogleApiClient();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleAPiClient != null) {
            mGoogleAPiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleAPiClient != null) {
            mGoogleAPiClient.disconnect();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_weather_details, container, false);

        mLocationView = (TextView) rootView.findViewById(R.id.location_textview);

        return rootView;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(getActivity(), permissions, LOCATION_PERMISSIONS_REQUEST);
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPiClient);
        //mLastLocation = getLastLocation();
        LocationAvailability availability = LocationServices.FusedLocationApi.getLocationAvailability(mGoogleAPiClient);
        System.out.println(mGoogleAPiClient.toString());


        if (mLastLocation != null) {
            if (mLocationView != null) {
                mLocationView.setText(mLastLocation.toString());
            }
        } else {
            if (mLocationView != null) {
                mLocationView.setText("No location given");
            }
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

    private void connectGoogleApiClient() {
        // TODO: Check if the phone needs to update their Google Play Services
        if (mGoogleAPiClient == null) {
            mGoogleAPiClient = new GoogleApiClient.Builder(getActivity())
                    .enableAutoManage(getActivity(), this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPiClient);
                    } catch (SecurityException e) {
                        Log.e(getClass().getSimpleName(), e.getMessage());
                    }
                }
                return;
            default:
                return;
        }
    }
}
