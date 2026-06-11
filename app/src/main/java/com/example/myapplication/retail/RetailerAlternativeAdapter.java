package com.example.myapplication.retail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.utils.GlassMotion;
import com.example.myapplication.utils.LinkHandler;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class RetailerAlternativeAdapter extends RecyclerView.Adapter<RetailerAlternativeAdapter.ViewHolder> {

    private final Context context;
    private final List<RetailerAlternative> items = new ArrayList<>();

    public RetailerAlternativeAdapter(Context context) {
        this.context = context;
    }

    public void submitList(List<RetailerAlternative> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.retailer_alternative_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RetailerAlternative item = items.get(position);
        holder.productName.setText(item.productName);
        holder.brand.setText(item.brand);
        holder.reason.setText(item.reason);
        holder.signal.setText(context.getString(R.string.alternative_healthy_option));
        holder.category.setText(context.getString(R.string.alternative_same_category_type));
        holder.retailerHint.setText(individualMarketHint(item.retailerHint));
        holder.viewButton.setEnabled(item.productUrl != null && !item.productUrl.trim().isEmpty());
        holder.viewButton.setOnClickListener(v -> LinkHandler.openLink(context, item.productUrl, item.productName, item.reason));
        GlassMotion.attachPress(holder.viewButton);

        if (item.imageUrl != null && !item.imageUrl.trim().isEmpty()) {
            holder.productImage.setVisibility(View.VISIBLE);
            holder.placeholderLayout.setVisibility(View.GONE);
            Picasso.get()
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_pantry)
                    .error(R.drawable.ic_pantry)
                    .into(holder.productImage);
        } else {
            holder.productImage.setVisibility(View.VISIBLE);
            holder.placeholderLayout.setVisibility(View.GONE);
            holder.productImage.setImageBitmap(createProductThumbnail(item));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView productImage;
        final TextView productName;
        final TextView brand;
        final TextView reason;
        final TextView signal;
        final TextView category;
        final TextView retailerHint;
        final LinearLayout placeholderLayout;
        final TextView placeholderInitials;
        final TextView placeholderLabel;
        final Button viewButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.alternative_image_view);
            placeholderLayout = itemView.findViewById(R.id.alternative_placeholder_layout);
            placeholderInitials = itemView.findViewById(R.id.alternative_placeholder_initials_text_view);
            placeholderLabel = itemView.findViewById(R.id.alternative_placeholder_label_text_view);
            productName = itemView.findViewById(R.id.alternative_name_text_view);
            brand = itemView.findViewById(R.id.alternative_brand_text_view);
            reason = itemView.findViewById(R.id.alternative_reason_text_view);
            signal = itemView.findViewById(R.id.alternative_signal_text_view);
            category = itemView.findViewById(R.id.alternative_category_text_view);
            retailerHint = itemView.findViewById(R.id.alternative_retailer_hint_text_view);
            viewButton = itemView.findViewById(R.id.alternative_view_button);
        }
    }

    private String initialsFor(String brand, String productName) {
        String source = brand != null && !brand.trim().isEmpty() ? brand : productName;
        if (source == null || source.trim().isEmpty()) return "OK";
        StringBuilder initials = new StringBuilder();
        for (String part : source.trim().split("\\s+")) {
            if (part.isEmpty()) continue;
            initials.append(Character.toUpperCase(part.charAt(0)));
            if (initials.length() >= 2) break;
        }
        return initials.length() > 0 ? initials.toString() : "OK";
    }

    private String categoryLabelFor(RetailerAlternative item) {
        String text = ((item.healthSignal != null ? item.healthSignal : "") + " "
                + (item.reason != null ? item.reason : "") + " "
                + (item.productName != null ? item.productName : "")).toLowerCase();
        if (text.contains("soda") || text.contains("cola")) return "Soda";
        if (text.contains("beverage") || text.contains("drink") || text.contains("water")) return "Beverage";
        if (text.contains("bar")) return "Bar";
        if (text.contains("cereal")) return "Cereal";
        if (text.contains("granola")) return "Granola";
        if (text.contains("yogurt") || text.contains("skyr")) return "Yogurt";
        if (text.contains("chocolate") || text.contains("candy")) return "Chocolate";
        if (text.contains("cookie")) return "Cookie";
        if (text.contains("chip") || text.contains("snack") || text.contains("popcorn")) return "Snack";
        if (text.contains("bread")) return "Bread";
        if (text.contains("pasta")) return "Pasta";
        if (text.contains("sauce") || text.contains("dressing")) return "Sauce";
        if (text.contains("butter")) return "Spread";
        if (text.contains("milk")) return "Milk";
        return "Product";
    }

    private String individualMarketHint(String hint) {
        if (hint == null || hint.trim().isEmpty()) {
            return context.getString(R.string.alternative_available_default);
        }
        String lower = hint.toLowerCase();
        if (lower.contains("many grocery retailers") || lower.contains("select grocery retailers")
                || lower.contains("major grocery stores")) {
            return context.getString(R.string.alternative_available_major_markets);
        }
        if (lower.contains("health grocery stores")) {
            return context.getString(R.string.alternative_available_health_markets);
        }
        return hint;
    }

    private Bitmap createProductThumbnail(RetailerAlternative item) {
        int width = 480;
        int height = 270;
        int accent = accentColorFor(item);
        String category = categoryLabelFor(item);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setShader(new LinearGradient(0, 0, width, height,
                blendWithWhite(accent, 0.82f), Color.WHITE, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(0, 0, width, height), 34f, 34f, paint);
        paint.setShader(null);

        paint.setColor(blendWithWhite(accent, 0.65f));
        canvas.drawRoundRect(new RectF(70, 212, 410, 232), 18f, 18f, paint);

        RectF pack = new RectF(136, 24, 344, 242);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(pack, 24f, 24f, paint);
        paint.setColor(accent);
        canvas.drawRoundRect(new RectF(pack.left, pack.top, pack.right, pack.top + 64), 24f, 24f, paint);
        canvas.drawRect(pack.left, pack.top + 36, pack.right, pack.top + 64, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(blendWithBlack(accent, 0.12f));
        canvas.drawRoundRect(pack, 24f, 24f, paint);
        paint.setStyle(Paint.Style.FILL);

        drawFittedText(canvas, safeText(item.brand, "Better Choice"), pack.centerX(), 62f,
                178f, 28f, Color.WHITE, true);
        drawFittedText(canvas, safeText(item.productName, "Healthy Option"), pack.centerX(), 128f,
                170f, 27f, Color.rgb(30, 41, 59), true);
        drawFittedText(canvas, category.toUpperCase(), pack.centerX(), 166f,
                150f, 21f, blendWithBlack(accent, 0.10f), false);

        paint.setColor(blendWithWhite(accent, 0.78f));
        canvas.drawRoundRect(new RectF(174, 184, 306, 214), 15f, 15f, paint);
        drawFittedText(canvas, "HEALTHY", pack.centerX(), 206f,
                108f, 18f, blendWithBlack(accent, 0.18f), true);

        paint.setColor(accent);
        canvas.drawCircle(154, 42, 11, paint);
        canvas.drawCircle(326, 42, 11, paint);
        return bitmap;
    }

    private int accentColorFor(RetailerAlternative item) {
        String category = categoryLabelFor(item).toLowerCase();
        if (category.contains("soda") || category.contains("beverage")) return Color.rgb(20, 132, 144);
        if (category.contains("bar") || category.contains("granola")) return Color.rgb(161, 98, 7);
        if (category.contains("cereal") || category.contains("bread") || category.contains("pasta")) return Color.rgb(180, 83, 9);
        if (category.contains("yogurt") || category.contains("milk")) return Color.rgb(37, 99, 235);
        if (category.contains("chocolate") || category.contains("cookie")) return Color.rgb(109, 40, 217);
        if (category.contains("snack")) return Color.rgb(5, 150, 105);
        if (category.contains("sauce") || category.contains("spread")) return Color.rgb(220, 38, 38);
        return Color.rgb(5, 150, 105);
    }

    private void drawFittedText(Canvas canvas, String text, float centerX, float baseline,
                                float maxWidth, float maxSize, int color, boolean bold) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        paint.setTextSize(maxSize);
        String display = oneLine(text);
        while (paint.measureText(display) > maxWidth && display.length() > 4) {
            display = display.substring(0, display.length() - 4).trim() + "...";
        }
        while (paint.measureText(display) > maxWidth && paint.getTextSize() > 12f) {
            paint.setTextSize(paint.getTextSize() - 1f);
        }
        canvas.drawText(display, centerX, baseline, paint);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String oneLine(String value) {
        return safeText(value, "").replaceAll("\\s+", " ");
    }

    private int blendWithWhite(int color, float amount) {
        return blend(color, Color.WHITE, amount);
    }

    private int blendWithBlack(int color, float amount) {
        return blend(color, Color.BLACK, amount);
    }

    private int blend(int from, int to, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        int red = (int) (Color.red(from) * (1f - clamped) + Color.red(to) * clamped);
        int green = (int) (Color.green(from) * (1f - clamped) + Color.green(to) * clamped);
        int blue = (int) (Color.blue(from) * (1f - clamped) + Color.blue(to) * clamped);
        return Color.rgb(red, green, blue);
    }
}
