package com.example.sajiansehat.models;

import java.io.Serializable;

public class RestaurantRecommendation implements Serializable {
    private String id;
    private String name;
    private String address;
    private String phone;
    private String rating;
    private int reviewCount;
    private String priceRange;
    private String googleMapsUrl;
    private String location;
    private String type;  // Tipe restoran (Warung Sunda, Restoran Nusantara, dll)
    private String specialty;  // Spesialisasi menu

    public RestaurantRecommendation() {
    }

    public RestaurantRecommendation(String id, String name, String address, 
            String phone, String rating, int reviewCount, String priceRange, 
            String googleMapsUrl, String location) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.priceRange = priceRange;
        this.googleMapsUrl = googleMapsUrl;
        this.location = location;
    }

    public RestaurantRecommendation(String id, String name, String address, 
            String phone, String rating, int reviewCount, String priceRange, 
            String googleMapsUrl, String location, String type, String specialty) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.priceRange = priceRange;
        this.googleMapsUrl = googleMapsUrl;
        this.location = location;
        this.type = type;
        this.specialty = specialty;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(String priceRange) {
        this.priceRange = priceRange;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }
}
