package com.example.wattway_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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

    private static final String OCM_API_KEY = "5697ac5d-7169-4f6c-ab6c-047272aa2c34";
    private static final String BASE_URL = "https://api.openchargemap.io/v3/";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int NEARBY_RADIUS_KM = 50;
    private static final int SEARCH_RADIUS_KM = 500;

    private LinearLayout stationsContainer;
    private ProgressBar progressBar;
    private TextView tvNoStations;
    private EditText etSearchStation;
    private ChipGroup chipFilters;

    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = -33.9608;
    private double currentLng = 25.6022;
    private List<ChargingStation> allStations = new ArrayList<>();
    private List<ChargingStation> filteredStations = new ArrayList<>();
    private String currentFilter = "All";
    private boolean isSearchMode = false;

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
        } else {
            fetchNearbyStations(NEARBY_RADIUS_KM);
        }
    }

    private void initializeViews() {
        stationsContainer = findViewById(R.id.stationsContainer);
        progressBar = findViewById(R.id.progressBar);
        tvNoStations = findViewById(R.id.tvNoStations);
        etSearchStation = findViewById(R.id.etSearchStation);
        chipFilters = findViewById(R.id.chipFilters);

        tvNoStations.setVisibility(View.GONE);
    }

    private void setupSearch() {
        etSearchStation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                String query = etSearchStation.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchLocationAndFetchStations(query);
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etSearchStation.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        etSearchStation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().trim();
                if (searchText.isEmpty()) {
                    isSearchMode = false;
                    if (currentFilter.equals("Nearest")) {
                        getCurrentLocationAndLoadStations();
                    } else {
                        fetchNearbyStations(NEARBY_RADIUS_KM);
                    }
                } else if (searchText.length() >= 2) {
                    isSearchMode = true;
                    filterStations(searchText);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchLocationAndFetchStations(String locationName) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
                List<android.location.Address> addresses = geocoder.getFromLocationName(locationName, 5);

                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address bestAddress = null;
                    for (android.location.Address address : addresses) {
                        if (address.getCountryCode() != null &&
                                address.getCountryCode().equalsIgnoreCase("ZA")) {
                            bestAddress = address;
                            break;
                        }
                    }

                    if (bestAddress == null) {
                        bestAddress = addresses.get(0);
                    }

                    final double searchLat = bestAddress.getLatitude();
                    final double searchLng = bestAddress.getLongitude();
                    final String foundLocation = bestAddress.getLocality() != null ?
                            bestAddress.getLocality() : bestAddress.getFeatureName();

                    runOnUiThread(() -> {
                        currentLat = searchLat;
                        currentLng = searchLng;
                        isSearchMode = true;

                        Toast.makeText(this,
                                "Searching near " + foundLocation,
                                Toast.LENGTH_SHORT).show();

                        fetchNearbyStations(SEARCH_RADIUS_KM);
                    });
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this,
                                "Location not found. Try: Johannesburg, Cape Town, Durban, etc.",
                                Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Search error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setupFilters() {
        for (int i = 0; i < chipFilters.getChildCount(); i++) {
            View child = chipFilters.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnClickListener(v -> {
                    currentFilter = chip.getText().toString();

                    etSearchStation.setText("");
                    isSearchMode = false;

                    if (currentFilter.equals("Nearest")) {
                        getCurrentLocationAndLoadStations();
                    } else {
                        fetchNearbyStations(SEARCH_RADIUS_KM);
                    }
                });
            }
        }
    }

    private void applyFilter() {
        filteredStations.clear();

        if (currentFilter.equals("Nearest")) {
            filteredStations.addAll(allStations);
            Collections.sort(filteredStations, (a, b) ->
                    Double.compare(a.getDistanceKm(), b.getDistanceKm()));
        } else {
            filteredStations.addAll(allStations);
        }

        displayStations(filteredStations);
    }

    private void filterStations(String searchText) {
        if (searchText.isEmpty()) {
            applyFilter();
            return;
        }

        if (isSearchMode && allStations.size() < 100) {
            fetchNearbyStations(SEARCH_RADIUS_KM);
        }

        List<ChargingStation> searchResults = new ArrayList<>();
        String searchLower = searchText.toLowerCase();

        for (ChargingStation station : allStations) {
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
                        android.util.Log.d("StationsActivity", "User location: " + currentLat + ", " + currentLng);
                    } else {
                        currentLat = -33.9608;
                        currentLng = 25.6022;
                        android.util.Log.d("StationsActivity", "Using default location");
                    }
                    isSearchMode = false;
                    fetchNearbyStations(NEARBY_RADIUS_KM);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("StationsActivity", "Failed to get location", e);
                    currentLat = -33.9608;
                    currentLng = 25.6022;
                    isSearchMode = false;
                    fetchNearbyStations(NEARBY_RADIUS_KM);
                });
    }

    private void fetchNearbyStations(int radiusKm) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OpenChargeMapService service = retrofit.create(OpenChargeMapService.class);

        int maxResults = isSearchMode ? 200 : 100;

        Call<List<ChargingStation>> call = service.getNearbyStations(
                currentLat,
                currentLng,
                radiusKm,
                maxResults,
                OCM_API_KEY
        );

        call.enqueue(new Callback<List<ChargingStation>>() {
            @Override
            public void onResponse(Call<List<ChargingStation>> call, Response<List<ChargingStation>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    allStations = response.body();

                    for (ChargingStation station : allStations) {
                        station.calculateDistance(currentLat, currentLng);
                    }

                    Collections.sort(allStations, (a, b) ->
                            Double.compare(a.getDistanceKm(), b.getDistanceKm()));

                    filteredStations.clear();
                    filteredStations.addAll(allStations);

                    if (isSearchMode) {
                        filterStations(etSearchStation.getText().toString());
                    } else {
                        displayStations(filteredStations);
                    }
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
            tvName.setTextColor(0xFF000000);

            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", station.getDistanceKm()));
            tvDistance.setTextColor(0xFFE53935);

            tvAddress.setText(station.getFullAddress());
            tvAddress.setTextColor(0xFF757575);

            String details = station.getConnectorDetails();
            tvDetails.setText(details);
            tvDetails.setTextColor(0xFF9E9E9E);

            if (station.getUsageCost() != null && !station.getUsageCost().isEmpty()) {
                tvPrice.setText(station.getUsageCost());
                tvPrice.setTextColor(0xFF4CAF50);
                tvPrice.setVisibility(View.VISIBLE);
            } else {
                tvPrice.setVisibility(View.GONE);
            }

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
                fetchNearbyStations(NEARBY_RADIUS_KM);
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

    interface OpenChargeMapService {
        @GET("poi/")
        Call<List<ChargingStation>> getNearbyStations(
                @Query("latitude") double latitude,
                @Query("longitude") double longitude,
                @Query("distance") int distance,
                @Query("maxresults") int maxResults,
                @Query("key") String apiKey
        );
    }
}