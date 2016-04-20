package com.github.clans.rxweather.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class WeatherForecast {

    private long dt;
    private Temp temp;
    private float pressure; //hPa
    private int humidity;
    private ArrayList<Weather> weather;
    @SerializedName("speed")
    private float windSpeed;
    @SerializedName("deg")
    private int windDirection;

    class Temp {

        public float morn;
        public float day;
        public float eve;
        public float night;
        public float min;
        public float max;
    }

    class Weather {
        public int id;
        public String description;
        public String icon;
    }
}
