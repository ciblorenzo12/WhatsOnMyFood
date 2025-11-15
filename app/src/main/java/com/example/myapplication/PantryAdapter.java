package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    private List<Product> products;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);
    }

    public PantryAdapter(List<Product> products, OnItemClickListener listener) {
        this.products = products;
        this.listener = listener;
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
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return products.size();
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
        TextView productNameTextView, productBrandTextView, productQuantityTextView;
        public MaterialCardView cardView; // Expose the foreground view

        public PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.product_image_view);
            productNameTextView = itemView.findViewById(R.id.product_name_text_view);
            productBrandTextView = itemView.findViewById(R.id.product_brand_text_view);
            productQuantityTextView = itemView.findViewById(R.id.product_quantity_text_view);
            cardView = itemView.findViewById(R.id.card_view);
        }

        public void bind(final Product product, final OnItemClickListener listener) {
            productNameTextView.setText(product.productName);
            productBrandTextView.setText(product.brands);
            productQuantityTextView.setText(product.quantity);
            if (product.imageUrl != null && !product.imageUrl.isEmpty()) {
                Picasso.get().load(product.imageUrl).into(productImageView);
            }
            itemView.setOnClickListener(v -> listener.onItemClick(product));
        }
    }
}
