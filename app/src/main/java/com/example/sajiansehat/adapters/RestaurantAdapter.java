package com.example.sajiansehat.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sajiansehat.R;
import com.example.sajiansehat.models.RestaurantRecommendation;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {
    
    private List<RestaurantRecommendation> restaurants;
    private OnRestaurantClickListener onClickListener;

    public interface OnRestaurantClickListener {
        void onRestaurantClick(RestaurantRecommendation restaurant);
    }

    public RestaurantAdapter(List<RestaurantRecommendation> restaurants, 
            OnRestaurantClickListener onClickListener) {
        this.restaurants = restaurants;
        this.onClickListener = onClickListener;
    }

    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) {
            // Empty state view holder
            View view = new android.widget.TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return new RestaurantViewHolder(view, true);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant, parent, false);
            return new RestaurantViewHolder(view, false);
        }
    }

    @Override
    public int getItemViewType(int position) {
        // Return view type 1 if empty, 0 if normal
        return restaurants == null || restaurants.isEmpty() ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        if (restaurants == null || restaurants.isEmpty()) {
            // Show empty state message
            if (holder.isEmptyState) {
                android.widget.TextView emptyView = (android.widget.TextView) holder.itemView;
                emptyView.setText("⚠️ Rekomendasi di lokasi tersebut tidak ada");
                emptyView.setPadding(16, 24, 16, 24);
                emptyView.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
                emptyView.setTextSize(14);
                emptyView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.gray_600, null));
            }
        } else {
            RestaurantRecommendation restaurant = restaurants.get(position);
            holder.bind(restaurant, onClickListener);
        }
    }

    @Override
    public int getItemCount() {
        // Always show at least 1 item (empty state or normal)
        return (restaurants != null && !restaurants.isEmpty()) ? restaurants.size() : 1;
    }

    public static class RestaurantViewHolder extends RecyclerView.ViewHolder {
        private TextView tvRestaurantName;
        private TextView tvRestaurantType;  // Tipe restoran (Warung Sunda, dll)
        private TextView tvRestaurantSpecialty;  // Spesialisasi menu
        private TextView tvRestaurantLocation;
        private TextView tvRestaurantAddress;
        private TextView tvRestaurantRating;
        private TextView tvRestaurantPrice;
        private MaterialButton btnOpen;
        public boolean isEmptyState = false;

        public RestaurantViewHolder(@NonNull View itemView) {
            this(itemView, false);
        }

        public RestaurantViewHolder(@NonNull View itemView, boolean isEmptyState) {
            super(itemView);
            this.isEmptyState = isEmptyState;
            
            if (!isEmptyState) {
                tvRestaurantName = itemView.findViewById(R.id.tv_restaurant_name);
                tvRestaurantType = itemView.findViewById(R.id.tv_restaurant_type);
                tvRestaurantSpecialty = itemView.findViewById(R.id.tv_restaurant_specialty);
                tvRestaurantLocation = itemView.findViewById(R.id.tv_restaurant_location);
                tvRestaurantAddress = itemView.findViewById(R.id.tv_restaurant_address);
                tvRestaurantRating = itemView.findViewById(R.id.tv_restaurant_rating);
                tvRestaurantPrice = itemView.findViewById(R.id.tv_restaurant_price);
                btnOpen = itemView.findViewById(R.id.btn_open_map);
            }
        }

        public void bind(RestaurantRecommendation restaurant, 
                OnRestaurantClickListener onClickListener) {
            tvRestaurantName.setText(restaurant.getName());
            
            // Display restaurant type if available (Warung Sunda, Restoran Nusantara, dll)
            if (restaurant.getType() != null && !restaurant.getType().isEmpty()) {
                tvRestaurantType.setVisibility(View.VISIBLE);
                tvRestaurantType.setText("🏪 " + restaurant.getType());
            } else {
                tvRestaurantType.setVisibility(View.GONE);
            }
            
            // Display restaurant specialty if available (Ayam Goreng, Ikan Bakar, dll)
            if (restaurant.getSpecialty() != null && !restaurant.getSpecialty().isEmpty()) {
                tvRestaurantSpecialty.setVisibility(View.VISIBLE);
                tvRestaurantSpecialty.setText("🍽️ Menu: " + restaurant.getSpecialty());
            } else {
                tvRestaurantSpecialty.setVisibility(View.GONE);
            }
            
            // Display location if available
            if (restaurant.getLocation() != null && !restaurant.getLocation().isEmpty()) {
                tvRestaurantLocation.setVisibility(View.VISIBLE);
                tvRestaurantLocation.setText("📍 " + restaurant.getLocation());
            } else {
                tvRestaurantLocation.setVisibility(View.GONE);
            }
            
            tvRestaurantAddress.setText("📍 " + restaurant.getAddress());
            tvRestaurantRating.setText("⭐ " + restaurant.getRating() + 
                " (" + restaurant.getReviewCount() + " review)");
            tvRestaurantPrice.setText("💰 " + restaurant.getPriceRange());
            
            btnOpen.setOnClickListener(v -> {
                if (onClickListener != null) {
                    onClickListener.onRestaurantClick(restaurant);
                }
            });
        }
    }
}
