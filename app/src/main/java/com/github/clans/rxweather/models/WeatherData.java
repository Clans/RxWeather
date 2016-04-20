package com.github.clans.rxweather.models;

import java.util.List;

public class WeatherData {

    private CurrentWeather currentWeather;
    private List<WeatherForecast> weatherForecast;

    public WeatherData(CurrentWeather currentWeather, List<WeatherForecast> weatherForecast) {
        this.currentWeather = currentWeather;
        this.weatherForecast = weatherForecast;
    }

    public CurrentWeather getCurrentWeather() {
        return currentWeather;
    }

    public List<WeatherForecast> getWeatherForecast() {
        return weatherForecast;
    }
}
