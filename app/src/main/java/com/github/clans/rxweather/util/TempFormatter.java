package com.github.clans.rxweather.util;

public class TempFormatter {

    public static String format(float temperature) {
        return String.valueOf(Math.round(temperature)) + "°";
    }
}
