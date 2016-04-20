package com.github.clans.rxweather;

import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateFormatter {

    private final static long MILLISECONDS_IN_SECONDS = 1000;

    public static String format(long timestamp) {
        long millis = timestamp * MILLISECONDS_IN_SECONDS;
        Calendar today = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        if (Math.abs(dayOfYear - today.get(Calendar.DAY_OF_YEAR)) < 2) {
            return DateUtils.getRelativeTimeSpanString(
                    millis,
                    new Date().getTime(),
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_WEEKDAY).toString();
        }

        return getWeekDay(calendar);
    }

    private static String getWeekDay(Calendar calendar) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
        return dayFormat.format(calendar.getTimeInMillis());
    }
}
