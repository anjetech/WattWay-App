package com.example.wattway_app;

public class User {
    public String fullName;
    public String email;

    public User() {
        // Required for Firebase
    }

    public User(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }
}
