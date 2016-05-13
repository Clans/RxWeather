package com.github.clans.rxweather.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.clans.rxweather.R;
import com.github.clans.rxweather.models.Address;

import java.util.List;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.ViewHolder> {

    private List<Address> addressList;

    public LocationsAdapter(List<Address> addressList) {
        this.addressList = addressList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView city;
        private TextView locality;

        public ViewHolder(View itemView) {
            super(itemView);
            city = (TextView) itemView.findViewById(R.id.city);
            locality = (TextView) itemView.findViewById(R.id.locality);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Address address = addressList.get(position);
        holder.city.setText(address.getCity());
        holder.locality.setText(address.getLocality());
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public void updateItems(List<Address> addressList) {
        this.addressList = addressList;
        notifyDataSetChanged();
    }

    public Address getItem(int position) {
        return addressList.get(position);
    }
}
