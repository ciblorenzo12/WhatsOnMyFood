package com.ciblorenzo.whatsonmyfood;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    private List<Product> products;
    private final OnItemClickListener listener;
    private final OnRiskRatingChangeListener riskRatingChangeListener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public interface OnRiskRatingChangeListener {
        void onRiskRatingChanged(Product product, int score);
    }

    public PantryAdapter(List<Product> products, OnItemClickListener listener, OnRiskRatingChangeListener riskRatingChangeListener) {
        this.products = products;
        this.listener = listener;
        this.riskRatingChangeListener = riskRatingChangeListener;
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pantry_list_item, parent, false);
        return new PantryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product, listener, riskRatingChangeListener);
        GlassMotion.enter(holder.cardView, Math.min(position * 35L, 220L));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateList(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        products.remove(position);
        notifyItemRemoved(position);
    }

    public Product getProductAt(int position) {
        return products.get(position);
    }

    static class PantryViewHolder extends RecyclerView.ViewHolder {
        ImageView productImageView;
        TextView productNameTextView, productBrandTextView, productQuantityTextView, aiVerifiedBadge;
        RatingBar userRiskRatingBar;
        public MaterialCardView cardView; // Expose the foreground view

        public PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.product_image_view);
            productNameTextView = itemView.findViewById(R.id.product_name_text_view);
            productBrandTextView = itemView.findViewById(R.id.product_brand_text_view);
            productQuantityTextView = itemView.findViewById(R.id.product_quantity_text_view);
            aiVerifiedBadge = itemView.findViewById(R.id.ai_verified_badge);
            userRiskRatingBar = itemView.findViewById(R.id.user_risk_rating_bar);
            cardView = itemView.findViewById(R.id.card_view);
            GlassMotion.attachPress(cardView);
        }

        public void bind(final Product product, final OnItemClickListener listener, final OnRiskRatingChangeListener riskRatingChangeListener) {
            productNameTextView.setText(product.productName);
            productBrandTextView.setText(product.brands);
            productQuantityTextView.setText(product.quantity);
            userRiskRatingBar.setOnRatingBarChangeListener(null);
            int userScore = product.userIngredientRiskScore == null ? 0 : product.userIngredientRiskScore;
            userRiskRatingBar.setRating(userScore / 20f);
            userRiskRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                if (!fromUser) return;
                int score = Math.round(rating * 20f);
                product.userIngredientRiskScore = score;
                riskRatingChangeListener.onRiskRatingChanged(product, score);
            });

            if (product.healthScore != null) {
                aiVerifiedBadge.setVisibility(View.VISIBLE);
            } else {
                aiVerifiedBadge.setVisibility(View.GONE);
            }

            if (product.imageUrl != null && !product.imageUrl.isEmpty()) {
                Picasso.get()
                        .load(product.imageUrl)
                        .placeholder(R.drawable.ic_scan)
                        .error(R.drawable.ic_scan)
                        .resize(100, 100)
                        .centerCrop()
                        .into(productImageView);
            } else {
                productImageView.setImageResource(R.drawable.ic_scan);
            }
            itemView.setOnClickListener(v -> listener.onItemClick(product));
        }
    }
}
