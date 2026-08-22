package com.ciblorenzo.whatsonmyfood.retail;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.R;
import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MarketplaceAdapter extends RecyclerView.Adapter<MarketplaceAdapter.ViewHolder> {

    private final Context context;
    private final List<MarketplaceItem> items = new ArrayList<>();

    public MarketplaceAdapter(Context context) {
        this.context = context;
    }

    public void submitList(List<MarketplaceItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.marketplace_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MarketplaceItem item = items.get(position);

        holder.itemRole.setText(item.type == MarketplaceItem.ItemType.ORIGINAL
                ? R.string.marketplace_item_original
                : R.string.marketplace_item_alternative);
        holder.productName.setText(MarketplacePresentation.safeText(item.productName, "Product name unavailable"));
        boolean hasBrand = item.brand != null && !item.brand.trim().isEmpty();
        holder.brandName.setVisibility(hasBrand ? View.VISIBLE : View.GONE);
        if (hasBrand) holder.brandName.setText(item.brand.trim());
        holder.retailerName.setText(MarketplacePresentation.safeText(item.retailerName, "Retailer availability varies"));
        holder.priceText.setText(MarketplacePresentation.safeText(item.price, "Price unavailable"));
        holder.distanceText.setText(MarketplacePresentation.safeText(item.distance, "Availability varies"));
        holder.sourceLabel.setText(MarketplacePresentation.safeText(item.sourceLabel, "PROVIDER SOURCE UNKNOWN"));
        bindComparison(holder, item);

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            holder.productImage.setContentDescription(context.getString(
                    R.string.marketplace_product_image, holder.productName.getText()));
            Picasso.get()
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_pantry)
                    .error(R.drawable.ic_pantry)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(R.drawable.ic_pantry);
            holder.productImage.setContentDescription(context.getString(
                    R.string.marketplace_product_image_unavailable, holder.productName.getText()));
        }

        boolean hasProductLink = item.productUrl != null && !item.productUrl.trim().isEmpty();
        holder.retailerAction.setText(hasProductLink
                ? R.string.marketplace_action_view_retailer
                : R.string.marketplace_action_unavailable);
        holder.retailerAction.setEnabled(hasProductLink);
        holder.retailerAction.setOnClickListener(hasProductLink
                ? v -> LinkHandler.openRetailerLink(context, item.productUrl, item.retailerName)
                : null);

        RetailerBrandAssets brand = RetailerBrandAssets.resolve(item.retailerName);
        if (brand.logoUrl != null) {
            Picasso.get()
                    .load(brand.logoUrl)
                    .placeholder(brand.logoResId)
                    .error(brand.logoResId)
                    .into(holder.retailerLogo);
        } else {
            holder.retailerLogo.setImageResource(brand.logoResId);
        }
    }

    private void bindComparison(ViewHolder holder, MarketplaceItem item) {
        if (item.healthScore >= 0) {
            holder.healthScore.setText(context.getString(R.string.marketplace_score_value, item.healthScore));
            holder.healthScore.getBackground().setTint(getScoreColor(item.healthScore));
        } else {
            holder.healthScore.setText(R.string.marketplace_score_unavailable);
            holder.healthScore.getBackground().setTint(Color.parseColor("#6B7280"));
        }

        int comparisonText;
        switch (item.comparisonCue) {
            case REFERENCE:
                comparisonText = R.string.marketplace_comparison_reference;
                break;
            case HIGHER:
                comparisonText = R.string.marketplace_comparison_higher;
                break;
            case SIMILAR:
                comparisonText = R.string.marketplace_comparison_similar;
                break;
            case LOWER:
                comparisonText = R.string.marketplace_comparison_lower;
                break;
            case UNAVAILABLE:
            default:
                comparisonText = R.string.marketplace_comparison_unavailable;
                break;
        }
        holder.comparisonCue.setText(comparisonText);
    }

    private int getScoreColor(int score) {
        if (score >= 80) return Color.parseColor("#4CAF50");
        if (score >= 60) return Color.parseColor("#FFC107");
        return Color.parseColor("#F44336");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView productImage;
        final TextView itemRole;
        final TextView productName;
        final TextView brandName;
        final ImageView retailerLogo;
        final TextView retailerName;
        final TextView healthScore;
        final TextView priceText;
        final TextView distanceText;
        final TextView sourceLabel;
        final TextView comparisonCue;
        final MaterialButton retailerAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            itemRole = itemView.findViewById(R.id.marketplace_item_role);
            productName = itemView.findViewById(R.id.product_name);
            brandName = itemView.findViewById(R.id.brand_name);
            retailerLogo = itemView.findViewById(R.id.retailer_logo);
            retailerName = itemView.findViewById(R.id.retailer_name);
            healthScore = itemView.findViewById(R.id.health_score);
            priceText = itemView.findViewById(R.id.price_text);
            distanceText = itemView.findViewById(R.id.distance_text);
            sourceLabel = itemView.findViewById(R.id.provider_source_text);
            comparisonCue = itemView.findViewById(R.id.comparison_cue_text);
            retailerAction = itemView.findViewById(R.id.retailer_action_button);
        }
    }
}
