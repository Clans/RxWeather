package com.github.clans.rxweather.models;

public class Address {

    private String placeId;
    private String city;
    private String locality;

    public Address(String placeId, String city, String locality) {
        this.placeId = placeId;
        this.city = city;
        this.locality = locality;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getCity() {
        return city;
    }

    public String getLocality() {
        return locality;
    }

    @Override
    public String toString() {
        return city + ", " + locality;
    }
}
