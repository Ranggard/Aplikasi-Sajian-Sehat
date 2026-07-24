package com.example.sajiansehat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sajiansehat.models.UserProfile;
import com.example.sajiansehat.utils.ProfileManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private FrameLayout layoutMain;
    private ScrollView layoutLoggedIn;
    private LinearLayout layoutLockedOverlay;
    private TextView tvProfilNama, tvProfilEmail, tvJenisKelamin, tvUmur, tvTinggi, tvBerat, tvTelepon;
    private MaterialButton btnLoginSekarang, btnEditProfil;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        // Bind UI views from layout
        layoutMain = view.findViewById(R.id.layoutMain);
        layoutLockedOverlay = view.findViewById(R.id.layout_locked_overlay);
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        tvProfilNama = view.findViewById(R.id.tvProfilNama);
        tvProfilEmail = view.findViewById(R.id.tvProfilEmail);
        tvJenisKelamin = view.findViewById(R.id.tvJenisKelamin);
        tvUmur = view.findViewById(R.id.tvUmur);
        tvTinggi = view.findViewById(R.id.tvTinggi);
        tvBerat = view.findViewById(R.id.tvBerat);
        tvTelepon = view.findViewById(R.id.tvTelepon);
        btnLoginSekarang = view.findViewById(R.id.btnLoginSekarang);
        btnEditProfil = view.findViewById(R.id.btnEditProfil);

        updateUI();

        // Login button
        if (btnLoginSekarang != null) {
            btnLoginSekarang.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                startActivity(intent);
            });
        }

        // Edit profile button
        if (btnEditProfil != null) {
            btnEditProfil.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        // Check if all views are initialized
        if (tvProfilNama == null || tvProfilEmail == null || layoutLockedOverlay == null || layoutLoggedIn == null) {
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        layoutLoggedIn.setVisibility(View.VISIBLE);
        
        if (currentUser != null) {
            layoutLockedOverlay.setVisibility(View.GONE);
            if (btnEditProfil != null) {
                btnEditProfil.setVisibility(View.VISIBLE);
            }

            UserProfile profile = ProfileManager.getProfile(requireContext());
            
            // Display name from local profile storage
            String name = profile.getNama() != null && !profile.getNama().isEmpty() 
                ? profile.getNama() 
                : (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Pengguna");
            
            // Display email from Firebase
            String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";

            tvProfilNama.setText(name);
            tvProfilEmail.setText(email);
            
            if (tvTelepon != null) {
                tvTelepon.setText(profile.getTelepon() != null && !profile.getTelepon().isEmpty() ? 
                                 profile.getTelepon() : "-");
            }
            if (tvJenisKelamin != null) {
                tvJenisKelamin.setText(profile.getJenisKelamin() != null && !profile.getJenisKelamin().isEmpty() ? 
                                      profile.getJenisKelamin() : "-");
            }
            if (tvUmur != null) {
                tvUmur.setText(profile.getUmur() > 0 ? profile.getUmur() + " tahun" : "-");
            }
            if (tvTinggi != null) {
                tvTinggi.setText(profile.getTinggi() > 0 ? profile.getTinggi() + " cm" : "-");
            }
            if (tvBerat != null) {
                tvBerat.setText(profile.getBerat() > 0 ? profile.getBerat() + " kg" : "-");
            }

        } else {
            layoutLockedOverlay.setVisibility(View.VISIBLE);
            if (btnEditProfil != null) {
                btnEditProfil.setVisibility(View.GONE);
            }
            
            tvProfilNama.setText("Pengguna Tamu");
            tvProfilEmail.setText("Belum login");
        }
    }
}
