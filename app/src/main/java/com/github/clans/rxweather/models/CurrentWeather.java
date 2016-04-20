package com.github.clans.rxweather.models;

import com.github.clans.rxweather.TempFormatter;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class CurrentWeather {

    private String name;
    private ArrayList<Weather> weather;
    private Main main;

    class Weather {
        public String description;
    }

    class Main {
        public float temp;
        @SerializedName("temp_min")
        public float tempMin;
        @SerializedName("temp_max")
        public float tempMax;
    }

    public String getLocationName() {
        return name;
    }

    public String getCurrentConditions() {
        return weather.size() > 0 ? weather.get(0).description : "";
    }

    public String getCurrentTemp() {
        return TempFormatter.format(main.temp);
    }
}
