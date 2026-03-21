package com.example.geotracker.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.geotracker.data.AppDatabase;
import com.example.geotracker.data.LocationEntity;
import com.example.geotracker.databinding.FragmentLocationListBinding;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationListFragment extends Fragment {

    private FragmentLocationListBinding binding;
    private LocationAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLocationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new LocationAdapter();
        binding.rvLocations.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvLocations.setAdapter(adapter);

        AppDatabase.getInstance(requireContext()).locationDao().getAllLocations().observe(getViewLifecycleOwner(), locations -> {
            adapter.setLocations(locations);
        });

        adapter.setOnItemClickListener(new LocationAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(LocationEntity location) {
                executor.execute(() -> {
                    AppDatabase.getInstance(requireContext()).locationDao().delete(location);
                });
            }

            @Override
            public void onNavigateClick(LocationEntity location) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + location.latitude + "," + location.longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivity(mapIntent);
                }
            }
        });
    }
}
