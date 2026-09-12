package com.ciblorenzo.whatsonmyfood;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.ciblorenzo.whatsonmyfood.utils.LinkHandler;

import java.util.ArrayList;
import java.util.List;

public class AdditiveDatabaseAdapter extends RecyclerView.Adapter<AdditiveDatabaseAdapter.ViewHolder> {
    private final List<AdditiveEntry> entries = new ArrayList<>();

    private java.util.function.Consumer<AdditiveEntry> onSelect;
    private String selectedName = "";
    public void setSelected(String name) { selectedName=name; notifyDataSetChanged(); }
    public void setOnSelect(java.util.function.Consumer<AdditiveEntry> listener){onSelect=listener;}

    public AdditiveDatabaseAdapter(List<AdditiveEntry> initialEntries) {
        updateEntries(initialEntries);
    }

    public void updateEntries(List<AdditiveEntry> nextEntries) {
        entries.clear();
        if (nextEntries != null) {
            for (AdditiveEntry entry : nextEntries) {
                if (entry != null && entry.isValid()) entries.add(entry);
            }
        }
        notifyDataSetChanged();
    }

    public List<AdditiveEntry> getEntries() {
        return entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.additive_database_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdditiveEntry entry = entries.get(position);
        boolean selected=entry.name.equals(selectedName);
        holder.itemView.setSelected(selected);
        ((com.google.android.material.card.MaterialCardView)holder.itemView).setStrokeColor(
                holder.itemView.getContext().getColor(selected?R.color.colorPrimary:R.color.divider));
        holder.nameTextView.setText(entry.name);
        holder.categoryTextView.setText(entry.category);
        holder.aliasTextView.setText(entry.aliases);
        holder.functionTextView.setText(entry.function);
        holder.explanationTextView.setText(entry.explanation);
        int status = entry.status == AdditiveEntry.HealthStatus.NOT_RECOMMENDED ? R.string.ui_caution
                : entry.status == AdditiveEntry.HealthStatus.RECOMMENDED ? R.string.ui_general_context : R.string.ui_usual_use;
        holder.noteTextView.setText(status);
        holder.noteTextView.setTextColor(holder.itemView.getContext().getColor(
                entry.status == AdditiveEntry.HealthStatus.NOT_RECOMMENDED ? R.color.ui_alert_text : R.color.text_secondary));
        holder.sourceButton.setText(R.string.ui_details);
        android.view.View.OnClickListener open = v -> { if(onSelect!=null)onSelect.accept(entry);else {
            com.ciblorenzo.whatsonmyfood.ui.IngredientDetailsView detail=new com.ciblorenzo.whatsonmyfood.ui.IngredientDetailsView(v.getContext(),null);
            detail.bind(entry);
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(v.getContext()).setView(detail).setPositiveButton(R.string.ui_close,null).show();
        }};
        holder.sourceButton.setOnClickListener(open);holder.itemView.setOnClickListener(open);

        GlassMotion.enter(holder.itemView, Math.min(position * 25L, 160L));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView categoryTextView;
        TextView aliasTextView;
        TextView functionTextView;
        TextView explanationTextView;
        TextView noteTextView;
        Button sourceButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.additive_name_text_view);
            categoryTextView = itemView.findViewById(R.id.additive_category_text_view);
            aliasTextView = itemView.findViewById(R.id.additive_alias_text_view);
            functionTextView = itemView.findViewById(R.id.additive_function_text_view);
            explanationTextView = itemView.findViewById(R.id.additive_explanation_text_view);
            noteTextView = itemView.findViewById(R.id.additive_note_text_view);
            sourceButton = itemView.findViewById(R.id.additive_source_button);
            GlassMotion.attachPress(itemView);
        }
    }
}
