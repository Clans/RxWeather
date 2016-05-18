package com.github.clans.rxweather.util;

public class WindFormatter {

    private static final int STEP = 45;
    private static final int MASK = 8;
    private static final String[] DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    public static String format(float windSpeed, int windDirection) {
        double wind = Math.round(windSpeed * 1e1) / 1e1;
        int idx = (windDirection / STEP) % MASK;
        return wind + " m/s " + DIRECTIONS[idx];
    }
}
