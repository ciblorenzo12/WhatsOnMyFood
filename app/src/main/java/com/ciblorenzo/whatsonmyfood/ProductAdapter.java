package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.Objects;

public class ProductAdapter extends ListAdapter<Product, ProductAdapter.ProductViewHolder> {

    public ProductAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Product> DIFF_CALLBACK = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return oldItem.barcode.equals(newItem.barcode);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product oldItem, @NonNull Product newItem) {
            return Objects.equals(oldItem.productName, newItem.productName) &&
                   Objects.equals(oldItem.brands, newItem.brands) &&
                   Objects.equals(oldItem.quantity, newItem.quantity) &&
                   Objects.equals(oldItem.imageUrl, newItem.imageUrl) &&
                   Objects.equals(oldItem.labels, newItem.labels) &&
                   Objects.equals(oldItem.packaging, newItem.packaging);
        }
    };

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pantry_list_item, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = getItem(position);
        holder.productNameTextView.setText(product.productName != null ? product.productName : "N/A");
        holder.productBrandTextView.setText(product.brands != null ? product.brands : "");
        holder.productQuantityTextView.setText(product.quantity != null ? product.quantity : "");

        if (product.imageUrl != null && !product.imageUrl.isEmpty()) {
            Picasso.get().load(product.imageUrl).into(holder.productImageView);
        } else {
            holder.productImageView.setImageResource(R.drawable.ic_pantry); // Placeholder
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, ProductDetailsActivity.class);
            intent.putExtra(ProductDetailsActivity.EXTRA_BARCODE, product.barcode);
            context.startActivity(intent);
        });
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImageView;
        TextView productNameTextView;
        TextView productBrandTextView;
        TextView productQuantityTextView;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.product_image_view);
            productNameTextView = itemView.findViewById(R.id.product_name_text_view);
            productBrandTextView = itemView.findViewById(R.id.product_brand_text_view);
            productQuantityTextView = itemView.findViewById(R.id.product_quantity_text_view);
        }
    }
}
