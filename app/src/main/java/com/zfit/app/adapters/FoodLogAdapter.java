package com.zfit.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zfit.app.R;
import com.zfit.app.models.FoodEntry;

import java.util.List;

public class FoodLogAdapter extends RecyclerView.Adapter<FoodLogAdapter.ViewHolder> {

    private final List<FoodEntry> entries;
    private final OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(FoodEntry entry);
    }

    public FoodLogAdapter(List<FoodEntry> entries, OnDeleteListener deleteListener) {
        this.entries = entries;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodEntry entry = entries.get(position);
        holder.tvFoodName.setText(entry.getFoodName());
        holder.tvMealType.setText(entry.getMealType());
        holder.tvCalories.setText(entry.getCalories() + " kcal");
        holder.tvMacros.setText(String.format("P %.0fg  C %.0fg  F %.0fg",
                entry.getProtein(), entry.getCarbs(), entry.getFat()));
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(entry);
            });
        }
        holder.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(entry);
            return true;
        });
    }

    @Override
    public int getItemCount() { return entries.size(); }

    public void updateData(List<FoodEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodName, tvMealType, tvCalories, tvMacros;
        Button btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvMealType = itemView.findViewById(R.id.tvMealType);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvMacros = itemView.findViewById(R.id.tvMacros);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
