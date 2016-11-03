package com.wyattbarnes.weatherproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * Created by wyatt.barnes on 2016/11/01.
 */

public class Utility {
    // Just setting these
    private static final long DEFAULT_CITY_ID = -1;
    private static final float DEFAULT_LATITUDE = 37.422F;
    private static final float DEFAULT_LONGITUDE = -122.083F;

    public static long getStoredCityId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getLong(context.getString(R.string.pref_city_id_key), DEFAULT_CITY_ID);
    }

    public static double getStoredLatitude(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return (double) preferences.getFloat(
                context.getString(R.string.pref_latitude_key),
                DEFAULT_LATITUDE
        );
    }

    public static double getStoredLongitude(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return (double) preferences.getFloat(
                context.getString(R.string.pref_longitude_key),
                DEFAULT_LONGITUDE
        );
    }

    public static String formatDate(long date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(date);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:00");
        format.setCalendar(calendar);
        return format.format(calendar.getTime());
    }
}
