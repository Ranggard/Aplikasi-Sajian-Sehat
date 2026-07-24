package com.example.sajiansehat;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sajiansehat.models.UserProfile;
import com.example.sajiansehat.utils.ProfileManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etNama, etTelepon, etEmail, etUmur, etTinggi, etBerat;
    private TextInputEditText etPasswordBaru, etKonfirmasiPassword;
    private AutoCompleteTextView spinnerJenisKelamin;
    private MaterialButton btnSimpan;
    private LinearLayout layoutPasswordSection;
    private MaterialCardView cardGoogleInfo;
    private FirebaseAuth mAuth;
    private boolean isGoogleUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        initializeUI();
        loadProfile();
    }

    private void initializeUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Profil");

        etNama = findViewById(R.id.etNama);
        etTelepon = findViewById(R.id.etTelepon);
        etEmail = findViewById(R.id.etEmail);
        spinnerJenisKelamin = findViewById(R.id.spinnerJenisKelamin);
        etUmur = findViewById(R.id.etUmur);
        etTinggi = findViewById(R.id.etTinggi);
        etBerat = findViewById(R.id.etBerat);
        etPasswordBaru = findViewById(R.id.etPasswordBaru);
        etKonfirmasiPassword = findViewById(R.id.etKonfirmasiPassword);
        layoutPasswordSection = findViewById(R.id.layoutPasswordSection);
        cardGoogleInfo = findViewById(R.id.cardGoogleInfo);
        btnSimpan = findViewById(R.id.btnSimpan);

        // Check whether user logged in with Google or Email/Password
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Check provider - Google or Email/Password
            isGoogleUser = false;
            for (var profile : currentUser.getProviderData()) {
                if (profile.getProviderId().equals("google.com")) {
                    isGoogleUser = true;
                    break;
                }
            }
            
            // Display UI based on user type
            if (isGoogleUser) {
                layoutPasswordSection.setVisibility(View.GONE);
                cardGoogleInfo.setVisibility(View.VISIBLE);
            } else {
                layoutPasswordSection.setVisibility(View.VISIBLE);
                cardGoogleInfo.setVisibility(View.GONE);
            }
        }

        // Setup dropdown jenis kelamin
        String[] jenisKelaminOptions = {"Laki-laki", "Perempuan"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                jenisKelaminOptions
        );
        spinnerJenisKelamin.setAdapter(adapter);

        btnSimpan.setOnClickListener(v -> simpanProfil());
    }

    private void loadProfile() {
        UserProfile profile = ProfileManager.getProfile(this);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Load Nama - priority from profile, else use Firebase DisplayName
        if (profile.getNama() != null && !profile.getNama().isEmpty()) {
            etNama.setText(profile.getNama());
        } else if (currentUser != null && currentUser.getDisplayName() != null) {
            etNama.setText(currentUser.getDisplayName());
        }
        
        // Load Email - from Firebase or local profile
        if (currentUser != null && currentUser.getEmail() != null) {
            etEmail.setText(currentUser.getEmail());
        } else if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
            etEmail.setText(profile.getEmail());
        }
        etEmail.setEnabled(true); // Email is editable
        
        // Load Telepon - from local profile (optional)
        if (profile.getTelepon() != null && !profile.getTelepon().isEmpty()) {
            etTelepon.setText(profile.getTelepon());
        }
        
        // Load Jenis Kelamin
        if (profile.getJenisKelamin() != null && !profile.getJenisKelamin().isEmpty()) {
            spinnerJenisKelamin.setText(profile.getJenisKelamin(), false);
        }
        
        // Load Umur
        if (profile.getUmur() > 0) {
            etUmur.setText(String.valueOf(profile.getUmur()));
        }
        
        // Load Tinggi
        if (profile.getTinggi() > 0) {
            etTinggi.setText(String.valueOf(profile.getTinggi()));
        }
        
        // Load Berat
        if (profile.getBerat() > 0) {
            etBerat.setText(String.valueOf(profile.getBerat()));
        }
    }

    private void simpanProfil() {
        String nama = etNama.getText() != null ? etNama.getText().toString().trim() : "";
        String telepon = etTelepon.getText() != null ? etTelepon.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String jenisKelamin = spinnerJenisKelamin.getText().toString().trim();
        String umurStr = etUmur.getText() != null ? etUmur.getText().toString().trim() : "";
        String tinggiStr = etTinggi.getText() != null ? etTinggi.getText().toString().trim() : "";
        String beratStr = etBerat.getText() != null ? etBerat.getText().toString().trim() : "";
        String passwordBaru = etPasswordBaru != null && etPasswordBaru.getText() != null ? 
                             etPasswordBaru.getText().toString().trim() : "";
        String konfirmasiPassword = etKonfirmasiPassword != null && etKonfirmasiPassword.getText() != null ? 
                                   etKonfirmasiPassword.getText().toString().trim() : "";

        // Validate required data
        if (nama.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        // Validate email must be @gmail.com
        if (!email.toLowerCase().endsWith("@gmail.com")) {
            Toast.makeText(this, "Email harus menggunakan @gmail.com", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (jenisKelamin.isEmpty()) {
            Toast.makeText(this, "Jenis kelamin harus dipilih", Toast.LENGTH_SHORT).show();
            return;
        }
        if (umurStr.isEmpty()) {
            Toast.makeText(this, "Umur tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tinggiStr.isEmpty()) {
            Toast.makeText(this, "Tinggi tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        if (beratStr.isEmpty()) {
            Toast.makeText(this, "Berat tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password if provided (for Email/Password users only)
        if (!isGoogleUser && !passwordBaru.isEmpty()) {
            if (passwordBaru.length() < 8) {
                Toast.makeText(this, "Password minimal 8 karakter", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!passwordBaru.equals(konfirmasiPassword)) {
                Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            int umur = Integer.parseInt(umurStr);
            double tinggi = Double.parseDouble(tinggiStr);
            double berat = Double.parseDouble(beratStr);

            if (umur <= 0 || tinggi <= 0 || berat <= 0) {
                Toast.makeText(this, "Umur, tinggi, dan berat harus lebih dari 0", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "User tidak terdeteksi. Silakan login ulang", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable button and show loading text
            btnSimpan.setEnabled(false);
            btnSimpan.setText("Menyimpan...");

            // Update password if provided (for Email/Password users only)
            if (!isGoogleUser && !passwordBaru.isEmpty()) {
                updatePassword(currentUser, passwordBaru, 
                              () -> checkAndUpdateEmail(currentUser, email, nama, telepon, jenisKelamin, umur, tinggi, berat));
            } else {
                // No password update needed, proceed to email check
                checkAndUpdateEmail(currentUser, email, nama, telepon, jenisKelamin, umur, tinggi, berat);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Format angka tidak valid", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePassword(FirebaseUser user, String newPassword, Runnable onSuccess) {
        user.updatePassword(newPassword)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Password berhasil diubah", Toast.LENGTH_SHORT).show();
                    onSuccess.run();
                } else {
                    // Reset button
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText("Simpan Profil");
                    
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    if (errorMsg.contains("REQUIRES_RECENT_LOGIN")) {
                        Toast.makeText(this, "For security, please login again before changing password", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Gagal mengubah password: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
            });
    }

    private void checkAndUpdateEmail(FirebaseUser currentUser, String email, String nama, String telepon, 
                                    String jenisKelamin, int umur, double tinggi, double berat) {
        String currentEmail = currentUser.getEmail();
        
        // Update Firebase Authentication if email has changed
        if (!currentEmail.equalsIgnoreCase(email)) {
            // Update email in Firebase Authentication
            currentUser.verifyBeforeUpdateEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Email successfully updated in Firebase Auth
                        // Now save to Firestore
                        saveProfileToFirestore(nama, telepon, email, jenisKelamin, umur, tinggi, berat);
                    } else {
                        // Gagal update email
                        btnSimpan.setEnabled(true);
                        btnSimpan.setText("Simpan Profil");
                        
                        Exception exception = task.getException();
                        if (exception != null) {
                            String errorMessage = exception.getMessage();
                            if (errorMessage != null && errorMessage.contains("EMAIL_EXISTS")) {
                                Toast.makeText(EditProfileActivity.this, "Email sudah terdaftar", Toast.LENGTH_SHORT).show();
                            } else if (errorMessage != null && errorMessage.contains("INVALID_EMAIL")) {
                                Toast.makeText(EditProfileActivity.this, "Email tidak valid", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(EditProfileActivity.this, "Gagal mengubah email: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
        } else {
            // Email not changed - save directly to Firestore
            saveProfileToFirestore(nama, telepon, email, jenisKelamin, umur, tinggi, berat);
        }
    }

    private void saveProfileToFirestore(String nama, String telepon, String email, 
                                        String jenisKelamin, int umur, double tinggi, double berat) {
        UserProfile profile = new UserProfile(nama, telepon, email, jenisKelamin, umur, tinggi, berat);
        
        // Save to local SharedPreferences
        ProfileManager.saveProfile(this, profile);
        
        // Save to Firestore for data synchronization
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            
            java.util.Map<String, Object> profileData = new java.util.HashMap<>();
            profileData.put("email", email);
            profileData.put("nama", nama);
            profileData.put("telepon", telepon);
            profileData.put("jenisKelamin", jenisKelamin);
            profileData.put("umur", umur);
            profileData.put("tinggi", tinggi);
            profileData.put("berat", berat);
            
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(profileData)
                .addOnSuccessListener(aVoid -> {
                    // Reset button
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText("Simpan Profil");
                    
                    Toast.makeText(this, "Profil berhasil disimpan", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Reset button
                    btnSimpan.setEnabled(true);
                    btnSimpan.setText("Simpan Profil");
                    
                    // Profil lokal sudah tersimpan, tapi Firestore gagal
                    Toast.makeText(this, "Profil tersimpan lokal, tapi gagal sync ke server", Toast.LENGTH_SHORT).show();
                    finish();
                });
        } else {
            // Reset button
            btnSimpan.setEnabled(true);
            btnSimpan.setText("Simpan Profil");
            
            Toast.makeText(this, "Profil berhasil disimpan", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
