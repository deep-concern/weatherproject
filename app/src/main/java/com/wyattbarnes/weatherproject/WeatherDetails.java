package com.wyattbarnes.weatherproject;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.wyattbarnes.weatherproject.authentication.OpenWeatherApiSyncAdapter;

public class WeatherDetails extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_details);
    }
}
