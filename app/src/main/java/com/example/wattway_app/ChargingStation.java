package com.example.wattway_app;

import android.location.Location;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChargingStation {

    @SerializedName("ID")
    private int id;

    @SerializedName("OperatorInfo")
    private OperatorInfo operatorInfo;

    @SerializedName("StatusType")
    private StatusType statusType;

    @SerializedName("AddressInfo")
    private AddressInfo addressInfo;

    @SerializedName("Connections")
    private List<Connection> connections;

    @SerializedName("UsageCost")
    private String usageCost;

    @SerializedName("NumberOfPoints")
    private int numberOfPoints;

    private double distanceKm = 0.0;

    // Inner classes for nested JSON structure
    public static class OperatorInfo {
        @SerializedName("Title")
        private String title;

        public String getTitle() {
            return title != null ? title : "Unknown Operator";
        }
    }

    public static class StatusType {
        @SerializedName("Title")
        private String title;

        @SerializedName("IsOperational")
        private boolean isOperational;

        public String getTitle() {
            return title != null ? title : "Unknown";
        }

        public boolean isOperational() {
            return isOperational;
        }
    }

    public static class AddressInfo {
        @SerializedName("Title")
        private String title;

        @SerializedName("AddressLine1")
        private String addressLine1;

        @SerializedName("AddressLine2")
        private String addressLine2;

        @SerializedName("Town")
        private String town;

        @SerializedName("StateOrProvince")
        private String stateOrProvince;

        @SerializedName("Postcode")
        private String postcode;

        @SerializedName("Country")
        private Country country;

        @SerializedName("Latitude")
        private double latitude;

        @SerializedName("Longitude")
        private double longitude;

        @SerializedName("ContactTelephone1")
        private String contactTelephone;

        public static class Country {
            @SerializedName("Title")
            private String title;

            public String getTitle() {
                return title != null ? title : "";
            }
        }

        public String getTitle() {
            return title != null ? title : "Charging Station";
        }

        public String getAddressLine1() {
            return addressLine1 != null ? addressLine1 : "";
        }

        public String getTown() {
            return town != null ? town : "";
        }

        public String getFullAddress() {
            StringBuilder address = new StringBuilder();
            if (addressLine1 != null && !addressLine1.isEmpty()) {
                address.append(addressLine1);
            }
            if (town != null && !town.isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(town);
            }
            if (stateOrProvince != null && !stateOrProvince.isEmpty()) {
                if (address.length() > 0) address.append(", ");
                address.append(stateOrProvince);
            }
            return address.toString();
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    public static class Connection {
        @SerializedName("ConnectionType")
        private ConnectionType connectionType;

        @SerializedName("StatusType")
        private StatusType statusType;

        @SerializedName("Level")
        private Level level;

        @SerializedName("PowerKW")
        private Double powerKW;

        @SerializedName("CurrentType")
        private CurrentType currentType;

        @SerializedName("Quantity")
        private Integer quantity;

        public static class ConnectionType {
            @SerializedName("Title")
            private String title;

            @SerializedName("FormalName")
            private String formalName;

            public String getTitle() {
                return title != null ? title : "Unknown";
            }
        }

        public static class Level {
            @SerializedName("Title")
            private String title;

            @SerializedName("Comments")
            private String comments;

            @SerializedName("IsFastChargeCapable")
            private boolean isFastChargeCapable;

            public String getTitle() {
                return title != null ? title : "";
            }

            public boolean isFastChargeCapable() {
                return isFastChargeCapable;
            }
        }

        public static class CurrentType {
            @SerializedName("Title")
            private String title;

            public String getTitle() {
                return title != null ? title : "";
            }
        }

        public ConnectionType getConnectionType() {
            return connectionType;
        }

        public Level getLevel() {
            return level;
        }

        public Double getPowerKW() {
            return powerKW;
        }

        public Integer getQuantity() {
            return quantity != null ? quantity : 1;
        }
    }

    // Getters for main class
    public String getTitle() {
        if (addressInfo != null) {
            return addressInfo.getTitle();
        }
        return "Charging Station";
    }

    public String getAddressLine1() {
        if (addressInfo != null) {
            return addressInfo.getAddressLine1();
        }
        return "";
    }

    public String getTown() {
        if (addressInfo != null) {
            return addressInfo.getTown();
        }
        return "";
    }

    public String getFullAddress() {
        if (addressInfo != null) {
            return addressInfo.getFullAddress();
        }
        return "Address not available";
    }

    public double getLatitude() {
        if (addressInfo != null) {
            return addressInfo.getLatitude();
        }
        return 0.0;
    }

    public double getLongitude() {
        if (addressInfo != null) {
            return addressInfo.getLongitude();
        }
        return 0.0;
    }

    public String getUsageCost() {
        return usageCost;
    }

    public String getOperatorTitle() {
        if (operatorInfo != null) {
            return operatorInfo.getTitle();
        }
        return "Unknown Operator";
    }

    public boolean isOperational() {
        if (statusType != null) {
            return statusType.isOperational();
        }
        return false;
    }

    public boolean hasFastCharging() {
        if (connections != null) {
            for (Connection conn : connections) {
                if (conn.getLevel() != null && conn.getLevel().isFastChargeCapable()) {
                    return true;
                }
                // Also check by power - anything above 50kW is considered fast
                if (conn.getPowerKW() != null && conn.getPowerKW() >= 50) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getConnectorDetails() {
        if (connections == null || connections.isEmpty()) {
            return "No connector information";
        }

        StringBuilder details = new StringBuilder();
        int totalConnectors = 0;

        for (Connection conn : connections) {
            totalConnectors += conn.getQuantity();
        }

        details.append(totalConnectors).append(" connector");
        if (totalConnectors != 1) details.append("s");

        // Add connector types
        details.append(" • ");
        boolean first = true;
        for (Connection conn : connections) {
            if (!first) details.append(", ");
            if (conn.getConnectionType() != null) {
                details.append(conn.getConnectionType().getTitle());
                if (conn.getPowerKW() != null) {
                    details.append(" (").append(conn.getPowerKW()).append("kW)");
                }
            }
            first = false;
        }

        // Add operator info
        details.append(" • ").append(getOperatorTitle());

        return details.toString();
    }

    public void calculateDistance(double userLat, double userLng) {
        float[] results = new float[1];
        Location.distanceBetween(userLat, userLng, getLatitude(), getLongitude(), results);
        this.distanceKm = results[0] / 1000.0;
    }

    public double getDistanceKm() {
        return distanceKm;
    }
}