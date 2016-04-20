package com.github.clans.rxweather;

import com.github.clans.rxweather.models.CurrentWeather;
import com.github.clans.rxweather.models.WeatherForecastEnvelope;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public class WeatherApi {

    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/";

    private WeatherService mWeatherService;

    public WeatherApi() {
        mWeatherService = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .build()
                .create(WeatherService.class);
    }

    public Observable<CurrentWeather> getCurrentWeather(double lat, double lon) {
        return mWeatherService.getCurrentWeather(lat, lon);
    }

    public Observable<WeatherForecastEnvelope> getWeatherForecast(double lat, double lon) {
        return mWeatherService.getWeatherForecast(lat, lon);
    }

    private interface WeatherService {

        @GET("weather?units=metric&mode=json&appid=8214ce54696c9025fb5264abfbef7096")
        Observable<CurrentWeather> getCurrentWeather(@Query("lat") double lat, @Query("lon") double lon);

        @GET("forecast/daily?mode=json&units=metric&cnt=7&appid=8214ce54696c9025fb5264abfbef7096")
        Observable<WeatherForecastEnvelope> getWeatherForecast(@Query("lat") double lat, @Query("lon") double lon);
    }
}
