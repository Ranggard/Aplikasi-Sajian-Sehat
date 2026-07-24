package com.example.sajiansehat.models;

import java.util.List;

public class ResepItem {
    private String id;
    private String title;
    private String image;
    private String sumber;
    private List<String> nutrisi;
    private List<String> bahan;
    private List<String> langkah;
    private List<RestaurantRecommendation> restaurants;
    private boolean hasRestaurantError;
    private String restaurantMessage;

    // Getter methods for recipe data from API response
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getImage() { return image; }
    public String getSumber() { return sumber; }
    public List<String> getNutrisi() { return nutrisi; }
    public List<String> getBahan() { return bahan; }
    public List<String> getLangkah() { return langkah; }
    public List<RestaurantRecommendation> getRestaurants() { return restaurants; }
    public boolean hasRestaurantError() { return hasRestaurantError; }
    public String getRestaurantMessage() { return restaurantMessage; }
}
