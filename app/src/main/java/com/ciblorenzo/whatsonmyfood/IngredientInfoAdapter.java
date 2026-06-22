package com.ciblorenzo.whatsonmyfood;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class IngredientInfoAdapter extends RecyclerView.Adapter<IngredientInfoAdapter.ViewHolder> {

    private List<IngredientInfo> ingredients;

    public IngredientInfoAdapter(List<IngredientInfo> ingredients) {
        this.ingredients = ingredients;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IngredientInfo info = ingredients.get(position);
        holder.nameText.setText(info.getName());
        holder.purposeText.setText(info.getPurpose());
        holder.healthText.setText(info.getHealthNote());
        holder.categoryText.setText(info.getCategory());
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    public void updateList(List<IngredientInfo> newList) {
        this.ingredients = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, purposeText, healthText, categoryText;

        ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.ingredient_name_text);
            purposeText = view.findViewById(R.id.ingredient_purpose_text);
            healthText = view.findViewById(R.id.ingredient_health_text);
            categoryText = view.findViewById(R.id.ingredient_category_text);
        }
    }
}
