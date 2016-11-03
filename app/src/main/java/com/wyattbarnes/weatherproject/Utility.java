package com.wyattbarnes.weatherproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * Utility function class.
 */
public class Utility {
    // Just setting these
    private static final float DEFAULT_LATITUDE = 37.422F;
    private static final float DEFAULT_LONGITUDE = -122.083F;

    // TODO: consider removing
    public static double getStoredLatitude(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return (double) preferences.getFloat(
                context.getString(R.string.pref_latitude_key),
                DEFAULT_LATITUDE
        );
    }

    // TODO: consider removing
    public static double getStoredLongitude(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return (double) preferences.getFloat(
                context.getString(R.string.pref_longitude_key),
                DEFAULT_LONGITUDE
        );
    }

    /**
     * Formats the date into an easier to read format.
     *
     * @param date
     * @return
     */
    public static String formatDate(long date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(date);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:00");
        format.setCalendar(calendar);
        return format.format(calendar.getTime());
    }
}
