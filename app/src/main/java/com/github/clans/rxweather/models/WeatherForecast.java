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

    public long getTimestamp() {
        return dt;
    }

    public String getConditions() {
        return weather.get(0).getDescription();
    }

    public float getMaxTemp() {
        return temp.max;
    }

    public float getMinTemp() {
        return temp.min;
    }

    public int getWeatherId() {
        return weather.get(0).getId();
    }

    public String getIcon() {
        return weather.get(0).getIcon();
    }
}
