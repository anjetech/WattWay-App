package com.example.wattway_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomePageActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String DIRECTIONS_API_KEY = "AIzaSyBEyv3xhDHI_jSU1yOIB_SJIaVOeM0tOpc";

    // Map and Location
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean mapCentered = false;
    private Location currentLocation;
    private Polyline currentRoute;

    // UI Elements
    private MaterialCardView searchCard;
    private BottomNavigationView bottomNav;
    private CardView directionsInfoCard;
    private FloatingActionButton fabMyLocation, fabDirections, fabStopNavigation;
    private TextView tvStationNameCard, tvStationAddressCard, tvDistanceCard, tvDurationCard;
    private Button btnStartDirections;
    private LinearLayout loadingContainer, directionsStepsContainer;
    private EditText etSearch;
    private View directionsBottomSheet;
    private TextView tvCurrentStep, tvNextStep, tvRemainingDistance, tvRemainingTime;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    // UI State
    private boolean uiElementsVisible = true;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable hideUiRunnable;

    // Station Navigation
    private Marker stationMarker;
    private boolean showDirectionsButton = false;
    private double stationLat, stationLng;
    private String stationName, stationAddress;

    // Navigation State
    private boolean isNavigating = false;
    private int currentStepIndex = 0;
    private final List<NavigationStep> navigationSteps = new ArrayList<>();

    // Navigation Step class - moved to top for visibility
    public static class NavigationStep {
        String instruction;
        String distance;
        String duration;
        double startLat, startLng;
        double endLat, endLng;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        initializeViews();
        setupLocationServices();
        setupMap();
        setupUI();
        checkIntentForStation();
    }

    private void initializeViews() {
        searchCard = findViewById(R.id.searchCard);
        bottomNav = findViewById(R.id.bottomNav);
        loadingContainer = findViewById(R.id.loadingContainer);
        directionsInfoCard = findViewById(R.id.directionsInfoCard);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        fabDirections = findViewById(R.id.fabDirections);
        fabStopNavigation = findViewById(R.id.fabStopNavigation);
        tvStationNameCard = findViewById(R.id.tvStationNameCard);
        tvStationAddressCard = findViewById(R.id.tvStationAddressCard);
        tvDistanceCard = findViewById(R.id.tvDistanceCard);
        tvDurationCard = findViewById(R.id.tvDurationCard);
        btnStartDirections = findViewById(R.id.btnStartDirections);
        etSearch = findViewById(R.id.etSearch);

        // Navigation bottom sheet views
        directionsBottomSheet = findViewById(R.id.directionsBottomSheet);
        tvCurrentStep = findViewById(R.id.tvCurrentStep);
        tvNextStep = findViewById(R.id.tvNextStep);
        tvRemainingDistance = findViewById(R.id.tvRemainingDistance);
        tvRemainingTime = findViewById(R.id.tvRemainingTime);
        directionsStepsContainer = findViewById(R.id.directionsStepsContainer);

        // Setup bottom sheet behavior
        if (directionsBottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(directionsBottomSheet);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetBehavior.setPeekHeight(200);
        }
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationCallback();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    private void setupUI() {
        setupBottomNav();
        setupSearchBar();
        setupDirectionsButton();
        setupFABs();
        resetUiTimer();
    }

    private void checkIntentForStation() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("show_directions", false)) {
            showDirectionsButton = true;
            stationLat = intent.getDoubleExtra("station_lat", 0);
            stationLng = intent.getDoubleExtra("station_lng", 0);
            stationName = intent.getStringExtra("station_name");
            stationAddress = intent.getStringExtra("station_address");
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLocation = location;

                    // Update navigation if active
                    if (isNavigating) {
                        updateNavigation(location);
                    }

                    if (mMap != null && !mapCentered) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                        mapCentered = true;

                        if (showDirectionsButton && stationLat != 0 && stationLng != 0) {
                            showStationOnMap();
                        }

                        fusedLocationClient.removeLocationUpdates(this);
                    }
                }
            }
        };
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);

        mMap.setOnMapClickListener(latLng -> toggleUiElements());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocationFeatures();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    private void enableLocationFeatures() {
        if (mMap == null) return;

        try {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
            startLocationUpdates();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setDurationMillis(5000)
                .setMaxUpdateAgeMillis(0)
                .build();

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(currentLocationRequest, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = location;
                        if (mMap != null && !mapCentered) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                            mapCentered = true;

                            if (showDirectionsButton && stationLat != 0 && stationLng != 0) {
                                showStationOnMap();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> getLastLocation());
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = location;
                        if (mMap != null && !mapCentered) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                            mapCentered = true;

                            if (showDirectionsButton && stationLat != 0 && stationLng != 0) {
                                showStationOnMap();
                            }
                        }
                    }
                });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void showStationOnMap() {
        if (mMap == null) return;

        LatLng stationLatLng = new LatLng(stationLat, stationLng);

        if (stationMarker != null) {
            stationMarker.remove();
        }

        stationMarker = mMap.addMarker(new MarkerOptions()
                .position(stationLatLng)
                .title(stationName != null ? stationName : "Charging Station")
                .snippet(stationAddress)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        if (stationMarker != null) {
            stationMarker.showInfoWindow();
        }

        updateDirectionsCard();

        if (currentLocation != null) {
            showBothLocationsOnMap(stationLatLng);
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(stationLatLng, 14f));
        }

        showDirectionsUI();
    }

    private void updateDirectionsCard() {
        if (tvStationNameCard != null) {
            tvStationNameCard.setText(stationName != null ? stationName : "Charging Station");
        }

        if (tvStationAddressCard != null) {
            tvStationAddressCard.setText(stationAddress != null ? stationAddress : "");
        }

        if (tvDistanceCard != null && currentLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                    stationLat, stationLng, results);
            float distanceKm = results[0] / 1000;
            tvDistanceCard.setText(String.format(Locale.getDefault(), "Distance: %.1f km", distanceKm));
        } else if (tvDistanceCard != null) {
            tvDistanceCard.setText(R.string.distance_placeholder);
        }
    }

    private void showBothLocationsOnMap(LatLng stationLatLng) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        builder.include(stationLatLng);

        try {
            LatLngBounds bounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));
        } catch (Exception e) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(stationLatLng, 14f));
        }
    }

    private void showDirectionsUI() {
        if (directionsInfoCard != null) {
            directionsInfoCard.setVisibility(View.VISIBLE);
            directionsInfoCard.setAlpha(0f);
            directionsInfoCard.setTranslationY(100);
            directionsInfoCard.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(300)
                    .start();
        }

        if (fabMyLocation != null) {
            fabMyLocation.setVisibility(View.VISIBLE);
        }

        if (fabDirections != null) {
            fabDirections.setVisibility(View.VISIBLE);
        }
    }

    private void setupFABs() {
        if (fabMyLocation != null) {
            fabMyLocation.setOnClickListener(v -> {
                if (currentLocation != null) {
                    LatLng myLoc = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLoc, 15f));
                }
            });
        }

        if (fabDirections != null) {
            fabDirections.setOnClickListener(v -> startInAppNavigation());
        }

        if (fabStopNavigation != null) {
            fabStopNavigation.setOnClickListener(v -> stopNavigation());
            fabStopNavigation.setVisibility(View.GONE);
        }
    }

    private void setupDirectionsButton() {
        if (btnStartDirections != null) {
            btnStartDirections.setText(R.string.start_navigation);
            btnStartDirections.setOnClickListener(v -> startInAppNavigation());
        }
    }

    private void startInAppNavigation() {
        if (currentLocation == null) {
            Toast.makeText(this, R.string.getting_location, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        if (loadingContainer != null) {
            loadingContainer.setVisibility(View.VISIBLE);
        }

        // Fetch directions from Google Directions API
        fetchDirections();
    }

    private void fetchDirections() {
        new Thread(() -> {
            try {
                String origin = currentLocation.getLatitude() + "," + currentLocation.getLongitude();
                String destination = stationLat + "," + stationLng;

                String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=" + origin +
                        "&destination=" + destination +
                        "&mode=driving" +
                        "&key=" + DIRECTIONS_API_KEY;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                runOnUiThread(() -> {
                    processDirectionsResponse(jsonResponse);
                    if (loadingContainer != null) {
                        loadingContainer.setVisibility(View.GONE);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (loadingContainer != null) {
                        loadingContainer.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, getString(R.string.directions_error) + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void processDirectionsResponse(JSONObject response) {
        try {
            JSONArray routes = response.getJSONArray("routes");
            if (routes.length() == 0) {
                Toast.makeText(this, R.string.no_routes_found, Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject route = routes.getJSONObject(0);
            JSONArray legs = route.getJSONArray("legs");
            JSONObject leg = legs.getJSONObject(0);

            // Get duration and distance
            String duration = leg.getJSONObject("duration").getString("text");
            String distance = leg.getJSONObject("distance").getString("text");

            if (tvDurationCard != null) {
                tvDurationCard.setText(getString(R.string.duration_format, duration));
            }
            if (tvDistanceCard != null) {
                tvDistanceCard.setText(getString(R.string.distance_format, distance));
            }

            // Parse steps
            navigationSteps.clear();
            JSONArray steps = leg.getJSONArray("steps");

            for (int i = 0; i < steps.length(); i++) {
                JSONObject step = steps.getJSONObject(i);
                NavigationStep navStep = new NavigationStep();
                navStep.instruction = step.getString("html_instructions")
                        .replaceAll("<[^>]*>", ""); // Remove HTML tags
                navStep.distance = step.getJSONObject("distance").getString("text");
                navStep.duration = step.getJSONObject("duration").getString("text");

                JSONObject startLoc = step.getJSONObject("start_location");
                navStep.startLat = startLoc.getDouble("lat");
                navStep.startLng = startLoc.getDouble("lng");

                JSONObject endLoc = step.getJSONObject("end_location");
                navStep.endLat = endLoc.getDouble("lat");
                navStep.endLng = endLoc.getDouble("lng");

                navigationSteps.add(navStep);
            }

            // Draw route on map
            drawRoute(route);

            // Start navigation UI
            startNavigationUI();

        } catch (Exception e) {
            Toast.makeText(this, R.string.processing_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void drawRoute(JSONObject route) {
        try {
            // Clear existing route
            if (currentRoute != null) {
                currentRoute.remove();
            }

            // Decode polyline
            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
            String encodedPolyline = overviewPolyline.getString("points");
            List<LatLng> decodedPath = decodePolyline(encodedPolyline);

            // Draw new route
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(decodedPath)
                    .width(12)
                    .color(Color.parseColor("#2196F3"))
                    .geodesic(true);

            currentRoute = mMap.addPolyline(polylineOptions);

            // Zoom to show entire route
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : decodedPath) {
                builder.include(point);
            }
            LatLngBounds bounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void startNavigationUI() {
        isNavigating = true;
        currentStepIndex = 0;

        // Update UI buttons
        if (btnStartDirections != null) {
            btnStartDirections.setText(R.string.stop_navigation);
            btnStartDirections.setOnClickListener(v -> stopNavigation());
        }

        // Show navigation bottom sheet
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        // Show stop navigation FAB
        if (fabStopNavigation != null) {
            fabStopNavigation.setVisibility(View.VISIBLE);
        }

        // Hide directions info card
        if (directionsInfoCard != null) {
            directionsInfoCard.setVisibility(View.GONE);
        }

        // Display first step
        updateNavigationDisplay();

        // Show all steps in the container
        displayAllSteps();

        // Start location updates for navigation
        startNavigationLocationUpdates();

        Toast.makeText(this, R.string.navigation_started, Toast.LENGTH_SHORT).show();
    }

    private void displayAllSteps() {
        if (directionsStepsContainer == null) return;

        directionsStepsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < navigationSteps.size(); i++) {
            NavigationStep step = navigationSteps.get(i);
            View stepView = inflater.inflate(android.R.layout.simple_list_item_2, directionsStepsContainer, false);

            TextView text1 = stepView.findViewById(android.R.id.text1);
            TextView text2 = stepView.findViewById(android.R.id.text2);

            text1.setText(String.format(Locale.getDefault(), "%d. %s", (i + 1), step.instruction));
            text2.setText(String.format(Locale.getDefault(), "%s • %s", step.distance, step.duration));

            if (i == currentStepIndex) {
                stepView.setBackgroundColor(Color.parseColor("#E3F2FD"));
            }

            directionsStepsContainer.addView(stepView);
        }
    }

    private void updateNavigationDisplay() {
        if (currentStepIndex >= navigationSteps.size()) {
            // Reached destination
            arriveAtDestination();
            return;
        }

        NavigationStep currentStep = navigationSteps.get(currentStepIndex);

        if (tvCurrentStep != null) {
            tvCurrentStep.setText(currentStep.instruction);
        }

        if (tvRemainingDistance != null) {
            tvRemainingDistance.setText(currentStep.distance);
        }

        if (tvRemainingTime != null) {
            tvRemainingTime.setText(currentStep.duration);
        }

        if (tvNextStep != null && currentStepIndex < navigationSteps.size() - 1) {
            NavigationStep nextStep = navigationSteps.get(currentStepIndex + 1);
            tvNextStep.setText(getString(R.string.then_format, nextStep.instruction));
            tvNextStep.setVisibility(View.VISIBLE);
        } else if (tvNextStep != null) {
            tvNextStep.setVisibility(View.GONE);
        }

        // Update the steps list to highlight current step
        displayAllSteps();
    }

    private void updateNavigation(Location location) {
        if (!isNavigating || navigationSteps.isEmpty()) return;

        NavigationStep currentStep = navigationSteps.get(currentStepIndex);

        // Check distance to current step's end point
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                currentStep.endLat, currentStep.endLng, results);

        // If within 20 meters of the step's end point, move to next step
        if (results[0] < 20) {
            currentStepIndex++;
            updateNavigationDisplay();

            // Announce the new instruction
            if (currentStepIndex < navigationSteps.size()) {
                Toast.makeText(this, navigationSteps.get(currentStepIndex).instruction,
                        Toast.LENGTH_LONG).show();
            }
        }

        // Update camera to follow user
        if (mMap != null) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
        }
    }

    private void startNavigationLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void arriveAtDestination() {
        Toast.makeText(this, R.string.destination_arrived, Toast.LENGTH_LONG).show();
        stopNavigation();
    }

    private void stopNavigation() {
        isNavigating = false;
        currentStepIndex = 0;

        // Clear route from map
        if (currentRoute != null) {
            currentRoute.remove();
            currentRoute = null;
        }

        // Reset UI
        if (btnStartDirections != null) {
            btnStartDirections.setText(R.string.start_navigation);
            btnStartDirections.setOnClickListener(v -> startInAppNavigation());
        }

        // Hide navigation UI
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        if (fabStopNavigation != null) {
            fabStopNavigation.setVisibility(View.GONE);
        }

        // Show directions info card again
        if (directionsInfoCard != null) {
            directionsInfoCard.setVisibility(View.VISIBLE);
        }

        // Stop frequent location updates
        fusedLocationClient.removeLocationUpdates(locationCallback);

        // Restart normal location updates
        startLocationUpdates();

        Toast.makeText(this, R.string.navigation_stopped, Toast.LENGTH_SHORT).show();
    }

    private void toggleUiElements() {
        if (uiElementsVisible) {
            hideUiElements();
        } else {
            showUiElements();
        }
    }

    private void hideUiElements() {
        uiElementsVisible = false;

        if (searchCard != null) {
            searchCard.animate()
                    .translationY(-searchCard.getHeight())
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> searchCard.setVisibility(View.GONE))
                    .start();
        }

        if (bottomNav != null && !isNavigating) {
            bottomNav.animate()
                    .translationY(bottomNav.getHeight())
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> bottomNav.setVisibility(View.GONE))
                    .start();
        }
    }

    private void showUiElements() {
        uiElementsVisible = true;

        if (searchCard != null) {
            searchCard.setVisibility(View.VISIBLE);
            searchCard.setAlpha(0f);
            searchCard.setTranslationY(-searchCard.getHeight());
            searchCard.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }

        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
            bottomNav.setAlpha(0f);
            bottomNav.setTranslationY(bottomNav.getHeight());
            bottomNav.animate()
                    .translationY(0)
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }

        resetUiTimer();
    }

    private void resetUiTimer() {
        if (hideUiRunnable != null) {
            uiHandler.removeCallbacks(hideUiRunnable);
        }

        hideUiRunnable = () -> {
            if (uiElementsVisible && !isNavigating) {
                hideUiElements();
            }
        };
        uiHandler.postDelayed(hideUiRunnable, 5000);
    }

    private void setupBottomNav() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
            bottomNav.setOnItemSelectedListener(item -> {
                resetUiTimer();

                int id = item.getItemId();
                if (id == R.id.nav_map) {
                    return true;
                } else if (id == R.id.nav_stations) {
                    startActivity(new Intent(this, StationsActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_load) {
                    startActivity(new Intent(this, LoadsheddingActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            });
        }
    }

    private void setupSearchBar() {
        if (etSearch != null) {
            etSearch.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    resetUiTimer();
                }
            });

            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String query = etSearch.getText().toString().trim();
                    if (!query.isEmpty()) {
                        searchLocation(query);
                    }
                    resetUiTimer();
                    return true;
                }
                return false;
            });
        }
    }

    private void searchLocation(String locationName) {
        if (mMap == null) return;

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng searchLatLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchLatLng, 12f));
            } else {
                Toast.makeText(this, R.string.location_not_found, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.search_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationFeatures();
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mapCentered && mMap != null) {
            getCurrentLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isNavigating && fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hideUiRunnable != null) {
            uiHandler.removeCallbacks(hideUiRunnable);
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}