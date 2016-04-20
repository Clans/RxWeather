package com.github.clans.rxweather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import rx.Observable;
import rx.Subscriber;

public class LocationHelper {

    private final Context mContext;
    private final GoogleApiClient mGoogleApiClient;

    public LocationHelper(Context context, GoogleApiClient googleApiClient) {
        mContext = context;
        mGoogleApiClient = googleApiClient;
    }

    public Observable<Location> getLocation() {
        return Observable.create(new Observable.OnSubscribe<Location>() {
            @Override
            public void call(final Subscriber<? super Location> subscriber) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                final Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location != null) {
                    subscriber.onNext(location);
                    subscriber.onCompleted();
                } else {
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setNumUpdates(1);
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    locationRequest.setMaxWaitTime(2000);

                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                            locationRequest, new LocationListener() {
                                @Override
                                public void onLocationChanged(Location location) {
                                    subscriber.onNext(location);
                                    subscriber.onCompleted();
                                }
                            });

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Location defaultLocation = new Location("");
                            defaultLocation.setLatitude(37.422);
                            defaultLocation.setLongitude(-122.084);
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(defaultLocation);
                                subscriber.onCompleted();
                            }
                        }
                    }, 2100);
                }
            }
        });
    }
}
