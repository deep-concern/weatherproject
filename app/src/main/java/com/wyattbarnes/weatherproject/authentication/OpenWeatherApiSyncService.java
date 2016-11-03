package com.wyattbarnes.weatherproject.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by wyatt.barnes on 2016/11/01.
 */

public class OpenWeatherApiSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static OpenWeatherApiSyncAdapter sOpenWeatherApiSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sOpenWeatherApiSyncAdapter == null) {
                sOpenWeatherApiSyncAdapter = new OpenWeatherApiSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return sOpenWeatherApiSyncAdapter.getSyncAdapterBinder();
    }
}
