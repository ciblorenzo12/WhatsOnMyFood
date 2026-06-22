package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    private String appliedLanguageCode;

    @Override
    protected void attachBaseContext(Context newBase) {
        appliedLanguageCode = LanguageManager.getLanguageCode(newBase);
        super.attachBaseContext(LanguageManager.wrap(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLanguageCode = LanguageManager.getLanguageCode(this);
        if (appliedLanguageCode != null && !appliedLanguageCode.equals(currentLanguageCode)) {
            recreate();
        }
    }

    protected void navigateBack() {
        onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigateBack();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            navigateBack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
