package com.example.sajiansehat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private LinearLayout layoutLockedOverlay, layoutLoggedIn;
    private TextView tvProfilNama, tvProfilEmail;
    private MaterialButton btnLoginSekarang, btnLogout;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        // Bind Views
        layoutLockedOverlay = view.findViewById(R.id.layout_locked_overlay);
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        tvProfilNama = view.findViewById(R.id.tvProfilNama);
        tvProfilEmail = view.findViewById(R.id.tvProfilEmail);
        btnLoginSekarang = view.findViewById(R.id.btnLoginSekarang);
        btnLogout = view.findViewById(R.id.btnLogout);

        updateUI();

        // Tombol Login
        btnLoginSekarang.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        });

        // Tombol Logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(requireContext(), "Berhasil logout", Toast.LENGTH_SHORT).show();
            updateUI(); // Segarkan tampilan ke mode belum login
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI(); // Pastikan UI diupdate saat fragment kembali tampil (misal habis login)
    }

    private void updateUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Layout profil selalu dibiarkan terlihat di belakang
        layoutLoggedIn.setVisibility(View.VISIBLE);
        
        if (currentUser != null) {
            // Sudah Login
            layoutLockedOverlay.setVisibility(View.GONE);

            String email = currentUser.getEmail();
            String name = currentUser.getDisplayName();

            if (name == null || name.isEmpty()) {
                name = "Pengguna Sajian Sehat";
            }

            tvProfilNama.setText(name);
            tvProfilEmail.setText(email);
        } else {
            // Belum Login
            layoutLockedOverlay.setVisibility(View.VISIBLE);
            
            tvProfilNama.setText("Pengguna Tamu");
            tvProfilEmail.setText("Belum login");
        }
    }
}
