package com.wyattbarnes.weatherproject.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by wyatt.barnes on 2016/10/31.
 */

public class HourlyWeatherOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 10;

    private static final String DATABASE_NAME = "hourlyweather.db";

    public HourlyWeatherOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // SQL script for creating our city table
        final String SQL_CREATE_CITY_TABLE = "CREATE TABLE "
                + HourlyWeatherContract.CityEntry.TABLE_NAME + " ("
                + HourlyWeatherContract.CityEntry._ID + " INTEGER PRIMARY KEY, "
                + HourlyWeatherContract.CityEntry.COLUMN_CITY_NAME + " TEXT NOT NULL, "
                + HourlyWeatherContract.CityEntry.COLUMN_CITY_COUNTRY + " TEXT NOT NULL, "
                + HourlyWeatherContract.CityEntry.COLUMN_CITY_LATITUDE + " REAL NOT NULL, "
                + HourlyWeatherContract.CityEntry.COLUMN_CITY_LONGITUDE + " REAL NOT NULL "
                + " )";

        final String SQL_CREATE_HOURLY_WEATHER_TABLE = "CREATE TABLE "
                + HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME + " ("
                + HourlyWeatherContract.HourlyWeatherEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_CITY_KEY + " INTEGER NOT NULL, "
                + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE + " INTEGER NOT NULL, "
                + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_WEATHER_DESCRIPTION + " TEXT NOT NULL, "
                + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_WEATHER_ID + " INTEGER NOT NULL, "
                + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_TEMP_MAX + " REAL NOT NULL, "
                + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_TEMP_MIN + " REAL NOT NULL, "

                + " FOREIGN KEY (" + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_CITY_KEY
                + ") REFERENCES " + HourlyWeatherContract.CityEntry.TABLE_NAME
                + " (" + HourlyWeatherContract.CityEntry._ID + " ), UNIQUE ("
                + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_DATE + ", "
                + HourlyWeatherContract.HourlyWeatherEntry.COLUMN_CITY_KEY + ") ON CONFLICT REPLACE)";

        sqLiteDatabase.execSQL(SQL_CREATE_CITY_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_HOURLY_WEATHER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // Dropping the tables on upgrade for development quickness
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "
                + HourlyWeatherContract.CityEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "
                + HourlyWeatherContract.HourlyWeatherEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
