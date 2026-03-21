package com.example.geotracker.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.geotracker.R;
import com.example.geotracker.data.LocationEntity;
import java.util.ArrayList;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {

    private List<LocationEntity> locations = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(LocationEntity location);
        void onNavigateClick(LocationEntity location);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setLocations(List<LocationEntity> locations) {
        this.locations = locations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationEntity location = locations.get(position);
        holder.tvName.setText(location.name);
        holder.tvCoords.setText(String.format("%.4f, %.4f", location.latitude, location.longitude));

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(location);
        });

        holder.btnNavigate.setOnClickListener(v -> {
            if (listener != null) listener.onNavigateClick(location);
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCoords;
        ImageButton btnDelete, btnNavigate;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_location_name);
            tvCoords = itemView.findViewById(R.id.tv_coordinates);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnNavigate = itemView.findViewById(R.id.btn_navigate);
        }
    }
}
