package com.bookmydoc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bookmydoc.adapter.OnboardingAdapter;
import com.bookmydoc.manager.DepthPageTransformer;
import com.bookmydoc.model.OnboardingModel;

import java.util.ArrayList;
import java.util.List;

public class OnBoardingActivity extends AppCompatActivity {

    ViewPager2 viewPager;
    OnboardingAdapter adapter;
    List<OnboardingModel> slideList;
    LinearLayout dotsLayout;
    TextView[] dots;
    private static final int PERMISSION_REQUEST_CODE = 100;

    Button nextBtn;
    int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        nextBtn = findViewById(R.id.nextBtn);

        slideList = new ArrayList<>();
        slideList.add(new OnboardingModel(R.drawable.book_appoinment_b, "Book Appointment", "Schedule doctor visits easily from your phone."));
        slideList.add(new OnboardingModel(R.drawable.doctor_con_b, "Virtual Consultation", "Talk to certified doctors from anywhere."));
        slideList.add(new OnboardingModel(R.drawable.delivery_b, "Medicine Delivery", "Get your prescribed medicines delivered at your door."));
        slideList.add(new OnboardingModel(R.drawable.get_started_b, "Get Started", "Let’s get started with your healthcare journey."));

        adapter = new OnboardingAdapter(slideList);
        viewPager.setAdapter(adapter);
        viewPager.setPageTransformer(new DepthPageTransformer());

        addDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                addDots(position);
                currentIndex = position;
                nextBtn.setText(position == slideList.size() - 1 ? "Get Started" : "Next");
            }
        });

        nextBtn.setOnClickListener(v -> {
            if (currentIndex < slideList.size() - 1) {
                viewPager.setCurrentItem(currentIndex + 1);
            } else {
//                if (checkPermission())
                    openLogin();
//                else requestPermission();
            }
        });
    }

    private void openLogin() {
        startActivity(new Intent(this, PhoneLoginActivity.class));
        finish();
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSION_REQUEST_CODE);
    }

    private void addDots(int position) {
        dots = new TextView[slideList.size()];
        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText("•");
            dots[i].setTextSize(35);
            dots[i].setTextColor(i == position ? getColor(R.color.primary) : getColor(android.R.color.darker_gray));
            dotsLayout.addView(dots[i]);
        }
    }
}
