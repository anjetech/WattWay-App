package com.example.wattway_app;

import android.os.Bundle;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public class LoadsheddingActivity extends AppCompatActivity {

    private static final String API_KEY = "B64EA798-9EE74965-A5AA3856-6471D5C5";
    private static final String BASE_URL = "https://developer.sepush.co.za/business/2.0/";

    private TextView tvSelectedArea, tvStageLabel, tvBlock, tvScheduleDate, tvActiveTime;
    private EditText etSearchArea;
    private LinearLayout scheduleContainer, searchResultsContainer;
    private CardView cardActiveLoadshedding, cardSelectedArea;
    private String currentAreaId = null;
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loadshedding);

        tvSelectedArea = findViewById(R.id.tvSelectedArea);
        tvStageLabel = findViewById(R.id.tvStageLabel);
        tvBlock = findViewById(R.id.tvBlock);
        tvScheduleDate = findViewById(R.id.tvScheduleDate);
        tvActiveTime = findViewById(R.id.tvActiveTime);
        etSearchArea = findViewById(R.id.etSearchArea);
        scheduleContainer = findViewById(R.id.scheduleContainer);
        searchResultsContainer = findViewById(R.id.searchResultsContainer);
        cardActiveLoadshedding = findViewById(R.id.cardActiveLoadshedding);
        cardSelectedArea = findViewById(R.id.cardSelectedArea);

        cardSelectedArea.setVisibility(View.GONE);
        cardActiveLoadshedding.setVisibility(View.GONE);
        scheduleContainer.setVisibility(View.GONE);
        tvScheduleDate.setVisibility(View.GONE);
        searchResultsContainer.setVisibility(View.GONE);

        setupBottomNav();
        setupSearch();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupSearch() {
        etSearchArea.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    searchResultsContainer.setVisibility(View.GONE);
                }
            }
        });

        etSearchArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                final String searchText = s.toString().trim();

                if (searchText.length() >= 2 && etSearchArea.hasFocus()) {
                    searchRunnable = new Runnable() {
                        @Override
                        public void run() {
                            searchArea(searchText);
                        }
                    };
                    searchHandler.postDelayed(searchRunnable, 500);
                } else {
                    searchResultsContainer.setVisibility(View.GONE);
                    searchResultsContainer.removeAllViews();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchArea(String searchText) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        EskomApiService apiService = retrofit.create(EskomApiService.class);

        apiService.searchAreas(API_KEY, searchText).enqueue(new Callback<AreaSearchResponse>() {
            @Override
            public void onResponse(Call<AreaSearchResponse> call, Response<AreaSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AreaSearchResponse searchResponse = response.body();
                    if (searchResponse.areas != null && !searchResponse.areas.isEmpty()) {
                        displaySearchResults(searchResponse.areas);
                    } else {
                        searchResultsContainer.setVisibility(View.GONE);
                    }
                } else {
                    searchResultsContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<AreaSearchResponse> call, Throwable t) {
                searchResultsContainer.setVisibility(View.GONE);
            }
        });
    }

    private void displaySearchResults(List<AreaResult> areas) {
        searchResultsContainer.removeAllViews();
        searchResultsContainer.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);

        int maxResults = Math.min(areas.size(), 10);

        for (int i = 0; i < maxResults; i++) {
            final AreaResult area = areas.get(i);
            View itemView = inflater.inflate(R.layout.search_result_item, searchResultsContainer, false);

            TextView tvAreaName = itemView.findViewById(R.id.tvAreaName);
            TextView tvAreaRegion = itemView.findViewById(R.id.tvAreaRegion);

            tvAreaName.setText(area.name);
            tvAreaRegion.setText(area.region);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentAreaId = area.id;

                    etSearchArea.clearFocus();
                    etSearchArea.setText(area.name);

                    searchResultsContainer.setVisibility(View.GONE);
                    searchResultsContainer.removeAllViews();

                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etSearchArea.getWindowToken(), 0);

                    fetchAreaSchedule();
                }
            });

            searchResultsContainer.addView(itemView);
        }
    }

    private void fetchCurrentStage() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        EskomApiService apiService = retrofit.create(EskomApiService.class);

        apiService.getStatus(API_KEY).enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StatusResponse status = response.body();
                    if (status.status != null && status.status.eskom != null) {
                        String stage = status.status.eskom.stage;
                        tvStageLabel.setText("Stage " + stage);
                    }
                }
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
            }
        });
    }

    private void fetchAreaSchedule() {
        if (currentAreaId == null) return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        EskomApiService apiService = retrofit.create(EskomApiService.class);

        apiService.getAreaSchedule(API_KEY, currentAreaId).enqueue(new Callback<AreaScheduleResponse>() {
            @Override
            public void onResponse(Call<AreaScheduleResponse> call, Response<AreaScheduleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AreaScheduleResponse schedule = response.body();

                    cardSelectedArea.setVisibility(View.VISIBLE);
                    scheduleContainer.setVisibility(View.VISIBLE);
                    tvScheduleDate.setVisibility(View.VISIBLE);

                    if (schedule.info != null) {
                        tvSelectedArea.setText(schedule.info.name);
                        tvBlock.setText(schedule.info.region);
                    }

                    fetchCurrentStage();

                    if (schedule.events != null && !schedule.events.isEmpty()) {
                        displaySchedule(schedule.events);
                    } else {
                        Toast.makeText(LoadsheddingActivity.this, "No schedule available", Toast.LENGTH_SHORT).show();
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMM", Locale.ENGLISH);
                    tvScheduleDate.setText("Today - " + sdf.format(new Date()));

                } else {
                    Toast.makeText(LoadsheddingActivity.this, "Failed to fetch schedule", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AreaScheduleResponse> call, Throwable t) {
                Toast.makeText(LoadsheddingActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displaySchedule(List<ScheduleEvent> events) {
        scheduleContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        Date now = new Date();
        boolean hasActiveEvent = false;

        for (ScheduleEvent event : events) {
            View itemView = inflater.inflate(R.layout.schedule_item, scheduleContainer, false);

            TextView tvTime = itemView.findViewById(R.id.tvScheduleTime);
            TextView tvStatus = itemView.findViewById(R.id.tvScheduleStatus);
            CardView cardView = itemView.findViewById(R.id.scheduleCard);

            String startTime = formatTime(event.start);
            String endTime = formatTime(event.end);
            tvTime.setText(startTime + " - " + endTime);

            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH);
                Date startDate = isoFormat.parse(event.start);
                Date endDate = isoFormat.parse(event.end);

                if (now.after(endDate)) {
                    tvStatus.setText("✓ Completed");
                    tvStatus.setTextColor(0xFF4CAF50);
                    cardView.setCardBackgroundColor(0xFFF5F5F5);
                } else if (now.after(startDate) && now.before(endDate)) {
                    tvStatus.setText("⚡ Currently Active");
                    tvStatus.setTextColor(0xFFFF9800);
                    cardView.setCardBackgroundColor(0xFFFFF9C4);

                    if (!hasActiveEvent) {
                        tvActiveTime.setText("Ends at " + endTime + " today");
                        cardActiveLoadshedding.setVisibility(View.VISIBLE);
                        hasActiveEvent = true;
                    }
                } else {
                    tvStatus.setText("Scheduled");
                    tvStatus.setTextColor(0xFF666666);
                    cardView.setCardBackgroundColor(0xFFFFFFFF);
                }
            } catch (Exception e) {
                tvStatus.setText("Scheduled");
                tvStatus.setTextColor(0xFF666666);
                cardView.setCardBackgroundColor(0xFFFFFFFF);
            }

            scheduleContainer.addView(itemView);
        }

        if (!hasActiveEvent) {
            cardActiveLoadshedding.setVisibility(View.GONE);
        }
    }

    private String formatTime(String isoTime) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH);
            SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            Date date = isoFormat.parse(isoTime);
            return displayFormat.format(date);
        } catch (Exception e) {
            return isoTime;
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_load);

        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_map) {
                    startActivity(new Intent(LoadsheddingActivity.this, HomePageActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (id == R.id.nav_stations) {
                    startActivity(new Intent(LoadsheddingActivity.this, StationsActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (id == R.id.nav_load) {
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(LoadsheddingActivity.this, ProfileActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    interface EskomApiService {
        @GET("status")
        Call<StatusResponse> getStatus(@Header("token") String token);

        @GET("area")
        Call<AreaScheduleResponse> getAreaSchedule(@Header("token") String token, @Query("id") String areaId);

        @GET("areas_search")
        Call<AreaSearchResponse> searchAreas(@Header("token") String token, @Query("text") String searchText);
    }

    static class StatusResponse {
        Status status;

        static class Status {
            Eskom eskom;

            static class Eskom {
                String stage;
            }
        }
    }

    static class AreaSearchResponse {
        List<AreaResult> areas;
    }

    static class AreaResult {
        String id;
        String name;
        String region;
    }

    static class AreaScheduleResponse {
        List<ScheduleEvent> events;
        AreaInfo info;

        static class AreaInfo {
            String name;
            String region;
        }
    }

    static class ScheduleEvent {
        @SerializedName("start")
        String start;

        @SerializedName("end")
        String end;

        @SerializedName("note")
        String note;
    }
}