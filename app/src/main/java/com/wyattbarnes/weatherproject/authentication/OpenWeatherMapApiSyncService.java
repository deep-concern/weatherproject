package com.wyattbarnes.weatherproject.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Service for the OWM API SyncAdapter.
 */
public class OpenWeatherMapApiSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static OpenWeatherMapApiSyncAdapter sOpenWeatherMapApiSyncAdapter = null;

    @Override
    public void onCreate() {
        // Synchronize, as this may fire multiple times per location change
        synchronized (sSyncAdapterLock) {
            if (sOpenWeatherMapApiSyncAdapter == null) {
                sOpenWeatherMapApiSyncAdapter = new OpenWeatherMapApiSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return sOpenWeatherMapApiSyncAdapter.getSyncAdapterBinder();
    }
}
