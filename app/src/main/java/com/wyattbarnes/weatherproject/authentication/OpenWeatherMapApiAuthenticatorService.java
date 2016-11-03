package com.wyattbarnes.weatherproject.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Dummy authenticator service.
 */

public class OpenWeatherMapApiAuthenticatorService extends Service {
    private OpenWeatherMapApiAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new OpenWeatherMapApiAuthenticator(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
