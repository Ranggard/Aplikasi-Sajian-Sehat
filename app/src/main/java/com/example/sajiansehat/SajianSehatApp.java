package com.example.sajiansehat;

import android.app.Application;
import com.example.sajiansehat.utils.ThemeHelper;

public class SajianSehatApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applyTheme(this);
    }
}
