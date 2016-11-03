package com.wyattbarnes.weatherproject.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by wyatt.barnes on 2016/11/01.
 */

public class OpenWeatherApiAuthenticatorService extends Service {
    private OpenWeatherApiAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new OpenWeatherApiAuthenticator(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
