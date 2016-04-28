package com.github.clans.rxweather;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.github.clans.rxweather.api.WeatherApi;
import com.github.clans.rxweather.models.CurrentWeather;
import com.github.clans.rxweather.models.WeatherData;
import com.github.clans.rxweather.models.WeatherForecastEnvelope;
import com.github.clans.rxweather.util.DiskCacheManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.schedulers.Timestamped;
import rx.subscriptions.CompositeSubscription;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = WeatherActivity.class.getSimpleName();
    private static final int PERMISSION_LOCATION = 100;
    private static final long LOCATION_TIMEOUT_SECONDS = 10;

    private GoogleApiClient mGoogleApiClient;
    private CompositeSubscription mCompositeSubscription;
    private DiskCacheManager mDiskCache;
    private View mLoadingIndicator;
    private WeatherForecastAdapter mForecastAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_activity);

        mCompositeSubscription = new CompositeSubscription();
        try {
            mDiskCache = DiskCacheManager.open(getCacheDir());
        } catch (IOException e) {
            Log.d(TAG, "Failed initializing disk cache.");
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mLoadingIndicator = findViewById(R.id.loadingIndicator);
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
        // TODO: try Android-ReactiveLocation
        LocationHelper locationHelper = new LocationHelper(this, mGoogleApiClient);
        Subscription subscription = locationHelper.getLocation()
                .timeout(LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .timestamp()
                .flatMap(new Func1<Timestamped<Location>, Observable<WeatherData>>() {
                    @Override
                    public Observable<WeatherData> call(final Timestamped<Location> timestampedLocation) {
                        WeatherApi weatherApi = new WeatherApi();
                        final Location location = timestampedLocation.getValue();
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        try {
                            return Observable.concat(
                                    Observable.just(mDiskCache != null ? mDiskCache.get(location) : null) // get data from disk cache
                                            .filter(new Func1<WeatherData, Boolean>() {
                                                @Override
                                                public Boolean call(WeatherData weatherData) {
                                                    return useCachedData(weatherData, timestampedLocation.getTimestampMillis());

                                                }
                                            }),
                                    Observable.zip(
                                            weatherApi.getCurrentWeather(lat, lon),
                                            weatherApi.getWeatherForecast(lat, lon),
                                            new Func2<CurrentWeather, WeatherForecastEnvelope, WeatherData>() {
                                                @Override
                                                public WeatherData call(CurrentWeather currentWeather, WeatherForecastEnvelope weatherForecastEnvelope) {
                                                    WeatherData weatherData = new WeatherData(currentWeather, weatherForecastEnvelope.getWeatherForecast());
                                                    weatherData.setTimestamp(timestampedLocation.getTimestampMillis());
                                                    return weatherData;
                                                }
                                            }) // get data from server and cache it
                                            .doOnNext(new Action1<WeatherData>() {
                                                @Override
                                                public void call(WeatherData weatherData) {
                                                    if (mDiskCache != null) {
                                                        try {
                                                            mDiskCache.put(location, weatherData);
                                                        } catch (IOException e) {
                                                            Log.d(TAG, "Error putting response into cache: " + e.getMessage());
                                                        }
                                                    }
                                                }
                                            })
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread()))
                                    .first();
                        } catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<WeatherData>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted: ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());
                        hideLoadingIndicator();
                    }

                    @Override
                    public void onNext(WeatherData weatherData) {
                        updateUi(weatherData);
                    }
                });

        mCompositeSubscription.add(subscription);
    }

    private boolean useCachedData(WeatherData weatherData, long currentTimestamp) {
        if (weatherData == null) return false;

        long weatherTimestamp = weatherData.getTimestamp();

        Calendar currentCal = Calendar.getInstance();
        currentCal.setTimeInMillis(currentTimestamp);

        Calendar cachedCal = Calendar.getInstance();
        cachedCal.setTimeInMillis(weatherTimestamp);

        int cachedHourOfDay = cachedCal.get(Calendar.HOUR_OF_DAY);
        int currentHourOfDay = currentCal.get(Calendar.HOUR_OF_DAY);

        return Math.abs(currentHourOfDay - cachedHourOfDay) <= 2;
    }

    private void updateUi(WeatherData weatherData) {
        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
//        list.addItemDecoration(new DividerItemDecoration(this, R.drawable.list_divider));
        if (list.getAdapter() == null) {
            mForecastAdapter = new WeatherForecastAdapter(weatherData);
            list.setAdapter(mForecastAdapter);
        } else {
            mForecastAdapter.update(weatherData);
        }

        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    mForecastAdapter.setAnimationsLocked(true);
                }
            }
        });

        hideLoadingIndicator();
    }

    private void hideLoadingIndicator() {
        if (mLoadingIndicator != null) {
            mLoadingIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadData();
                } else {
                    // TODO: handle deny
                    hideLoadingIndicator();
                }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
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
