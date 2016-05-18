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
    private int type;
    private boolean expanded;

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

    public float getWindSpeed() {
        return windSpeed;
    }

    public int getWindDirection() {
        return windDirection;
    }

    public int getHumidity() {
        return humidity;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public float getPressure() {
        return pressure;
    }

    public WeatherForecast copy() {
        WeatherForecast wf = new WeatherForecast();
        wf.dt = this.dt;
        wf.temp = this.temp;
        wf.pressure = this.pressure;
        wf.humidity = this.humidity;
        wf.weather = new ArrayList<>(this.weather);
        wf.windSpeed = this.windSpeed;
        wf.windDirection = this.windDirection;
        wf.type = this.type;
        return wf;
    }
}
