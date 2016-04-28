package com.github.clans.rxweather.util;

import android.text.TextUtils;

public class Utils {

    public static boolean isNight(String icon) {
        if (TextUtils.isEmpty(icon)) return false;
        char c = icon.charAt(icon.length() - 1);
        return c == 'n';
    }
}
