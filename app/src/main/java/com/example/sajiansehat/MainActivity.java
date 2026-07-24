package com.example.sajiansehat;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sajiansehat.utils.ProfileManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Initialize toolbar with colored title
        toolbar = findViewById(R.id.toolbar);
        setupToolbarTitle();
        updateAuthIcon();

    // Set logout menu click listener
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                handleLogout();
                return true;
            } else if (item.getItemId() == R.id.action_login) {
                handleLogin();
                return true;
            }
            return false;
        });

        // Initialize bottom navigation with fragment switching
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);

        // Load home fragment on first app launch
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAuthIcon();
    }

    // Create toolbar title with dual-color text: orange "Sajian" + green "Sehat" in bold
    private void setupToolbarTitle() {
        toolbar.setTitle("");

        String fullTitle = "SajianSehat";
        SpannableString spannable = new SpannableString(fullTitle);

        // Apply orange color and bold styling to "Sajian" (first 6 characters)
        int orangeColor = ContextCompat.getColor(this, R.color.orange_500);
        spannable.setSpan(new ForegroundColorSpan(orangeColor), 0, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Apply green color and bold styling to "Sehat" (last 5 characters)
        int greenColor = ContextCompat.getColor(this, R.color.green_600);
        spannable.setSpan(new ForegroundColorSpan(greenColor), 6, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 6, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        toolbar.setTitle(spannable);
    }

    // Update login/logout icon visibility based on login state
    private void updateAuthIcon() {
        MenuItem logoutItem = toolbar.getMenu().findItem(R.id.action_logout);
        MenuItem loginItem = toolbar.getMenu().findItem(R.id.action_login);
        
        if (logoutItem != null && loginItem != null) {
            boolean isLoggedIn = mAuth.getCurrentUser() != null;
            
            if (isLoggedIn) {
                logoutItem.setVisible(true);
                loginItem.setVisible(false);
                // Set logout icon tint to red when visible
                logoutItem.getIcon().setTint(ContextCompat.getColor(this, R.color.red_500));
            } else {
                logoutItem.setVisible(false);
                loginItem.setVisible(true);
                // Set login icon tint to green when visible
                loginItem.getIcon().setTint(ContextCompat.getColor(this, R.color.green_600));
            }
        }
    }

    // Handle logout action with confirmation dialog
    private void handleLogout() {
        if (mAuth.getCurrentUser() != null) {
            // Show confirmation dialog before logout
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Logout?")
                    .setMessage("Apakah Anda yakin ingin logout dari aplikasi?")
                    .setPositiveButton("Ya, Logout", (dialog, which) -> {
                        // Perform logout
                        mAuth.signOut();
                        ProfileManager.clearProfile(this);
                        Toast.makeText(MainActivity.this, "Berhasil logout", Toast.LENGTH_SHORT).show();
                        updateAuthIcon();
                        
                        // Navigate to HomeFragment and select home tab
                        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
                        bottomNav.setSelectedItemId(R.id.nav_home);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new HomeFragment())
                                .commit();
                    })
                    .setNegativeButton("Batal", (dialog, which) -> {
                        // User cancelled logout
                        dialog.dismiss();
                    })
                    .show();
        }
    }

    // Handle login action
    private void handleLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    // Switch between navigation fragments when bottom menu item is selected
    private BottomNavigationView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_rekomendasi) {
                    selectedFragment = new RekomendasiFragment();
                } else if (itemId == R.id.nav_history) {
                    selectedFragment = new HistoryFragment();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                // Replace fragment in container if valid fragment selected
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
                }
                
                // Update auth icon visibility
                updateAuthIcon();
                
                return true;
            };

    public void switchToRekomendasiTab() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_rekomendasi);
    }

    public void setBottomNavigationEnabled(boolean enabled) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null && bottomNav.getMenu() != null) {
            for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                bottomNav.getMenu().getItem(i).setEnabled(enabled);
            }
        }
    }
}