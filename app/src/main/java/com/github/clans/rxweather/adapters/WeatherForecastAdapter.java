package com.github.clans.rxweather.adapters;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.rxweather.WeatherItemAnimator;
import com.github.clans.rxweather.util.DateFormatter;
import com.github.clans.rxweather.R;
import com.github.clans.rxweather.util.TempFormatter;
import com.github.clans.rxweather.util.WeatherIconMapper;
import com.github.clans.rxweather.models.CurrentWeather;
import com.github.clans.rxweather.models.WeatherData;
import com.github.clans.rxweather.models.WeatherForecast;
import com.github.clans.rxweather.util.WindFormatter;

import java.util.List;

public class WeatherForecastAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_CHILD = 2;

    private List<WeatherForecast> forecast;
    private CurrentWeather currentWeather;
    private boolean animationsLocked = false;
    private int mLastPosition = RecyclerView.NO_POSITION;

    public WeatherForecastAdapter(WeatherData weatherData) {
        this.forecast = weatherData.getWeatherForecast();
        this.currentWeather = weatherData.getCurrentWeather();
    }

    public static class ViewHolderItem extends RecyclerView.ViewHolder {

        private TextView day;
        private TextView conditions;
        private TextView maxTemp;
        private TextView minTemp;
        private ImageView icon;

        public ViewHolderItem(View itemView) {
            super(itemView);
            day = (TextView) itemView.findViewById(R.id.day);
            conditions = (TextView) itemView.findViewById(R.id.conditions);
            maxTemp = (TextView) itemView.findViewById(R.id.maxTemp);
            minTemp = (TextView) itemView.findViewById(R.id.minTemp);
            icon = (ImageView) itemView.findViewById(R.id.icon);
        }
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {

        private TextView locationName;
        private TextView currentConditions;
        private TextView temp;
        private ImageView icon;

        public ViewHolderHeader(View itemView) {
            super(itemView);
            locationName = (TextView) itemView.findViewById(R.id.locationName);
            currentConditions = (TextView) itemView.findViewById(R.id.currentConditions);
            temp = (TextView) itemView.findViewById(R.id.temp);
            icon = (ImageView) itemView.findViewById(R.id.icon);
        }
    }

    public static class ViewHolderChild extends RecyclerView.ViewHolder {

        private TextView wind;
        private TextView humidity;
        private TextView pressure;

        public ViewHolderChild(View itemView) {
            super(itemView);
            wind = (TextView) itemView.findViewById(R.id.wind);
            humidity = (TextView) itemView.findViewById(R.id.humidity);
            pressure = (TextView) itemView.findViewById(R.id.pressure);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case TYPE_HEADER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_header, parent, false);
                return new ViewHolderHeader(view);
            case TYPE_ITEM:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_weather, parent, false);
                return new ViewHolderItem(view);
            case TYPE_CHILD:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_weather_details, parent, false);
                return new ViewHolderChild(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderHeader) {
            ViewHolderHeader vhh = (ViewHolderHeader) holder;
            vhh.locationName.setText(currentWeather.getLocationName());
            vhh.currentConditions.setText(currentWeather.getCurrentConditions());
            vhh.temp.setText(currentWeather.getCurrentTemp());
            vhh.icon.setImageResource(WeatherIconMapper.getWeatherIconRes(currentWeather.getWeatherId(),
                    currentWeather.getIcon()));
        } else if (holder instanceof ViewHolderItem) {
            runEnterAnimation(holder, position);

            WeatherForecast forecast = getForecastItem(position);
            if (forecast != null) {
                holder.itemView.setActivated(forecast.isExpanded());

                ViewHolderItem vhi = (ViewHolderItem) holder;
                vhi.day.setText(DateFormatter.format(forecast.getTimestamp()));
                vhi.conditions.setText(forecast.getConditions());
                vhi.maxTemp.setText(TempFormatter.format(forecast.getMaxTemp()));
                vhi.minTemp.setText(TempFormatter.format(forecast.getMinTemp()));
                vhi.icon.setImageResource(WeatherIconMapper.getWeatherIconRes(forecast.getWeatherId(),
                        forecast.getIcon()));
            }
        } else {
            WeatherForecast forecast = getForecastItem(position);
            if (forecast != null) {
                ViewHolderChild vhc = (ViewHolderChild) holder;
                vhc.wind.setText(WindFormatter.format(forecast.getWindSpeed(), forecast.getWindDirection()));
                vhc.humidity.setText(String.format("%s%%", forecast.getHumidity()));
                vhc.pressure.setText(String.format("%s hPa", forecast.getPressure()));
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
        } else if (getForecastItem(position).getType() == TYPE_CHILD) {
            return TYPE_CHILD;
        }
        return TYPE_ITEM;
    }

    private void runEnterAnimation(final RecyclerView.ViewHolder holder, int position) {
        int adapterPosition = holder.getAdapterPosition();
        if (!animationsLocked && adapterPosition > mLastPosition) {
            AnimatorSet set = new AnimatorSet();
            Animator[] animators = new ObjectAnimator[] {
                    ObjectAnimator.ofFloat(holder.itemView, "translationY", 50, 0),
                    ObjectAnimator.ofFloat(holder.itemView, "alpha", 0f, 1f)
            };

            set.setDuration(400);
            set.setInterpolator(new DecelerateInterpolator(2f));
            set.setStartDelay(30 * position);
            set.playTogether(animators);
            set.start();

            mLastPosition = adapterPosition;
        } else {
            holder.itemView.setAlpha(1f);
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

    public void expandCollapseToggle(int position) {
        if (position == 0) return;

        WeatherForecast parent = getForecastItem(position);
        if (parent.getType() == TYPE_CHILD) return;

        if (parent.isExpanded()) {
            parent.setExpanded(false);
            forecast.remove(position);
            notifyItemChanged(position, WeatherItemAnimator.COLLAPSE);
            notifyItemRemoved(position + 1);
        } else {
            WeatherForecast child = parent.copy();
            child.setType(TYPE_CHILD);
            parent.setExpanded(true);
            forecast.add(position, child);
            notifyItemInserted(position + 1);
            notifyItemChanged(position, WeatherItemAnimator.EXPAND);
        }
    }
}
