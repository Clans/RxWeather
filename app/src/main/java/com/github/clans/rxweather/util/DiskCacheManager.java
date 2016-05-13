package com.github.clans.rxweather.util;

import android.location.Location;
import android.os.Looper;

import com.github.clans.rxweather.models.WeatherData;
import com.google.gson.Gson;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class DiskCacheManager {

    private static final int DISK_CACHE_INDEX = 0;
    private static final long DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5Mb

    private DiskLruCache mDiskLruCache;
    private Gson mGson;

    private DiskCacheManager(File directory) throws IOException {
        mGson = new Gson();
        mDiskLruCache = DiskLruCache.open(directory, 1, 1, DISK_CACHE_SIZE);
    }

    public static synchronized DiskCacheManager open(File directory) throws IOException {
        return new DiskCacheManager(directory);
    }

    public synchronized WeatherData get(double lat, double lon) throws IOException {
        String key = getKey(lat, lon);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        if (snapshot != null) {
            String string = snapshot.getString(DISK_CACHE_INDEX);
            WeatherData weatherData = mGson.fromJson(string, WeatherData.class);
            snapshot.close();
            return weatherData;
        }

        return null;
    }

    public synchronized void put(double lat, double lon, WeatherData weatherData) throws IOException {
        OutputStream bos = null;
        DiskLruCache.Editor editor;
        try {
            editor = mDiskLruCache.edit(getKey(lat, lon));
            if (editor != null) {
                bos = editor.newOutputStream(DISK_CACHE_INDEX);
                String json = mGson.toJson(weatherData);
                bos.write(json.getBytes());
                editor.commit();
            }
        } catch (IOException e) {
            throw new IOException(e);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private String getKey(double latitude, double longitude) {
        double lat = Math.floor(latitude * 1e2) / 1e2;
        double lon = Math.floor(longitude * 1e2) / 1e2;
        String key = lat + "_" + lon;
        return key.replace(".", "");
    }
}
