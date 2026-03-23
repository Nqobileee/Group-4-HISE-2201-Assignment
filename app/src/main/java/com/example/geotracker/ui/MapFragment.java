package com.example.geotracker.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.geotracker.data.AppDatabase;
import com.example.geotracker.data.LocationEntity;
import com.example.geotracker.databinding.FragmentMapBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // OSMdroid configuration
        Context ctx = requireContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Map
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.setMultiTouchControls(true);
        mapController = binding.mapView.getController();
        mapController.setZoom(15.0);

        // My Location Overlay (The Blue Dot)
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), binding.mapView);
        locationOverlay.enableMyLocation();
        binding.mapView.getOverlays().add(locationOverlay);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Long Press to Save Location
        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                showSaveDialog(p);
                return true;
            }
        };
        binding.mapView.getOverlays().add(new MapEventsOverlay(mReceive));

        binding.fabAddLocation.setOnClickListener(v -> {
            GeoPoint center = (GeoPoint) binding.mapView.getMapCenter();
            showSaveDialog(center);
        });

        setupLocationUpdates();
        loadSavedMarkers();
    }

    private void setupLocationUpdates() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update camera if needed
                }
            }
        };
    }

    private void loadSavedMarkers() {
        AppDatabase.getInstance(requireContext()).locationDao().getAllLocations().observe(getViewLifecycleOwner(), locations -> {
            binding.mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker);
            for (LocationEntity loc : locations) {
                Marker startMarker = new Marker(binding.mapView);
                startMarker.setPosition(new GeoPoint(loc.latitude, loc.longitude));
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                startMarker.setTitle(loc.name);
                binding.mapView.getOverlays().add(startMarker);
            }
            binding.mapView.invalidate();
        });
    }

    private void showSaveDialog(GeoPoint point) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Save Landmark");
        
        final EditText input = new EditText(requireContext());
        input.setHint("Name (e.g. My School)");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            saveLocation(name.isEmpty() ? "Saved Point" : name, point.getLatitude(), point.getLongitude());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveLocation(String name, double lat, double lng) {
        executor.execute(() -> {
            LocationEntity location = new LocationEntity(name, lat, lng);
            AppDatabase.getInstance(requireContext()).locationDao().insert(location);
            requireActivity().runOnUiThread(() -> 
                Toast.makeText(getContext(), "Saved: " + name, Toast.LENGTH_SHORT).show());
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build();
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                mapController.setCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.mapView.onResume();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.mapView.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
