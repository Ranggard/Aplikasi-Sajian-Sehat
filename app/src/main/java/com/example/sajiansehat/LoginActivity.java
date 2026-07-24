package com.example.sajiansehat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogle;
    private TextView tvToRegister, tvForgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase authentication
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI views from layout
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvToRegister = findViewById(R.id.tvToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // If user already logged in, redirect to main activity
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        btnLogin.setOnClickListener(v -> loginUser());

        // Configure Google Sign-In with OAuth client credentials
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        tvToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
        
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Extract Google account from sign-in result
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        // Authenticate with Firebase using Google credentials
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void signInWithGoogle() {
        // Clear previous Google account to force account picker
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Launch Google sign-in flow
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        // Exchange Google token for Firebase authentication
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        
        // Sign in and check if user is new or existing
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(LoginActivity.this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if this is a new user or existing
                            // New user if metadata creationTime == lastSignInTime (within 2 seconds)
                            long creationTime = user.getMetadata().getCreationTimestamp();
                            long lastSignInTime = user.getMetadata().getLastSignInTimestamp();
                            boolean isNewUser = Math.abs(creationTime - lastSignInTime) < 2000;
                            
                            if (isNewUser) {
                                // New user account - must register first
                                Toast.makeText(LoginActivity.this, 
                                    "Email belum terdaftar. Silakan daftar terlebih dahulu.", 
                                    Toast.LENGTH_LONG).show();
                                // Delete newly created user
                                user.delete();
                                mAuth.signOut();
                                mGoogleSignInClient.signOut();
                            } else {
                                // Existing registered user - allowed to login
                                initializeUserProfile(user);
                                Toast.makeText(LoginActivity.this, "Berhasil login dengan Google!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            }
                        }
                    } else {
                        // Better error logging for debugging
                        String errorMsg = task.getException() != null ? 
                                task.getException().getMessage() : "Unknown error";
                        
                        // Log full exception for debugging
                        if (task.getException() != null) {
                            android.util.Log.e("GoogleSignIn", "Firebase auth failed", task.getException());
                        }
                        
                        // User-friendly error message
                        String userMessage = "Gagal login dengan Google";
                        if (errorMsg.contains("DEVELOPER_ERROR")) {
                            userMessage = "Kesalahan konfigurasi Google Sign-In. Pastikan SHA-1 fingerprint sudah terdaftar di Firebase Console.";
                        } else if (errorMsg.contains("NETWORK_ERROR")) {
                            userMessage = "Koneksi internet tidak stabil. Silakan coba lagi.";
                        } else if (errorMsg.contains("internal error")) {
                            userMessage = "Kesalahan internal Google. Pastikan Google Play Services sudah ter-update.";
                        }
                        
                        Toast.makeText(LoginActivity.this, userMessage + "\n(" + errorMsg + ")", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate email input
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email tidak boleh kosong");
            etEmail.requestFocus();
            return;
        }

        // Validate password input
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password tidak boleh kosong");
            etPassword.requestFocus();
            return;
        }

        // Authenticate with Firebase email/password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Login successful - redirect to main activity
                            initializeUserProfile(user);
                            Toast.makeText(LoginActivity.this, "Selamat datang kembali!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        // Login failed - invalid email or password
                        Toast.makeText(LoginActivity.this, "Login gagal: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void initializeUserProfile(FirebaseUser user) {
        // Create profile document with default data if not exists
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Pengguna";
        
        // Check if profile document already exists
        db.collection("users").document(uid)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (!task.getResult().exists()) {
                                // Document does not exist, create with default data
                                java.util.Map<String, Object> profileData = new java.util.HashMap<>();
                                profileData.put("email", email);
                                profileData.put("nama", displayName);
                                profileData.put("telepon", "");
                                profileData.put("jenisKelamin", "");
                                profileData.put("umur", 0);
                                profileData.put("tinggi", 0);
                                profileData.put("berat", 0);
                                
                                db.collection("users").document(uid)
                                    .set(profileData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Save to local SharedPreferences
                                        com.example.sajiansehat.models.UserProfile localProfile = 
                                            new com.example.sajiansehat.models.UserProfile(displayName, "", email, "", 0, 0, 0);
                                        com.example.sajiansehat.utils.ProfileManager.saveProfile(LoginActivity.this, localProfile);
                                    })
                                    .addOnFailureListener(e -> {
                                        // Background error - do not block login
                                    });
                    } else {
                        // Document exists - sync to local SharedPreferences
                        String nama = task.getResult().getString("nama");
                        String telepon = task.getResult().getString("telepon");
                        String jenisKelamin = task.getResult().getString("jenisKelamin");
                        
                        Long umurLong = task.getResult().getLong("umur");
                        int umur = umurLong != null ? umurLong.intValue() : 0;
                        
                        Double tinggiDouble = task.getResult().getDouble("tinggi");
                        double tinggi = tinggiDouble != null ? tinggiDouble : 0.0;
                        
                        Double beratDouble = task.getResult().getDouble("berat");
                        double berat = beratDouble != null ? beratDouble : 0.0;
                        
                        com.example.sajiansehat.models.UserProfile localProfile = 
                            new com.example.sajiansehat.models.UserProfile(
                                nama != null ? nama : displayName,
                                telepon != null ? telepon : "",
                                email,
                                jenisKelamin != null ? jenisKelamin : "",
                                umur,
                                tinggi,
                                berat
                            );
                        com.example.sajiansehat.utils.ProfileManager.saveProfile(LoginActivity.this, localProfile);
                    }
                }
            });
    }

    private void showForgotPasswordDialog() {
        // Create dialog with input field for email
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Forgot Password");
        builder.setMessage("Enter your email to receive a password reset link");

        // Create input field
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        // Set padding for input
        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        
        builder.setView(input);

        // Set dialog buttons
        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = input.getText() != null ? input.getText().toString().trim() : "";
            
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Send password reset email
            sendPasswordResetEmail(email);
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        // Show loading
        Toast.makeText(this, "Mengirim email reset password...", Toast.LENGTH_SHORT).show();
        
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Email sent successfully
                        new AlertDialog.Builder(this)
                                .setTitle("Email Sent")
                                .setMessage("A password reset link has been sent to " + email + ". Please check your inbox or spam folder.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        // Failed to send email
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Gagal mengirim email";
                        
                        // Simplify error messages
                        if (errorMessage.contains("no user record")) {
                            errorMessage = "Email tidak terdaftar. Silakan periksa kembali atau daftar akun baru.";
                        } else if (errorMessage.contains("badly formatted")) {
                            errorMessage = "Format email tidak valid.";
                        }
                        
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
