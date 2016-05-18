package com.github.clans.rxweather;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.animation.DecelerateInterpolator;

import com.github.clans.rxweather.adapters.WeatherForecastAdapter;

import java.util.List;

public class WeatherItemAnimator extends DefaultItemAnimator {

    public static final int EXPAND = 1;
    public static final int COLLAPSE = 2;

    @NonNull
    @Override
    public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
                                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                                     int changeFlags,
                                                     @NonNull List<Object> payloads) {

        WeatherItemHolderInfo info = (WeatherItemHolderInfo)
                super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads);
        info.doExpand = payloads.contains(EXPAND);
        info.doCollapse = payloads.contains(COLLAPSE);

        return info;
    }

    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
                                 @NonNull RecyclerView.ViewHolder newHolder,
                                 @NonNull ItemHolderInfo preInfo, @NonNull ItemHolderInfo postInfo) {

        if (newHolder instanceof WeatherForecastAdapter.ViewHolderItem) {
            final WeatherForecastAdapter.ViewHolderItem holder = (WeatherForecastAdapter.ViewHolderItem) newHolder;
            final WeatherItemHolderInfo info = (WeatherItemHolderInfo) preInfo;

            Context context = holder.itemView.getContext();
            int fromColor = ContextCompat.getColor(context, R.color.background_light);
            int toColor = ContextCompat.getColor(context, R.color.lightGray);

            ValueAnimator colorAnimator = null;
            if (info.doExpand) {
                colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
            } else if (info.doCollapse) {
                colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), toColor, fromColor);
            }


            if (colorAnimator != null) {
                colorAnimator.setStartDelay(260);
                colorAnimator.setDuration(300);
                colorAnimator.setInterpolator(new DecelerateInterpolator(2f));
                colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        holder.itemView.setBackgroundColor((int) animation.getAnimatedValue());
                    }
                });
                colorAnimator.start();
            }
        }

        return super.animateChange(oldHolder, newHolder, preInfo, postInfo);
    }

    @Override
    public ItemHolderInfo obtainHolderInfo() {
        return new WeatherItemHolderInfo();
    }

    static class WeatherItemHolderInfo extends ItemHolderInfo {
        boolean doExpand;
        boolean doCollapse;
    }
}
