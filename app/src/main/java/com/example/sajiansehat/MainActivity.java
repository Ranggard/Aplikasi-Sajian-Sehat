package com.example.sajiansehat;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sajiansehat.utils.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup Toolbar
        toolbar = findViewById(R.id.toolbar);
        setupToolbarTitle();
        updateThemeIcon();

        // Handle theme menu click
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_theme) {
                showThemePopup(toolbar.findViewById(R.id.action_theme));
                return true;
            }
            return false;
        });

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);

        // Secara default, tampilkan halaman Beranda saat pertama kali dibuka
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
        }
    }

    /**
     * Setup judul toolbar dengan teks berwarna: "Sajian" (orange) + "Sehat" (green)
     */
    private void setupToolbarTitle() {
        toolbar.setTitle("");

        // Buat SpannableString "SajianSehat"
        String fullTitle = "SajianSehat";
        SpannableString spannable = new SpannableString(fullTitle);

        // "Sajian" (index 0-6) → Orange + Bold
        int orangeColor = ContextCompat.getColor(this, R.color.orange_500);
        spannable.setSpan(new ForegroundColorSpan(orangeColor), 0, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // "Sehat" (index 6-11) → Green + Bold
        int greenColor = ContextCompat.getColor(this, R.color.green_600);
        spannable.setSpan(new ForegroundColorSpan(greenColor), 6, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 6, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        toolbar.setTitle(spannable);
    }

    /**
     * Update ikon tema di toolbar sesuai mode yang tersimpan
     */
    private void updateThemeIcon() {
        int currentTheme = ThemeHelper.getSavedTheme(this);
        MenuItem themeItem = toolbar.getMenu().findItem(R.id.action_theme);
        if (themeItem != null) {
            switch (currentTheme) {
                case ThemeHelper.THEME_LIGHT:
                    themeItem.setIcon(R.drawable.ic_theme_light);
                    break;
                case ThemeHelper.THEME_DARK:
                    themeItem.setIcon(R.drawable.ic_theme_dark);
                    break;
                case ThemeHelper.THEME_SYSTEM:
                default:
                    themeItem.setIcon(R.drawable.ic_theme_system);
                    break;
            }
        }
    }

    /**
     * Tampilkan PopupMenu dengan 3 pilihan tema
     */
    private void showThemePopup(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, getString(R.string.theme_light));
        popup.getMenu().add(0, 2, 1, getString(R.string.theme_dark));
        popup.getMenu().add(0, 3, 2, getString(R.string.theme_system));

        int currentTheme = ThemeHelper.getSavedTheme(this);

        popup.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == 1) {
                ThemeHelper.setTheme(this, ThemeHelper.THEME_LIGHT);
            } else if (itemId == 2) {
                ThemeHelper.setTheme(this, ThemeHelper.THEME_DARK);
            } else if (itemId == 3) {
                ThemeHelper.setTheme(this, ThemeHelper.THEME_SYSTEM);
            }
            updateThemeIcon();
            return true;
        });

        popup.show();
    }

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

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            selectedFragment).commit();
                }
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