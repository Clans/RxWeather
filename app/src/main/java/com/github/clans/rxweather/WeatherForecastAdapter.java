package com.github.clans.rxweather;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.github.clans.rxweather.models.CurrentWeather;
import com.github.clans.rxweather.models.WeatherData;
import com.github.clans.rxweather.models.WeatherForecast;

import java.util.List;

public class WeatherForecastAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<WeatherForecast> forecast;
    private CurrentWeather currentWeather;
    private int lastAnimatedPosition = -1;
    private boolean animationsLocked = false;

    public WeatherForecastAdapter(WeatherData weatherData) {
        this.forecast = weatherData.getWeatherForecast();
        this.currentWeather = weatherData.getCurrentWeather();
    }

    public static class ViewHolderItem extends RecyclerView.ViewHolder {

        private TextView day;
        private TextView conditions;
        private TextView maxTemp;
        private TextView minTemp;

        public ViewHolderItem(View itemView) {
            super(itemView);
            day = (TextView) itemView.findViewById(R.id.day);
            conditions = (TextView) itemView.findViewById(R.id.conditions);
            maxTemp = (TextView) itemView.findViewById(R.id.maxTemp);
            minTemp = (TextView) itemView.findViewById(R.id.minTemp);
        }
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {

        private TextView locationName;
        private TextView currentConditions;
        private TextView temp;

        public ViewHolderHeader(View itemView) {
            super(itemView);
            locationName = (TextView) itemView.findViewById(R.id.locationName);
            currentConditions = (TextView) itemView.findViewById(R.id.currentConditions);
            temp = (TextView) itemView.findViewById(R.id.temp);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_HEADER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_header, parent, false);
            return new ViewHolderHeader(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_weather, parent, false);
            return new ViewHolderItem(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderHeader) {
            ViewHolderHeader vhh = (ViewHolderHeader) holder;
            vhh.locationName.setText(currentWeather.getLocationName());
            vhh.currentConditions.setText(currentWeather.getCurrentConditions());
            vhh.temp.setText(currentWeather.getCurrentTemp());
        } else {
            runEnterAnimation(holder.itemView, position);

            WeatherForecast forecast = getForecastItem(position);
            if (forecast != null) {
                ViewHolderItem vhi = (ViewHolderItem) holder;
                vhi.day.setText(DateFormatter.format(forecast.getTimestamp()));
                vhi.conditions.setText(forecast.getConditions());
                vhi.maxTemp.setText(TempFormatter.format(forecast.getMaxTemp()));
                vhi.minTemp.setText(TempFormatter.format(forecast.getMinTemp()));
            }
        }
    }

    @Override
    public int getItemCount() {
        return forecast.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    private void runEnterAnimation(View view, int position) {
        if (animationsLocked) return;

        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            view.setTranslationY(200);
            view.setAlpha(0.f);
            view.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setStartDelay(30 * position)
                    .setInterpolator(new DecelerateInterpolator(2.f))
                    .setDuration(400)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            animationsLocked = true;
                        }
                    })
                    .start();
        }
    }

    private WeatherForecast getForecastItem(int position) {
        return this.forecast.get(--position);
    }

    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    public void update(WeatherData weatherData) {
        this.forecast = weatherData.getWeatherForecast();
        this.currentWeather = weatherData.getCurrentWeather();
        notifyDataSetChanged();
    }
}
