package com.github.clans.rxweather;

import com.github.clans.rxweather.util.Utils;

public class WeatherIconMapper {

    public static int getWeatherIconRes(int weatherId, String icon) {
        boolean isNight = Utils.isNight(icon);

        if (weatherId == 500) {
            return isNight ? R.drawable.ic_weather_cloud_rain_moon_alt : R.drawable.ic_weather_cloud_rain_sun_alt;
        } else if (weatherId >= 501 && weatherId <= 503) {
            return isNight ? R.drawable.ic_weather_cloud_rain_moon : R.drawable.ic_weather_cloud_rain_sun;
        } else if (weatherId == 800) {
            return isNight ? R.drawable.ic_weather_moon : R.drawable.ic_weather_sun;
        } else if (weatherId >= 801 && weatherId <= 803) {
            return isNight ? R.drawable.ic_weather_cloud_moon : R.drawable.ic_weather_cloud_sun;
        }

            return R.drawable.ic_weather_sun;
    }
}
