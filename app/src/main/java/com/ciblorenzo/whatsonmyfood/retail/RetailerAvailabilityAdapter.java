package com.ciblorenzo.whatsonmyfood.retail;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.R;
import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class RetailerAvailabilityAdapter extends RecyclerView.Adapter<RetailerAvailabilityAdapter.ViewHolder> {

    private final Context context;
    private final List<RetailerAvailability> items = new ArrayList<>();

    public RetailerAvailabilityAdapter(Context context) {
        this.context = context;
    }

    public void submitList(List<RetailerAvailability> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.retailer_availability_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RetailerAvailability item = items.get(position);
        RetailerBrandAssets brand = RetailerBrandAssets.resolve(item.retailerName);
        holder.brandStrip.setBackgroundColor(brand.brandColor);
        holder.logoTile.setBackground(makeLogoTile(brand.brandColor));
        holder.logoImage.setVisibility(View.VISIBLE);
        holder.logoText.setVisibility(View.GONE);
        if (brand.logoUrl != null) {
            Picasso.get()
                    .load(brand.logoUrl)
                    .placeholder(brand.logoResId)
                    .error(brand.logoResId)
                    .into(holder.logoImage);
        } else {
            holder.logoImage.setImageResource(brand.logoResId);
        }
        holder.logoTile.setContentDescription(item.retailerName + " logo");
        holder.retailerName.setText(item.retailerName);
        holder.status.setText(item.availabilityStatus);
        holder.status.setTextColor(item.available ? Color.parseColor("#057A45") : Color.parseColor("#6B7280"));
        holder.status.setBackground(makeRoundRect(item.available ? Color.parseColor("#E8F8EF") : Color.parseColor("#F3F4F6"), 999f));
        holder.meta.setText(buildMeta(item));
        boolean hasNote = item.note != null && !item.note.trim().isEmpty();
        holder.note.setText(hasNote ? item.note.trim() : "");
        holder.note.setVisibility(hasNote ? View.VISIBLE : View.GONE);
        boolean hasAddress = item.address != null && !item.address.trim().isEmpty();
        holder.address.setText(hasAddress ? item.address.trim() : "");
        holder.address.setVisibility(hasAddress ? View.VISIBLE : View.GONE);
        holder.address.setOnClickListener(hasAddress && item.mapUrl != null && !item.mapUrl.trim().isEmpty()
                ? v -> LinkHandler.openMapLink(context, item.mapUrl, item.retailerName + " map")
                : null);
        holder.shopButton.setText(item.available ? R.string.shop_now : R.string.search_store);
        holder.shopButton.setEnabled(item.productUrl != null && !item.productUrl.trim().isEmpty());
        holder.shopButton.setOnClickListener(v -> LinkHandler.openRetailerLink(context, item.productUrl, item.retailerName));
        GlassMotion.attachPress(holder.shopButton);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String buildMeta(RetailerAvailability item) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, item.price);
        appendPart(builder, item.distance);
        appendPart(builder, item.fulfillment);
        return builder.toString();
    }

    private void appendPart(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (builder.length() > 0) builder.append(" | ");
        builder.append(value.trim());
    }

    private GradientDrawable makeRoundRect(int color, float radiusDp) {
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * density);
        return drawable;
    }

    private GradientDrawable makeLogoTile(int strokeColor) {
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(18f * density);
        drawable.setStroke(Math.max(1, Math.round(1.5f * density)), strokeColor);
        return drawable;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View brandStrip;
        final View logoTile;
        final ImageView logoImage;
        final TextView logoText;
        final TextView retailerName;
        final TextView status;
        final TextView meta;
        final TextView note;
        final TextView address;
        final Button shopButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            brandStrip = itemView.findViewById(R.id.retailer_brand_strip);
            logoTile = itemView.findViewById(R.id.retailer_logo_tile);
            logoImage = itemView.findViewById(R.id.retailer_logo_image_view);
            logoText = itemView.findViewById(R.id.retailer_logo_text_view);
            retailerName = itemView.findViewById(R.id.retailer_name_text_view);
            status = itemView.findViewById(R.id.retailer_status_text_view);
            meta = itemView.findViewById(R.id.retailer_meta_text_view);
            note = itemView.findViewById(R.id.retailer_note_text_view);
            address = itemView.findViewById(R.id.retailer_address_text_view);
            shopButton = itemView.findViewById(R.id.retailer_shop_button);
        }
    }
}
