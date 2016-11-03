package com.wyattbarnes.weatherproject;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by wyatt.barnes on 2016/11/01.
 */

public class WeatherDetailsAdapter extends CursorAdapter {
    private static final int VIEW_TYPE_WEATHER_WITH_LOCATION = 0;
    private static final int VIEW_TYPE_WEATHER_ONLY = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    public WeatherDetailsAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int layoutId = -1;
        switch (getItemViewType(cursor.getPosition())) {
            case VIEW_TYPE_WEATHER_WITH_LOCATION:
                layoutId = R.layout.list_first_item_hourlyweather;
                break;
            case VIEW_TYPE_WEATHER_ONLY:
                layoutId = R.layout.list_other_item_hourlyweather;
                break;
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int viewType = getItemViewType(cursor.getPosition());

        if (viewType == VIEW_TYPE_WEATHER_WITH_LOCATION) {
            String cityName = cursor.getString(WeatherDetailsFragment.COLUMN_CITY_NAME);
            String cityCountry = cursor.getString(WeatherDetailsFragment.COLUMN_CITY_COUNTRY);
            viewHolder.locationView.setText(cityName + ", " + cityCountry);
        }

        // Get the tempurature
        String maxTempurature = cursor.getString(WeatherDetailsFragment.COLUMN_HOURLY_WEATHER_TEMP_MAX);
        viewHolder.maxTempView.setText(maxTempurature + "°");
        String minTempurature = cursor.getString(WeatherDetailsFragment.COLUMN_HOURLY_WEATHER_TEMP_MIN);
        viewHolder.minTempView.setText(minTempurature + "°");

        // TODO: Add icon for weather
        // Get the weather's description
        String description = cursor.getString(WeatherDetailsFragment.COLUMN_HOURLY_WEATHER_DESCRIPTION);
        viewHolder.weatherDescriptionView.setText(description);

        // Display the date
        long date = cursor.getLong(WeatherDetailsFragment.COLUMN_HOURLY_WEATHER_DATE);
        viewHolder.dateTextView.setText(Utility.formatDate(date));
    }

    public static class ViewHolder {
        public final TextView locationView;
        public final TextView minTempView;
        public final TextView maxTempView;
        public final TextView weatherDescriptionView;
        public final TextView dateTextView;

        public ViewHolder(View view) {
            locationView = (TextView) view.findViewById(R.id.list_item_location_textview);
            minTempView = (TextView) view.findViewById(R.id.list_item_min_temp_textView);
            maxTempView = (TextView) view.findViewById(R.id.list_item_max_temp_textView);
            weatherDescriptionView = (TextView) view.findViewById(R.id.list_item_weather_description_textview);
            dateTextView = (TextView) view.findViewById(R.id.list_item_date_textview);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_WEATHER_WITH_LOCATION : VIEW_TYPE_WEATHER_ONLY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
}
