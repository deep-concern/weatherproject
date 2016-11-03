package com.wyattbarnes.weatherproject.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Calendar;

/**
 * Created by wyatt.barnes on 2016/10/31.
 */

public class HourlyWeatherContract {
    public static final String CONTENT_AUTHORITY = "com.wyattbarnes.weatherproject";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_CITY = "city";
    public static final String PATH_HOURLY_WEATHER = "hourlyweather";

    public static final class CityEntry implements BaseColumns {
        private static final String LOG_TAG = CityEntry.class.getSimpleName();

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_CITY).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/" + CONTENT_AUTHORITY
                + "/" + PATH_CITY;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/" + CONTENT_AUTHORITY
                + "/" + PATH_CITY;

        public static final String TABLE_NAME = "city";

        public static final String COLUMN_CITY_NAME = "city_name";
        public static final String COLUMN_CITY_COUNTRY = "country";
        public static final String COLUMN_CITY_LATITUDE = "latitude";
        public static final String COLUMN_CITY_LONGITUDE = "longitude";

        public static Uri buildCityUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class HourlyWeatherEntry implements BaseColumns {
        private static final String LOG_TAG = HourlyWeatherEntry.class.getSimpleName();

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_HOURLY_WEATHER).build();

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/" + CONTENT_AUTHORITY
                + "/" + PATH_HOURLY_WEATHER;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/" + CONTENT_AUTHORITY
                + "/" + PATH_HOURLY_WEATHER;

        public static final String TABLE_NAME = "hourlyweather";

        public static final String COLUMN_CITY_KEY = "city_id";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_WEATHER_ID = "weather_id";
        public static final String COLUMN_WEATHER_DESCRIPTION = "description";
        public static final String COLUMN_TEMP_MAX = "temp_max";
        public static final String COLUMN_TEMP_MIN = "temp_min";

        public static Uri buildHourlyWeatherUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildHourlyWeatherWithCityId(long cityId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(cityId))
                    .build();
        }

        public static Uri buildHourlyWeatherWithCityIdAndDate(long cityId, long startDate) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(cityId))
                    .appendPath(Long.toString(normalizeDate(startDate)))
                    .build();
        }

        public static Uri buildHourlyWeatherWithCityIdAndStartDate(long cityId, long startDate) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(cityId))
                    .appendQueryParameter(COLUMN_DATE, Long.toString(normalizeDate(startDate)))
                    .build();

        }

        public static Long getCityIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }

        public static long getDateFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(3));
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (null != dateString && !dateString.isEmpty()) {
                return Long.parseLong(dateString);
            } else {
                return 0;
            }
        }
    }

    static long normalizeDate(long startDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startDate);
        calendar.set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DATE),
                calendar.get(Calendar.HOUR),
                0,
                0);
        return calendar.getTimeInMillis();
    }
}
