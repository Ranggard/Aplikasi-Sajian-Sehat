package com.example.sajiansehat.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationManager {
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private Geocoder geocoder;

    public LocationManager(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }

    /**
     * Check if location permissions are granted
     */
    public boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Get last known location
     */
    public void getLastKnownLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Permission not granted");
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            getLocationName(location, callback);
                        } else {
                            callback.onLocationError("Location not available");
                        }
                    })
                    .addOnFailureListener(e -> {
                        callback.onLocationError(e.getMessage());
                    });
        } catch (SecurityException e) {
            callback.onLocationError(e.getMessage());
        }
    }

    /**
     * Get location name from coordinates
     */
    private void getLocationName(Location location, LocationCallback callback) {
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String subAdminArea = address.getSubAdminArea();
                
                String locationName = city;
                if (city != null && subAdminArea != null && !city.equals(subAdminArea)) {
                    locationName = city + ", " + subAdminArea;
                }
                
                callback.onLocationSuccess(locationName, location.getLatitude(), 
                    location.getLongitude());
            } else {
                callback.onLocationError("Address not found");
            }
        } catch (IOException e) {
            callback.onLocationError("Geocoding error: " + e.getMessage());
        }
    }

    /**
     * Callback interface for location updates
     */
    public interface LocationCallback {
        void onLocationSuccess(String locationName, double latitude, double longitude);
        void onLocationError(String error);
    }
}
