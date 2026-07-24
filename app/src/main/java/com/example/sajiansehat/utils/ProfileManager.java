package com.example.sajiansehat.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.sajiansehat.models.UserProfile;

public class ProfileManager {
    private static final String PREFS_NAME = "SajianSehat_Profile";
    private static final String KEY_NAMA = "nama";
    private static final String KEY_TELEPON = "telepon";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_JENIS_KELAMIN = "jenis_kelamin";
    private static final String KEY_UMUR = "umur";
    private static final String KEY_TINGGI = "tinggi";
    private static final String KEY_BERAT = "berat";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Save user profile to SharedPreferences
    public static void saveProfile(Context context, UserProfile profile) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_NAMA, profile.getNama());
        editor.putString(KEY_TELEPON, profile.getTelepon());
        editor.putString(KEY_EMAIL, profile.getEmail());
        editor.putString(KEY_JENIS_KELAMIN, profile.getJenisKelamin());
        editor.putInt(KEY_UMUR, profile.getUmur());
        editor.putFloat(KEY_TINGGI, (float) profile.getTinggi());
        editor.putFloat(KEY_BERAT, (float) profile.getBerat());
        editor.apply();
    }

    // Retrieve user profile from SharedPreferences
    public static UserProfile getProfile(Context context) {
        SharedPreferences prefs = getPrefs(context);
        UserProfile profile = new UserProfile();
        
        profile.setNama(prefs.getString(KEY_NAMA, ""));
        profile.setTelepon(prefs.getString(KEY_TELEPON, ""));
        profile.setEmail(prefs.getString(KEY_EMAIL, ""));
        profile.setJenisKelamin(prefs.getString(KEY_JENIS_KELAMIN, ""));
        profile.setUmur(prefs.getInt(KEY_UMUR, 0));
        profile.setTinggi(prefs.getFloat(KEY_TINGGI, 0));
        profile.setBerat(prefs.getFloat(KEY_BERAT, 0));
        
        return profile;
    }

    // Check if required profile data is complete
    public static boolean isProfileComplete(Context context) {
        UserProfile profile = getProfile(context);
        return profile.isDataLengkap();
    }

    // Clear user profile from SharedPreferences
    public static void clearProfile(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.clear();
        editor.apply();
    }

    // Update a specific profile field
    public static void updateField(Context context, String key, Object value) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Double) {
            editor.putFloat(key, ((Double) value).floatValue());
        }
        
        editor.apply();
    }
}
