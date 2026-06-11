package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.utils.GlassMotion;
import com.example.myapplication.utils.LinkHandler;

import java.util.ArrayList;
import java.util.List;

public class AdditiveDatabaseAdapter extends RecyclerView.Adapter<AdditiveDatabaseAdapter.ViewHolder> {
    private final List<AdditiveEntry> entries = new ArrayList<>();

    public AdditiveDatabaseAdapter(List<AdditiveEntry> initialEntries) {
        updateEntries(initialEntries);
    }

    public void updateEntries(List<AdditiveEntry> nextEntries) {
        entries.clear();
        if (nextEntries != null) {
            entries.addAll(nextEntries);
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
        holder.nameTextView.setText(entry.name);
        holder.categoryTextView.setText(entry.category);
        holder.aliasTextView.setText(entry.aliases);
        holder.functionTextView.setText(entry.function);
        holder.explanationTextView.setText(entry.explanation);
        holder.noteTextView.setText(entry.note);
        holder.sourceButton.setOnClickListener(v -> LinkHandler.openLink(v.getContext(), entry.sourceUrl, entry.sourceTitle, entry.function));
        
        // Apply color based on health status
        int statusColor;
        switch (entry.status) {
            case RECOMMENDED:
                statusColor = 0xFF2ECC71; // Green
                break;
            case NOT_RECOMMENDED:
                statusColor = 0xFFE74C3C; // Red
                break;
            case MODERATE:
            default:
                statusColor = 0xFFF39C12; // Orange/Yellow
                break;
        }
        holder.categoryTextView.getBackground().setTint(statusColor);

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
