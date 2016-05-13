package com.github.clans.rxweather.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.github.clans.rxweather.R;
import com.github.clans.rxweather.adapters.LocationsAdapter;
import com.github.clans.rxweather.models.Address;
import com.github.clans.rxweather.util.ItemClickSupport;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class AddLocationDialogFragment extends DialogFragment {

    private EditText mLocationName;
    private TextInputLayout mLocationNameLayout;
    private RecyclerView mSuggestionsList;
    private CompositeSubscription mCompositeSubscription;
    private GoogleApiClient mGoogleApiClient;
    private LocationsAdapter mLocationsAdapter;
    private OnItemClickListener mItemClickListener;
    private ProgressBar mProgressBar;

    public interface OnItemClickListener {
        void onItemClick(Address address);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mCompositeSubscription = new CompositeSubscription();

        AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AppDialog)
                .setView(R.layout.add_location_dialog_fragment)
                .create();

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        mLocationName = (EditText) getDialog().findViewById(R.id.locationName);
        mLocationNameLayout = (TextInputLayout) getDialog().findViewById(R.id.locationNameLayout);
        mSuggestionsList = (RecyclerView) getDialog().findViewById(R.id.suggestions);
        mProgressBar = (ProgressBar) getDialog().findViewById(R.id.loading);

        mSuggestionsList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mLocationsAdapter = new LocationsAdapter(new ArrayList<Address>());
        mSuggestionsList.setAdapter(mLocationsAdapter);

        ItemClickSupport.addTo(mSuggestionsList).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Address address = mLocationsAdapter.getItem(position);
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(address);
                }
                dismiss();
            }
        });

        init();
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        mGoogleApiClient = googleApiClient;
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        mItemClickListener = itemClickListener;
    }

    private void init() {
        Subscription subscription = RxTextView.textChangeEvents(mLocationName)
                .debounce(600, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .filter(new Func1<TextViewTextChangeEvent, Boolean>() {
                    @Override
                    public Boolean call(TextViewTextChangeEvent textViewTextChangeEvent) {
                        String s = mLocationName.getText().toString();
                        if (s.length() > 0 && s.length() < 3) {
                            mLocationNameLayout.setError("Enter at least 3 letters");
                        } else {
                            mLocationNameLayout.setError(null);
                            mLocationNameLayout.setErrorEnabled(false);
                        }

                        boolean pass = !TextUtils.isEmpty(s) && s.length() >= 3;
                        if (pass) {
                            mProgressBar.setVisibility(View.VISIBLE);
                        }
                        return pass;
                    }
                })
                .observeOn(Schedulers.computation())
                .flatMap(new Func1<TextViewTextChangeEvent, Observable<List<Address>>>() {
                    @Override
                    public Observable<List<Address>> call(final TextViewTextChangeEvent textViewTextChangeEvent) {
                        return Observable.create(new Observable.OnSubscribe<List<Address>>() {
                            @Override
                            public void call(final Subscriber<? super List<Address>> subscriber) {
                                String s = textViewTextChangeEvent.text().toString();
                                Timber.d("Fetching predictions for: %s", s);

                                AutocompleteFilter filter = new AutocompleteFilter.Builder()
                                        .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES).build();

                                Places.GeoDataApi.getAutocompletePredictions(mGoogleApiClient, s, null, filter)
                                        .setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
                                            @Override
                                            public void onResult(@NonNull AutocompletePredictionBuffer predictions) {
                                                List<Address> addressList = new ArrayList<>();
                                                for (AutocompletePrediction prediction : predictions) {
                                                    addressList.add(new Address(prediction.getPlaceId(),
                                                            prediction.getPrimaryText(null).toString(),
                                                            prediction.getSecondaryText(null).toString()));
                                                }
                                                subscriber.onNext(addressList);
                                                predictions.release();
                                            }
                                        });
                            }
                        });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Address>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "Error fetching predictions");
                    }

                    @Override
                    public void onNext(List<Address> addressList) {
                        mProgressBar.setVisibility(View.GONE);
                        mLocationsAdapter.updateItems(addressList);
                        for (int i = 0; i < addressList.size(); i++) {
                            Timber.d(addressList.get(i).toString());
                        }
                    }
                });

        mCompositeSubscription.add(subscription);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mCompositeSubscription.unsubscribe();
    }
}
