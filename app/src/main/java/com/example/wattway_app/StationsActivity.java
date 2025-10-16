package com.example.wattway_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class StationsActivity extends AppCompatActivity {

    // OpenChargeMap API Key
    private static final String OCM_API_KEY = "5697ac5d-7169-4f6c-ab6c-047272aa2c34";
    private static final String BASE_URL = "https://api.openchargemap.io/v3/";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private LinearLayout stationsContainer;
    private ProgressBar progressBar;
    private TextView tvNoStations;
    private EditText etSearchStation;
    private ChipGroup chipFilters;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private List<ChargingStation> allStations = new ArrayList<>();
    private List<ChargingStation> filteredStations = new ArrayList<>();
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations);

        initializeViews();
        setupBottomNav();
        setupSearch();
        setupFilters();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (checkLocationPermission()) {
            getCurrentLocationAndLoadStations();
        }
    }

    private void initializeViews() {
        stationsContainer = findViewById(R.id.stationsContainer);
        progressBar = findViewById(R.id.progressBar);
        tvNoStations = findViewById(R.id.tvNoStations);
        etSearchStation = findViewById(R.id.etSearchStation);
        chipFilters = findViewById(R.id.chipFilters);

        // Initially hide no stations message
        tvNoStations.setVisibility(View.GONE);
    }

    private void setupSearch() {
        etSearchStation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        for (int i = 0; i < chipFilters.getChildCount(); i++) {
            View child = chipFilters.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnClickListener(v -> {
                    currentFilter = chip.getText().toString();
                    applyFilter();
                });
            }
        }
    }

    private void applyFilter() {
        filteredStations.clear();

        switch (currentFilter) {
            case "Nearest":
                filteredStations.addAll(allStations);
                Collections.sort(filteredStations, (a, b) ->
                        Double.compare(a.getDistanceKm(), b.getDistanceKm()));
                break;

            case "Fast Charge":
                for (ChargingStation station : allStations) {
                    if (station.hasFastCharging()) {
                        filteredStations.add(station);
                    }
                }
                break;

            case "Backup Power":
                // Filter for stations with backup power if data available
                filteredStations.addAll(allStations);
                break;

            default: // "All"
                filteredStations.addAll(allStations);
                break;
        }

        displayStations(filteredStations);
    }

    private void filterStations(String searchText) {
        if (searchText.isEmpty()) {
            applyFilter();
            return;
        }

        List<ChargingStation> searchResults = new ArrayList<>();
        String searchLower = searchText.toLowerCase();

        for (ChargingStation station : filteredStations) {
            if (station.getTitle().toLowerCase().contains(searchLower) ||
                    station.getAddressLine1().toLowerCase().contains(searchLower) ||
                    station.getTown().toLowerCase().contains(searchLower)) {
                searchResults.add(station);
            }
        }

        displayStations(searchResults);
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private void getCurrentLocationAndLoadStations() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        fetchNearbyStations();
                    } else {
                        // Use default location (Port Elizabeth)
                        currentLat = -33.9608;
                        currentLng = 25.6022;
                        fetchNearbyStations();
                    }
                })
                .addOnFailureListener(e -> {
                    // Use default location
                    currentLat = -33.9608;
                    currentLng = 25.6022;
                    fetchNearbyStations();
                });
    }

    private void fetchNearbyStations() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenChargeMapService service = retrofit.create(OpenChargeMapService.class);

        // Search within 50km radius, max 50 results
        Call<List<ChargingStation>> call = service.getNearbyStations(
                currentLat,
                currentLng,
                50,  // distance in km
                50,  // max results
                OCM_API_KEY
        );

        call.enqueue(new Callback<List<ChargingStation>>() {
            @Override
            public void onResponse(Call<List<ChargingStation>> call, Response<List<ChargingStation>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    allStations = response.body();

                    // Calculate distances
                    for (ChargingStation station : allStations) {
                        station.calculateDistance(currentLat, currentLng);
                    }

                    // Sort by distance
                    Collections.sort(allStations, (a, b) ->
                            Double.compare(a.getDistanceKm(), b.getDistanceKm()));

                    filteredStations.addAll(allStations);
                    displayStations(filteredStations);
                } else {
                    showNoStationsMessage();
                }
            }

            @Override
            public void onFailure(Call<List<ChargingStation>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StationsActivity.this,
                        "Error loading stations: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showNoStationsMessage();
            }
        });
    }

    private void displayStations(List<ChargingStation> stations) {
        stationsContainer.removeAllViews();

        if (stations.isEmpty()) {
            showNoStationsMessage();
            return;
        }

        tvNoStations.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (ChargingStation station : stations) {
            View stationView = inflater.inflate(R.layout.station_item, stationsContainer, false);

            TextView tvName = stationView.findViewById(R.id.tvStationName);
            TextView tvDistance = stationView.findViewById(R.id.tvStationDistance);
            TextView tvAddress = stationView.findViewById(R.id.tvStationAddress);
            TextView tvDetails = stationView.findViewById(R.id.tvStationDetails);
            TextView tvPrice = stationView.findViewById(R.id.tvStationPrice);

            tvName.setText(station.getTitle());
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", station.getDistanceKm()));
            tvAddress.setText(station.getFullAddress());

            // Set details (number of connectors, types)
            String details = station.getConnectorDetails();
            tvDetails.setText(details);

            // Set price if available
            if (station.getUsageCost() != null && !station.getUsageCost().isEmpty()) {
                tvPrice.setText(station.getUsageCost());
                tvPrice.setVisibility(View.VISIBLE);
            } else {
                tvPrice.setVisibility(View.GONE);
            }

            // Set click listener
            stationView.setOnClickListener(v -> {
                Intent intent = new Intent(StationsActivity.this, HomePageActivity.class);
                intent.putExtra("show_directions", true);
                intent.putExtra("station_lat", station.getLatitude());
                intent.putExtra("station_lng", station.getLongitude());
                intent.putExtra("station_name", station.getTitle());
                intent.putExtra("station_address", station.getFullAddress());
                startActivity(intent);
                finish();
            });

            stationsContainer.addView(stationView);
        }
    }

    private void showNoStationsMessage() {
        tvNoStations.setVisibility(View.VISIBLE);
        stationsContainer.removeAllViews();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndLoadStations();
            } else {
                // Use default location
                currentLat = -33.9608;
                currentLng = 25.6022;
                fetchNearbyStations();
            }
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_stations);

        bottomNav.setOnItemSelectedListener((MenuItem item) -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                startActivity(new Intent(this, HomePageActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_stations) {
                return true;
            } else if (id == R.id.nav_load) {
                startActivity(new Intent(this, LoadsheddingActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    // Retrofit interface
    interface OpenChargeMapService {
        @GET("poi/")
        Call<List<ChargingStation>> getNearbyStations(
                @Query("latitude") double latitude,
                @Query("longitude") double longitude,
                @Query("distance") int distance,
                @Query("maxresults") int maxResults,
                @Query("key") String apiKey
        );

        @GET("poi/")
        Call<List<ChargingStation>> getNearbyStationsNoKey(
                @Query("latitude") double latitude,
                @Query("longitude") double longitude,
                @Query("distance") int distance,
                @Query("maxresults") int maxResults
        );
    }
}