package com.example.sajiansehat;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class SajianSehatApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Force light mode - disable system theme following
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
