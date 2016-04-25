package com.github.clans.rxweather;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.clans.rxweather.models.CurrentWeather;
import com.github.clans.rxweather.models.WeatherData;
import com.github.clans.rxweather.models.WeatherForecastEnvelope;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = WeatherActivity.class.getSimpleName();
    private static final int PERMISSION_LOCATION = 100;
    private static final long LOCATION_TIMEOUT_SECONDS = 20;

    private GoogleApiClient mGoogleApiClient;
    private CompositeSubscription mCompositeSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mCompositeSubscription = new CompositeSubscription();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        mCompositeSubscription.unsubscribe();
        super.onStop();
    }

    private void loadData() {
        LocationHelper locationHelper = new LocationHelper(this, mGoogleApiClient);
        locationHelper.getLocation()
                .timeout(LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .flatMap(new Func1<Location, Observable<WeatherData>>() {
                    @Override
                    public Observable<WeatherData> call(Location location) {
                        WeatherApi weatherApi = new WeatherApi();
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        return Observable.zip(
                                weatherApi.getCurrentWeather(lat, lon),
                                weatherApi.getWeatherForecast(lat, lon),
                                new Func2<CurrentWeather, WeatherForecastEnvelope, WeatherData>() {
                                    @Override
                                    public WeatherData call(CurrentWeather currentWeather, WeatherForecastEnvelope weatherForecastEnvelope) {
                                        return new WeatherData(currentWeather, weatherForecastEnvelope.getWeatherForecast());
                                    }
                                })
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread());
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<WeatherData>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                    }

                    @Override
                    public void onNext(WeatherData weatherData) {
                        updateUi(weatherData);
                    }
                });
    }

    private void updateUi(WeatherData weatherData) {
        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
//        list.addItemDecoration(new DividerItemDecoration(this, R.drawable.list_divider));
        final WeatherForecastAdapter forecastAdapter = new WeatherForecastAdapter(weatherData);
        list.setAdapter(forecastAdapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    forecastAdapter.setAnimationsLocked(true);
                }
            }
        });

        findViewById(R.id.loadingIndicator).setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadData();
                } else {

                }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_LOCATION);
        } else {
            loadData();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
