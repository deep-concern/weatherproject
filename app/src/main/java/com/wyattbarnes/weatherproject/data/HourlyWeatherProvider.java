package com.wyattbarnes.weatherproject.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.text.DecimalFormat;

/**
 * Main content provider for hourly weather updates per city.
 */
public class HourlyWeatherProvider extends ContentProvider {
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final String LOG_TAG = HourlyWeatherProvider.class.getSimpleName();
    private HourlyWeatherOpenHelper mOpenHelper;

    private static final int HOURLY_WEATHER = 100;
    private static final int HOURLY_WEATHER_WITH_CITY_ID = 101;
    private static final int HOURLY_WEATHER_WITH_CITY_ID_AND_DATE = 102;
    private static final int CITY = 300;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#####");

    private static final SQLiteQueryBuilder sHourlyWeatherByCoordinatesQueryBuilder;

    static {
        sHourlyWeatherByCoordinatesQueryBuilder = new SQLiteQueryBuilder();

        sHourlyWeatherByCoordinatesQueryBuilder.setTables(
                HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME + " INNER JOIN "
                + HourlyWeatherContract.CityEntry.TABLE_NAME
                + " ON " + HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME
                + "." + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_CITY_KEY
                + " = " + HourlyWeatherContract.CityEntry.TABLE_NAME
                + "." + HourlyWeatherContract.CityEntry._ID
        );
    }

    private static final String sCityCoordinatesSelection =
            HourlyWeatherContract.CityEntry.TABLE_NAME
            + "." + HourlyWeatherContract.CityEntry._ID + " = ? ";

    private static final String sCityCoordinatesAndStartDateSelection =
            HourlyWeatherContract.CityEntry.TABLE_NAME
            + "." + HourlyWeatherContract.CityEntry._ID + " = ? AND "
            + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE + " >= ? ";

    private static final String sCityCoordinatesAndDateSelection =
            HourlyWeatherContract.CityEntry.TABLE_NAME
            + "." + HourlyWeatherContract.CityEntry._ID + " = ? AND "
            + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE + " = ? ";


    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = HourlyWeatherContract.CONTENT_AUTHORITY;

        // Hourly weather matching
        matcher.addURI(authority, HourlyWeatherContract.PATH_HOURLY_WEATHER,
                HOURLY_WEATHER);
        matcher.addURI(authority, HourlyWeatherContract.PATH_HOURLY_WEATHER + "/*",
                HOURLY_WEATHER_WITH_CITY_ID);
        matcher.addURI(authority, HourlyWeatherContract.PATH_HOURLY_WEATHER + "/*/*",
                HOURLY_WEATHER_WITH_CITY_ID_AND_DATE);

        // City matching
        matcher.addURI(authority, HourlyWeatherContract.PATH_CITY, CITY);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new HourlyWeatherOpenHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch(sUriMatcher.match(uri)) {
            case HOURLY_WEATHER:
                return HourlyWeatherContract.HourlyWeatherEntry.CONTENT_TYPE;
            case HOURLY_WEATHER_WITH_CITY_ID:
                return HourlyWeatherContract.HourlyWeatherEntry.CONTENT_TYPE;
            case HOURLY_WEATHER_WITH_CITY_ID_AND_DATE:
                return HourlyWeatherContract.HourlyWeatherEntry.CONTENT_ITEM_TYPE;
            case CITY:
                return HourlyWeatherContract.CityEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;

        // Determines what kind of request based on uri
        switch (sUriMatcher.match(uri)) {
            case HOURLY_WEATHER:
                cursor = mOpenHelper.getReadableDatabase().query(
                        HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case HOURLY_WEATHER_WITH_CITY_ID:
                cursor = getHourlyWeatherByCoordinates(uri, projection, sortOrder);
                break;
            case HOURLY_WEATHER_WITH_CITY_ID_AND_DATE:
                cursor = getHourlyWeatherByCoordinatesAndDate(uri, projection, sortOrder);
                break;
            case CITY:
                cursor = mOpenHelper.getReadableDatabase().query(
                        HourlyWeatherContract.CityEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case HOURLY_WEATHER:
                long hourlyWeatherId = db.insert(HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME, null, contentValues);
                if (hourlyWeatherId > 0) {
                    returnUri = HourlyWeatherContract.HourlyWeatherEntry.buildHourlyWeatherUri(hourlyWeatherId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case CITY:
                long cityId = db.insert(HourlyWeatherContract.CityEntry.TABLE_NAME, null, contentValues);
                if (cityId > 0) {
                    returnUri = HourlyWeatherContract.CityEntry.buildCityUri(cityId);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        if (selection == null) {
            selection = "1";
        }

        switch (match) {
            case HOURLY_WEATHER:
                rowsDeleted = db.delete(
                        HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            case CITY:
                rowsDeleted = db.delete(
                        HourlyWeatherContract.CityEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case HOURLY_WEATHER:
                rowsUpdated = db.update(HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME,
                        contentValues, selection, selectionArgs);
                break;
            case CITY:
                rowsUpdated = db.update(HourlyWeatherContract.CityEntry.TABLE_NAME,
                        contentValues, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case HOURLY_WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }

    private Cursor getHourlyWeatherByCoordinates(Uri uri, String[] projection, String sortOrder) {
        long cityId = HourlyWeatherContract.HourlyWeatherEntry.getCityIdFromUri(uri);
        long startDate = HourlyWeatherContract.HourlyWeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = sCityCoordinatesSelection;
            selectionArgs = new String[] {Long.toString(cityId)};
        } else {
            selection = sCityCoordinatesAndStartDateSelection;
            selectionArgs = new String[] {Long.toString(cityId), Long.toString(startDate)};
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        SQLiteStatement smt = db.compileStatement(sHourlyWeatherByCoordinatesQueryBuilder.buildQuery(projection, selection, null, null, sortOrder, null));
        smt.bindAllArgsAsStrings(selectionArgs);

        return sHourlyWeatherByCoordinatesQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getHourlyWeatherByCoordinatesAndDate(Uri uri, String[] projection, String sortOrder) {
        long cityId = HourlyWeatherContract.HourlyWeatherEntry.getCityIdFromUri(uri);
        long date = HourlyWeatherContract.HourlyWeatherEntry.getDateFromUri(uri);

        return sHourlyWeatherByCoordinatesQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sCityCoordinatesAndDateSelection,
                new String[] {Long.toString(cityId), Long.toString(date)},
                null,
                null,
                sortOrder
        );
    }
}
