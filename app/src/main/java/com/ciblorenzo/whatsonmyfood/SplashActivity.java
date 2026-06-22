package com.ciblorenzo.whatsonmyfood;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Simple entrance animation
        findViewById(R.id.splash_logo).animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        GlassMotion.enter(findViewById(R.id.splash_title), 400L);
        GlassMotion.enter(findViewById(R.id.splash_subtitle), 600L);

        // Delay for splash and then navigate
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, SignInActivity.class));
            }
            finish();
        }, 2200); // 2.2 seconds display time
    }
}
