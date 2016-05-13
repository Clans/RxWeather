package com.github.clans.rxweather.util;

import com.github.clans.rxweather.R;
import com.github.clans.rxweather.util.Utils;

public class WeatherIconMapper {

    public static int getWeatherIconRes(int weatherId, String icon) {
        boolean isNight = Utils.isNight(icon);

        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_weather_cloud_lightning;
        } else if (weatherId >= 300 && weatherId <= 310) {
            return isNight ? R.drawable.ic_weather_cloud_drizzle_moon : R.drawable.ic_weather_cloud_drizzle_sun;
        } else if (weatherId >= 311 && weatherId <= 321) {
            return R.drawable.ic_weather_cloud_drizzle;
        } else if (weatherId == 500) {
            return isNight ? R.drawable.ic_weather_cloud_rain_moon_alt : R.drawable.ic_weather_cloud_rain_sun_alt;
        } else if (weatherId >= 501 && weatherId <= 503) {
            return isNight ? R.drawable.ic_weather_cloud_rain_moon : R.drawable.ic_weather_cloud_rain_sun;
        } else if (weatherId >= 504 && weatherId <= 531) {
            return R.drawable.ic_weather_cloud_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_weather_cloud_snow_alt;
        } else if (weatherId >= 701 && weatherId <= 781) {
            return R.drawable.ic_weather_cloud_fog;
        } else if (weatherId == 800 || weatherId == 951) {
            return isNight ? R.drawable.ic_weather_moon : R.drawable.ic_weather_sun;
        } else if (weatherId >= 801 && weatherId <= 803) {
            return isNight ? R.drawable.ic_weather_cloud_moon : R.drawable.ic_weather_cloud_sun;
        } else if (weatherId == 804) {
            return R.drawable.ic_weather_cloud;
        } else if (weatherId == 900) {
            return R.drawable.ic_weather_tornado;
        } else if (weatherId == 901 || weatherId == 902 || weatherId == 905 || (weatherId >= 956 && weatherId <= 962)) {
            return R.drawable.ic_weather_wind;
        } else if (weatherId == 903) {
            return R.drawable.ic_weather_thermometer_zero;
        } else if (weatherId == 904) {
            return R.drawable.ic_weather_thermometer_75;
        } else if (weatherId == 906) {
            return R.drawable.ic_weather_cloud_hail_alt;
        } else if (weatherId >= 952 && weatherId <= 955) {
            return isNight ? R.drawable.ic_weather_cloud_wind_moon : R.drawable.ic_weather_cloud_wind_sun;
        }

        return R.drawable.ic_weather_sun;
    }
}
