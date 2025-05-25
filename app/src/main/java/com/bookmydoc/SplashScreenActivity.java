package com.bookmydoc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreenActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 5000; // 5 seconds
    private FirebaseAuth auth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        TextView appName = findViewById(R.id.app_name);
        appName.animate().alpha(1f).setDuration(1000).setStartDelay(500);

        new Handler().postDelayed(() -> {
//            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
//            boolean isFirstTime = prefs.getBoolean("first_time", true);

            if (user!=null) {
                startActivity(new Intent(this, MainActivity.class));
//                prefs.edit().putBoolean("first_time", false).apply(); // Set flag
            } else {
                startActivity(new Intent(this, OnBoardingActivity.class));
            }

            finish();
        }, SPLASH_DELAY);
    }
}