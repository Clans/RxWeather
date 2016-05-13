package com.github.clans.rxweather.activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.github.clans.rxweather.LocationHelper;
import com.github.clans.rxweather.R;
import com.github.clans.rxweather.adapters.WeatherForecastAdapter;
import com.github.clans.rxweather.api.WeatherApi;
import com.github.clans.rxweather.dialogs.AddLocationDialogFragment;
import com.github.clans.rxweather.models.Address;
import com.github.clans.rxweather.models.CurrentWeather;
import com.github.clans.rxweather.models.WeatherData;
import com.github.clans.rxweather.models.WeatherForecastEnvelope;
import com.github.clans.rxweather.util.DiskCacheManager;
import com.github.clans.rxweather.util.ItemClickSupport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

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
import timber.log.Timber;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int PERMISSION_LOCATION = 100;
    private static final long LOCATION_TIMEOUT_SECONDS = 10;

    private GoogleApiClient mGoogleApiClient;
    private CompositeSubscription mCompositeSubscription;
    private DiskCacheManager mDiskCache;
    private View mLoadingIndicator;
    private WeatherForecastAdapter mForecastAdapter;
    private int mScrollOffset = 4;
    private FloatingActionButton fab;
    private boolean animate;
    private WeatherApi weatherApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_activity);

        weatherApi = new WeatherApi();
        mCompositeSubscription = new CompositeSubscription();
        try {
            mDiskCache = DiskCacheManager.open(getCacheDir());
        } catch (IOException e) {
            Timber.d("Failed initializing disk cache.");
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .build();
        }

        mLoadingIndicator = findViewById(R.id.loadingIndicator);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        animate = savedInstanceState == null;
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
        Subscription subscription = locationHelper.getLocation()
                .timeout(LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .timestamp()
                .flatMap(new Func1<Timestamped<Location>, Observable<WeatherData>>() {
                    @Override
                    public Observable<WeatherData> call(final Timestamped<Location> timestampedLocation) {
                        final Location location = timestampedLocation.getValue();
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        return getWeatherForecast(lat, lon);
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);

        mCompositeSubscription.add(subscription);
    }

    private Subscriber<WeatherData> subscriber = new Subscriber<WeatherData>() {
        @Override
        public void onCompleted() {
            Timber.d("onCompleted()");
        }

        @Override
        public void onError(Throwable e) {
            Timber.d("onError: %s", e.getMessage());
            hideLoadingIndicator();
        }

        @Override
        public void onNext(WeatherData weatherData) {
            updateUi(weatherData);
        }
    };

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

    private Observable<WeatherData> getWeatherForecast(final double lat, final double lon) {
        final long timestamp = System.currentTimeMillis();
        try {
            return Observable.concat(
                    Observable.just(mDiskCache != null ? mDiskCache.get(lat, lon) : null) // get data from disk cache
                            .filter(new Func1<WeatherData, Boolean>() {
                                @Override
                                public Boolean call(WeatherData weatherData) {
                                    return useCachedData(weatherData, timestamp);

                                }
                            }),
                    Observable.zip(
                            weatherApi.getCurrentWeather(lat, lon),
                            weatherApi.getWeatherForecast(lat, lon),
                            new Func2<CurrentWeather, WeatherForecastEnvelope, WeatherData>() {
                                @Override
                                public WeatherData call(CurrentWeather currentWeather, WeatherForecastEnvelope weatherForecastEnvelope) {
                                    WeatherData weatherData = new WeatherData(currentWeather, weatherForecastEnvelope.getWeatherForecast());
                                    weatherData.setTimestamp(timestamp);
                                    return weatherData;
                                }
                            }) // get data from server and cache it
                            .doOnNext(new Action1<WeatherData>() {
                                @Override
                                public void call(WeatherData weatherData) {
                                    if (mDiskCache != null) {
                                        try {
                                            mDiskCache.put(lat, lon, weatherData);
                                        } catch (IOException e) {
                                            Timber.d("Error putting response into cache: %s", e.getMessage());
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

    private void updateUi(WeatherData weatherData) {
        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
//        list.addItemDecoration(new DividerItemDecoration(this, R.drawable.list_divider));
        if (list.getAdapter() == null) {
            mForecastAdapter = new WeatherForecastAdapter(weatherData);
            mForecastAdapter.setAnimationsLocked(!animate);
            list.setAdapter(mForecastAdapter);
            showFabAnimated(animate);
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

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (Math.abs(dy) > mScrollOffset) {
                    if (dy > 0) {
                        fab.hide();
                    } else {
                        fab.show();
                    }
                }
            }
        });

        ItemClickSupport.addTo(list).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Timber.d("Position clicked: %d", position);
            }
        });

        final AddLocationDialogFragment addLocationDialog = new AddLocationDialogFragment();
        addLocationDialog.setGoogleApiClient(mGoogleApiClient);
        addLocationDialog.setOnItemClickListener(new AddLocationDialogFragment.OnItemClickListener() {
            @Override
            public void onItemClick(Address address) {
                loadPlaceInfo(address);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLocationDialog.show(getSupportFragmentManager(), "addLocation");
            }
        });

        hideLoadingIndicator();
    }

    private void handleError(Throwable e) {
        Timber.d("onError: %s", e.getMessage());
        hideLoadingIndicator();
    }

    private void showFabAnimated(boolean animateFab) {
        if (animateFab) {
            ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(fab,
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f));

            animator.setStartDelay(600);
            animator.setDuration(300);
            animator.setInterpolator(new OvershootInterpolator());
            animator.start();
        } else {
            fab.setAlpha(1f);
        }
    }

    private void hideLoadingIndicator() {
        if (mLoadingIndicator != null) {
            mLoadingIndicator.setVisibility(View.GONE);
        }
    }

    private void loadPlaceInfo(Address address) {
        Places.GeoDataApi.getPlaceById(mGoogleApiClient, address.getPlaceId())
                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(@NonNull PlaceBuffer places) {
                        if (places.getStatus().isSuccess() && places.getCount() > 0) {
                            Place place = places.get(0);
                            LatLng latLng = place.getLatLng();
                            Subscription subscription = getWeatherForecast(latLng.latitude, latLng.longitude)
                                    .subscribe(new Subscriber<WeatherData>() {
                                        @Override
                                        public void onCompleted() {
                                            Timber.d("onCompleted()");
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            handleError(e);
                                        }

                                        @Override
                                        public void onNext(WeatherData weatherData) {
                                            updateUi(weatherData);
                                        }
                                    });
                            mCompositeSubscription.add(subscription);
                        } else {
                            Timber.e("Place not found");
                        }
                        places.release();
                    }
                });
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
