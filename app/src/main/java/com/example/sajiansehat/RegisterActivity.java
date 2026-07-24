package com.example.sajiansehat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword;
    private MaterialButton btnRegister, btnGoogle;
    private TextView tvToLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase authentication
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI views from layout
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvToLogin = findViewById(R.id.tvToLogin);

        // Configure Google Sign-In with OAuth client credentials
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnRegister.setOnClickListener(v -> registerUser());
        btnGoogle.setOnClickListener(v -> signUpWithGoogle());

        tvToLogin.setOnClickListener(v -> {
            // Return to login screen
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate name input
        if (TextUtils.isEmpty(name)) {
            etName.setError("Nama tidak boleh kosong");
            return;
        }

        // Validate email input
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email tidak boleh kosong");
            return;
        }

        // Validate password (minimum 8 characters)
        if (TextUtils.isEmpty(password) || password.length() < 8) {
            etPassword.setError("Password minimal 8 karakter");
            return;
        }

        // Create account on Firebase with email/password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Initialize user profile in Firestore
                            initializeUserProfile(user);
                            
                            // Direct login - go to MainActivity
                            Toast.makeText(RegisterActivity.this, "Akun berhasil dibuat! Selamat datang.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        // Registration failed - email already exists or invalid format
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Pendaftaran gagal";
                        
                        // Simplify error message for user
                        if (errorMessage.contains("email address is badly formatted")) {
                            errorMessage = "Format email tidak valid. Gunakan format: nama@gmail.com";
                        } else if (errorMessage.contains("email address is already")) {
                            errorMessage = "Email sudah terdaftar. Silakan gunakan email lain atau login.";
                        }
                        
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void signUpWithGoogle() {
        // Clear previous Google account to force account picker
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Launch Google sign-in flow
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        
        // Sign in and check if user is new or existing
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(RegisterActivity.this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if this is a new user
                            long creationTime = user.getMetadata().getCreationTimestamp();
                            long lastSignInTime = user.getMetadata().getLastSignInTimestamp();
                            boolean isNewUser = Math.abs(creationTime - lastSignInTime) < 2000;
                            
                            if (isNewUser) {
                                // New user - allowed to register
                                initializeUserProfile(user);
                                Toast.makeText(RegisterActivity.this, 
                                    "Akun berhasil dibuat dengan Google! Selamat datang.", 
                                    Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            } else {
                                // Existing user - cannot register again
                                Toast.makeText(RegisterActivity.this, 
                                    "Email sudah terdaftar. Silakan login menggunakan halaman login.", 
                                    Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                                mGoogleSignInClient.signOut();
                            }
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, 
                            "Gagal membuat akun dengan Google: " + task.getException().getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void initializeUserProfile(FirebaseUser user) {
        // Create profile document in Firestore with default data
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Pengguna";
        
        // For manual registration, get name from input field
        String namaInput = etName.getText() != null ? etName.getText().toString().trim() : "";
        String finalName = !namaInput.isEmpty() ? namaInput : displayName;
        
        java.util.Map<String, Object> profileData = new java.util.HashMap<>();
        profileData.put("email", email);
        profileData.put("nama", finalName);
        profileData.put("telepon", "");
        profileData.put("jenisKelamin", "");
        profileData.put("umur", 0);
        profileData.put("tinggi", 0);
        profileData.put("berat", 0);
        
        // Save to Firestore
        db.collection("users").document(uid)
            .set(profileData)
            .addOnFailureListener(e -> {
                // Error handling - background process
            });
        
        // Save to local SharedPreferences for immediate display
        com.example.sajiansehat.models.UserProfile localProfile = 
            new com.example.sajiansehat.models.UserProfile(finalName, "", email, "", 0, 0, 0);
        com.example.sajiansehat.utils.ProfileManager.saveProfile(this, localProfile);
    }
}
