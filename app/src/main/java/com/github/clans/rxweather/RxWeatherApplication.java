package com.github.clans.rxweather;

import android.app.Application;
import android.util.Log;

import timber.log.Timber;

public class RxWeatherApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
    }

    private static class CrashReportingTree extends Timber.Tree {

        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) return;

            //TODO: report a non-fatal error
        }
    }

}
