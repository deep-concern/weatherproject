package com.wyattbarnes.weatherproject;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wyatt.barnes on 2016/11/01.
 */

public class HourlyWeatherContentObserver extends ContentObserver {

    public HourlyWeatherContentObserver(Handler handler, View rootView) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
    }
}
