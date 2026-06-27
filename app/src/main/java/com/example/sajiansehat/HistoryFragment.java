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
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        LinearLayout layoutLockedOverlay = view.findViewById(R.id.layout_locked_overlay);
        MaterialButton btnLoginSekarang = view.findViewById(R.id.btnLoginSekarang);
        
        rvHistory = view.findViewById(R.id.rvHistory);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            layoutLockedOverlay.setVisibility(View.GONE);
            setupRecyclerView();
            loadHistoryData(currentUser.getUid());
        } else {
            layoutLockedOverlay.setVisibility(View.VISIBLE);
        }

        btnLoginSekarang.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), LoginActivity.class));
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentUser != null) {
                loadHistoryData(currentUser.getUid());
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        return view;
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter(historyList, item -> {
            Intent intent = new Intent(requireContext(), HasilRekomendasiActivity.class);
            intent.putExtra("DATA_REKOMENDASI", item.getJson_data());
            intent.putExtra("DURASI", item.getDurasi());
            startActivity(intent);
        });
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(historyAdapter);
    }

    private void loadHistoryData(String uid) {
        swipeRefreshLayout.setRefreshing(true);
        db.collection("users").document(uid).collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);
                    if (task.isSuccessful()) {
                        historyList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            HistoryItem item = document.toObject(HistoryItem.class);
                            item.setId(document.getId());
                            historyList.add(item);
                        }
                        historyAdapter.updateData(historyList);
                    } else {
                        Toast.makeText(requireContext(), "Gagal memuat riwayat", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
