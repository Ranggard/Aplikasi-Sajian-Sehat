package com.example.sajiansehat.models;

import java.util.List;

public class RekomendasiResponse {
    public AnalisisKesehatan analisis;
    public List<ResepItem> resep;
    public List<RestaurantRecommendation> restaurants;
    public String durasi;
    public String tipe;
    public String location;
    public boolean hasRestaurantError;
    public String restaurantMessage;

    public RekomendasiResponse() {
    }

    public RekomendasiResponse(AnalisisKesehatan analisis, List<ResepItem> resep, 
            List<RestaurantRecommendation> restaurants, String durasi, String tipe,
            String location, boolean hasRestaurantError) {
        this.analisis = analisis;
        this.resep = resep;
        this.restaurants = restaurants;
        this.durasi = durasi;
        this.tipe = tipe;
        this.location = location;
        this.hasRestaurantError = hasRestaurantError;
    }

    // Getters
    public AnalisisKesehatan getAnalisis() {
        return analisis;
    }

    public List<ResepItem> getResep() {
        return resep;
    }

    public List<ResepItem> getRecipes() {
        return resep;
    }

    public List<RestaurantRecommendation> getRestaurants() {
        return restaurants;
    }

    public String getDurasi() {
        return durasi;
    }

    public String getTipe() {
        return tipe;
    }

    public String getLocation() {
        return location;
    }

    public boolean hasRestaurantError() {
        return hasRestaurantError;
    }

    public String getRestaurantMessage() {
        return restaurantMessage;
    }
}
