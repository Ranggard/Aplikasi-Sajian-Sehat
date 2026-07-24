package com.example.sajiansehat.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sajiansehat.R;
import com.example.sajiansehat.models.ResepItem;
import com.example.sajiansehat.models.RestaurantRecommendation;
import com.example.sajiansehat.models.RestaurantRecommendation;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ResepAdapter extends RecyclerView.Adapter<ResepAdapter.ResepViewHolder> {

    private List<ResepItem> listResep;
    private String locationName;
    private List<RestaurantRecommendation> restaurants;
    private boolean[] expandedStates;

    public ResepAdapter(List<ResepItem> listResep) {
        this(listResep, "", null);
    }

    public ResepAdapter(List<ResepItem> listResep, String locationName) {
        this(listResep, locationName, null);
    }

    public ResepAdapter(List<ResepItem> listResep, String locationName, List<RestaurantRecommendation> restaurants) {
        this.listResep = listResep;
        this.locationName = locationName != null ? locationName : "";
        this.restaurants = restaurants != null ? restaurants : new java.util.ArrayList<>();
        // Initialize expanded state array (tracks which cards are expanded/collapsed)
        this.expandedStates = new boolean[listResep != null ? listResep.size() : 0];
    }

    // Update recipe list when filtered by day (clears previous expansion states)
    public void updateData(List<ResepItem> newList) {
        this.listResep = newList;
        this.expandedStates = new boolean[newList != null ? newList.size() : 0];
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_resep, parent, false);
        return new ResepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResepViewHolder holder, int position) {
        ResepItem resep = listResep.get(position);

        holder.tvJudulResep.setText(resep.getTitle());
        holder.tvSumber.setText(resep.getSumber());

        // Determine meal type (Breakfast, Lunch, Dinner) based on position modulo 3
        // Since 3 items are always shown per day or less
        int mealIndex = position % 3;
        if (mealIndex == 0) {
            holder.tvWaktuMakan.setText("SARAPAN");
            holder.tvWaktuMakan.setTextColor(holder.itemView.getContext().getColor(R.color.orange_600));
        } else if (mealIndex == 1) {
            holder.tvWaktuMakan.setText("MAKAN SIANG");
            holder.tvWaktuMakan.setTextColor(holder.itemView.getContext().getColor(R.color.green_700));
        } else {
            holder.tvWaktuMakan.setText("MAKAN MALAM");
            holder.tvWaktuMakan.setTextColor(holder.itemView.getContext().getColor(R.color.gray_600));
        }

        // Hide location from recipe card (moved to restaurant recommendations)
        holder.tvLocation.setVisibility(View.GONE);

        // Combine nutrition information - limit display to 3 items
        if (resep.getNutrisi() != null && !resep.getNutrisi().isEmpty()) {
            StringBuilder nutrisiStr = new StringBuilder();
            for (int i = 0; i < Math.min(resep.getNutrisi().size(), 3); i++) {
                nutrisiStr.append(resep.getNutrisi().get(i));
                if (i < 2 && i < resep.getNutrisi().size() - 1) {
                    nutrisiStr.append(" • ");
                }
            }
            holder.tvNutrisi.setText(nutrisiStr.toString());
        } else {
            holder.tvNutrisi.setText("Informasi nutrisi tidak tersedia");
        }

        // Format ingredients list with bullet points and line breaks
        if (resep.getBahan() != null && !resep.getBahan().isEmpty()) {
            StringBuilder bahanStr = new StringBuilder();
            for (String b : resep.getBahan()) {
                bahanStr.append("• ").append(b).append("\n");
            }
            holder.tvBahan.setText(bahanStr.toString().trim());
        } else {
            holder.tvBahan.setText("-");
        }

        // Format cooking steps with numbering
        if (resep.getLangkah() != null && !resep.getLangkah().isEmpty()) {
            StringBuilder langkahStr = new StringBuilder();
            for (int i = 0; i < resep.getLangkah().size(); i++) {
                langkahStr.append(i + 1).append(". ").append(resep.getLangkah().get(i)).append("\n\n");
            }
            holder.tvCaraMasak.setText(langkahStr.toString().trim());
        } else {
            holder.tvCaraMasak.setText("-");
        }

        // Load recipe image using Glide library with center crop and error placeholder
        if (resep.getImage() != null && !resep.getImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(resep.getImage())
                    .placeholder(R.drawable.placeholder_recipe)
                    .error(R.drawable.placeholder_recipe)
                    .centerCrop()
                    .into(holder.imgResep);
        } else {
            // Use placeholder if no image
            holder.imgResep.setImageResource(R.drawable.placeholder_recipe);
        }

        // Handle expand/collapse toggle for recipe details
        boolean isExpanded = expandedStates[position];
        holder.layoutExpandedContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.tvToggleIcon.setText(isExpanded ? "▲" : "▼");
        holder.tvToggleText.setText(isExpanded ? "Hide Recipe" : "Lihat Resep");

        holder.layoutToggleDetail.setOnClickListener(v -> {
            // Toggle expansion state and update UI
            expandedStates[position] = !expandedStates[position];
            notifyItemChanged(position);
        });

        // Open YouTube tutorial search for recipe name
        holder.btnTutorialVideo.setOnClickListener(v -> {
            String query = "Resep " + resep.getTitle();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)));
            holder.itemView.getContext().startActivity(intent);
        });

        // Setup restaurants in expanded content - extract from resep item
        List<RestaurantRecommendation> recipeRestaurants = resep.getRestaurants() != null ? resep.getRestaurants() : new java.util.ArrayList<>();
        if (!recipeRestaurants.isEmpty()) {
            holder.rvRestaurantsInResep.setVisibility(View.VISIBLE);
            RestaurantAdapter restaurantAdapter = new RestaurantAdapter(recipeRestaurants, restaurant -> {
                openRestaurantInMaps(restaurant, holder.itemView.getContext());
            });
            holder.rvRestaurantsInResep.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(holder.itemView.getContext()));
            holder.rvRestaurantsInResep.setAdapter(restaurantAdapter);
        } else {
            holder.rvRestaurantsInResep.setVisibility(View.VISIBLE);
            // Show empty state message
            holder.rvRestaurantsInResep.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(holder.itemView.getContext()));
            List<RestaurantRecommendation> emptyList = new java.util.ArrayList<>();
            RestaurantAdapter restaurantAdapter = new RestaurantAdapter(emptyList, restaurant -> {
                openRestaurantInMaps(restaurant, holder.itemView.getContext());
            });
            holder.rvRestaurantsInResep.setAdapter(restaurantAdapter);
        }
    }

    private void openRestaurantInMaps(RestaurantRecommendation restaurant, android.content.Context context) {
        if (restaurant == null || context == null) {
            return;
        }
        
        try {
            // Build search query dengan prioritas: nama + alamat lengkap
            String query = "";
            
            // Strategy 1: Gunakan nama + alamat lengkap (paling akurat)
            if (restaurant.getName() != null && !restaurant.getName().isEmpty() &&
                restaurant.getAddress() != null && !restaurant.getAddress().isEmpty()) {
                query = restaurant.getName() + ", " + restaurant.getAddress();
            }
            // Strategy 2: Jika alamat tidak lengkap, gunakan nama + lokasi + kota
            else if (restaurant.getName() != null && !restaurant.getName().isEmpty() &&
                     restaurant.getLocation() != null && !restaurant.getLocation().isEmpty()) {
                query = restaurant.getName() + ", " + restaurant.getLocation();
            }
            // Strategy 3: Fallback ke nama saja
            else if (restaurant.getName() != null && !restaurant.getName().isEmpty()) {
                query = restaurant.getName();
            }
            else {
                query = "Restoran";
            }
            
            android.util.Log.d("RestaurantMap", "Search query: " + query);
            
            // Build Google Maps search URL dengan query yang sudah di-encode
            // IMPORTANT: Encode properly untuk handle special characters (comma, space, dll)
            String encodedQuery = android.net.Uri.encode(query);
            String mapsUrl = "https://www.google.com/maps/search/" + encodedQuery;
            
            android.util.Log.d("RestaurantMap", "Maps URL: " + mapsUrl);
            
            // Try 1: Try opening with geo:0,0 intent (more reliable for Maps)
            try {
                // Build geo intent: geo:0,0?q=search_query
                String geoUrl = "geo:0,0?q=" + encodedQuery;
                Intent geoIntent = new Intent(Intent.ACTION_VIEW);
                geoIntent.setData(android.net.Uri.parse(geoUrl));
                geoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                android.util.Log.d("RestaurantMap", "Trying geo intent: " + geoUrl);
                
                if (geoIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(geoIntent);
                    android.util.Log.d("RestaurantMap", "Opened with geo intent");
                    return;
                }
            } catch (Exception e) {
                android.util.Log.d("RestaurantMap", "Geo intent failed: " + e.getMessage());
            }
            
            // Try 2: Direct HTTPS Maps URL (works in browser and Maps)
            try {
                Intent mapsIntent = new Intent(Intent.ACTION_VIEW);
                mapsIntent.setData(android.net.Uri.parse(mapsUrl));
                mapsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                android.util.Log.d("RestaurantMap", "Trying direct HTTPS intent");
                
                if (mapsIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(mapsIntent);
                    android.util.Log.d("RestaurantMap", "Opened with HTTPS intent");
                    return;
                } else {
                    android.util.Log.d("RestaurantMap", "HTTPS intent cannot resolve, trying anyway");
                    // Try anyway - sometimes resolveActivity returns false but intent still works
                    context.startActivity(mapsIntent);
                    android.util.Log.d("RestaurantMap", "Opened anyway (force start)");
                    return;
                }
            } catch (Exception e) {
                android.util.Log.e("RestaurantMap", "HTTPS intent failed: " + e.getMessage());
            }
            
            // Try 3: If all else fails, show Google Maps homepage
            try {
                Intent fallbackIntent = new Intent(Intent.ACTION_VIEW);
                fallbackIntent.setData(android.net.Uri.parse("https://maps.google.com"));
                fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                context.startActivity(fallbackIntent);
                android.util.Log.d("RestaurantMap", "Opened Google Maps homepage");
            } catch (Exception e) {
                android.util.Log.e("RestaurantMap", "All methods failed: " + e.getMessage());
                android.widget.Toast.makeText(context, "Tidak bisa membuka Maps", android.widget.Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            android.util.Log.e("RestaurantMap", "Unexpected error: " + e.getMessage());
            android.widget.Toast.makeText(context, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return listResep != null ? listResep.size() : 0;
    }

    static class ResepViewHolder extends RecyclerView.ViewHolder {
        ImageView imgResep;
        TextView tvJudulResep, tvSumber, tvNutrisi, tvWaktuMakan, tvLocation;
        
        // Expandable Views
        LinearLayout layoutToggleDetail, layoutExpandedContent;
        TextView tvToggleText, tvToggleIcon;
        TextView tvBahan, tvCaraMasak;
        MaterialButton btnTutorialVideo;
        RecyclerView rvRestaurantsInResep;

        public ResepViewHolder(@NonNull View itemView) {
            super(itemView);
            imgResep = itemView.findViewById(R.id.imgResep);
            tvJudulResep = itemView.findViewById(R.id.tvJudulResep);
            tvSumber = itemView.findViewById(R.id.tvSumber);
            tvNutrisi = itemView.findViewById(R.id.tvNutrisi);
            
            // Meal type indicator positioned over/inside image
            tvWaktuMakan = itemView.findViewById(R.id.tvWaktuMakan);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            
            layoutToggleDetail = itemView.findViewById(R.id.layoutToggleDetail);
            layoutExpandedContent = itemView.findViewById(R.id.layoutExpandedContent);
            tvToggleText = itemView.findViewById(R.id.tvToggleText);
            tvToggleIcon = itemView.findViewById(R.id.tvToggleIcon);
            
            tvBahan = itemView.findViewById(R.id.tvBahan);
            tvCaraMasak = itemView.findViewById(R.id.tvCaraMasak);
            btnTutorialVideo = itemView.findViewById(R.id.btnTutorialVideo);
            rvRestaurantsInResep = itemView.findViewById(R.id.rvRestaurantsInResep);
        }
    }
}
