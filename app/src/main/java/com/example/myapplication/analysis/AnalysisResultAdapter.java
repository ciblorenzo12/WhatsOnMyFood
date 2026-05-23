package com.example.myapplication.analysis;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.WebViewActivity;
import com.example.myapplication.utils.GlassMotion;
import com.example.myapplication.utils.LinkHandler;

import java.util.List;

public class AnalysisResultAdapter extends RecyclerView.Adapter<AnalysisResultAdapter.ViewHolder> {

    private final List<AnalysisResult> results;

    public AnalysisResultAdapter(List<AnalysisResult> results) {
        this.results = results;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.analysis_warning_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnalysisResult result = results.get(position);
        holder.warningMessage.setText(result.getMessage());
        GlassMotion.enter(holder.itemView, Math.min(position * 25L, 160L));

        // Set icon based on warning level
        switch (result.getLevel()) {
            case INFO:
                holder.warningIcon.setImageResource(android.R.drawable.ic_dialog_info);
                holder.warningIcon.setColorFilter(0xFF00FF00); // Green
                break;
            case WARNING:
                holder.warningIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                holder.warningIcon.setColorFilter(0xFFFFA500); // Orange
                break;
            case SEVERE:
                holder.warningIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                holder.warningIcon.setColorFilter(0xFFFF0000); // Red
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle(result.getMessage())
                    .setMessage(result.getExplanation())
                    .setPositiveButton("OK", null)
                    .show();
        });

        if (result.getSourceUrl() != null && !result.getSourceUrl().isEmpty()) {
            holder.viewSourceButton.setVisibility(View.VISIBLE);
            holder.viewSourceButton.setOnClickListener(v -> {
                LinkHandler.openLink(v.getContext(), result.getSourceUrl(), "Verification: " + result.getMessage(), result.getVisualQuote());
            });
        } else {
            holder.viewSourceButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView warningIcon;
        TextView warningMessage;
        Button viewSourceButton;

        ViewHolder(View view) {
            super(view);
            warningIcon = view.findViewById(R.id.warning_icon);
            warningMessage = view.findViewById(R.id.warning_message);
            viewSourceButton = view.findViewById(R.id.view_source_button);
            GlassMotion.attachPress(view);
        }
    }
}
