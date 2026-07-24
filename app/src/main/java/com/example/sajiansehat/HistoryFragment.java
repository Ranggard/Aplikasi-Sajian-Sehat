package com.example.sajiansehat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.sajiansehat.adapters.HistoryAdapter;
import com.example.sajiansehat.models.HistoryItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private HistoryAdapter historyAdapter;
    private List<HistoryItem> historyList = new ArrayList<>();
    private List<HistoryItem> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private MaterialButtonToggleGroup toggleDurasiFilter;
    private String selectedDurasi = "semua"; // Default: tampilkan semua

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        LinearLayout layoutLockedOverlay = view.findViewById(R.id.layout_locked_overlay);
        MaterialButton btnLoginSekarang = view.findViewById(R.id.btnLoginSekarang);
        
        rvHistory = view.findViewById(R.id.rvHistory);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        toggleDurasiFilter = view.findViewById(R.id.toggleDurasiFilter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Show user's history if logged in, otherwise show login prompt
        if (currentUser != null) {
            layoutLockedOverlay.setVisibility(View.GONE);
            setupRecyclerView();
            setupFilterButtons();
            loadHistoryData(currentUser.getUid());
        } else {
            layoutLockedOverlay.setVisibility(View.VISIBLE);
        }

        // Navigate to login when user clicks login button
        btnLoginSekarang.setOnClickListener(v -> {
            if (getContext() != null) {
                startActivity(new Intent(getContext(), LoginActivity.class));
            }
        });

        // Load history data when user swipes down to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentUser != null) {
                loadHistoryData(currentUser.getUid());
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update UI based on current login state
        FirebaseUser currentUser = mAuth.getCurrentUser();
        LinearLayout layoutLockedOverlay = getView() != null ? getView().findViewById(R.id.layout_locked_overlay) : null;
        LinearLayout layoutLoggedIn = getView() != null ? getView().findViewById(R.id.layout_logged_in) : null;
        
        if (layoutLockedOverlay != null && layoutLoggedIn != null) {
            if (currentUser != null) {
                layoutLockedOverlay.setVisibility(View.GONE);
                layoutLoggedIn.setVisibility(View.VISIBLE);
                loadHistoryData(currentUser.getUid());
            } else {
                layoutLockedOverlay.setVisibility(View.VISIBLE);
                layoutLoggedIn.setVisibility(View.GONE);
            }
        }
    }

    private void setupRecyclerView() {
        // Initialize history adapter with item click listener to show recommendation details
        historyAdapter = new HistoryAdapter(filteredList, item -> {
            if (!isAdded() || getContext() == null) {
                return;
            }
            Intent intent = new Intent(getContext(), HasilRekomendasiActivity.class);
            intent.putExtra("DATA_REKOMENDASI", item.getJson_data());
            intent.putExtra("DURASI", item.getDurasi());
            intent.putExtra("HISTORY_ID", item.getId());  // Pass document ID untuk delete
            intent.putExtra("IS_FROM_HISTORY", true);     // Flag untuk identifikasi dari history
            startActivity(intent);
        });
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(historyAdapter);
    }

    private void setupFilterButtons() {
        // Setup filter button listener
        toggleDurasiFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnSemua) {
                    selectedDurasi = "semua";
                } else if (checkedId == R.id.btn1Hari) {
                    selectedDurasi = "1_hari";
                } else if (checkedId == R.id.btn1Minggu) {
                    selectedDurasi = "1_minggu";
                } else if (checkedId == R.id.btn1Bulan) {
                    selectedDurasi = "1_bulan";
                }
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        filteredList.clear();
        
        if (selectedDurasi.equals("semua")) {
            // Tampilkan semua history
            filteredList.addAll(historyList);
        } else {
            // Filter berdasarkan durasi yang dipilih
            for (HistoryItem item : historyList) {
                if (item.getDurasi() != null && item.getDurasi().equals(selectedDurasi)) {
                    filteredList.add(item);
                }
            }
        }
        
        historyAdapter.updateData(filteredList);
    }

    private void loadHistoryData(String uid) {
        // Fetch user's saved recommendations from Firestore, sorted by newest first
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        db.collection("users").document(uid).collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    // Check if fragment is still attached before accessing context
                    if (!isAdded() || getContext() == null) {
                        return;
                    }
                    
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    
                    if (task.isSuccessful()) {
                        historyList.clear();
                        // Convert Firestore documents to HistoryItem objects
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            HistoryItem item = document.toObject(HistoryItem.class);
                            item.setId(document.getId());
                            historyList.add(item);
                        }
                        // Apply filter after data is loaded
                        applyFilter();
                    } else {
                        Toast.makeText(getContext(), "Gagal memuat riwayat", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
