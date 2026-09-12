package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    private String appliedLanguageCode;

    private com.ciblorenzo.whatsonmyfood.ui.ResponsiveContentContainer responsiveShell;

    @Override
    public void setContentView(int layoutId) {
        if (this instanceof ScanBarcodeActivity || this instanceof PantryArInsightsActivity) {
            super.setContentView(layoutId);
            return;
        }
        responsiveShell = new com.ciblorenzo.whatsonmyfood.ui.ResponsiveContentContainer(this);
        if (this instanceof ProfileActivity) responsiveShell.setMaxWidthDp(720);
        if (this instanceof SignInActivity || this instanceof AddProductActivity || this instanceof SubscriptionActivity) {
            responsiveShell.setMaxWidthDp(640);
        }
        android.view.View root = getLayoutInflater().inflate(layoutId, responsiveShell.content(), false);
        responsiveShell.content().addView(root);
        responsiveShell.setFitsSystemWindows(true);
        super.setContentView(responsiveShell, new android.widget.FrameLayout.LayoutParams(
                -1, -1, android.view.Gravity.CENTER_HORIZONTAL));
    }

    @Override
    protected void onPostCreate(android.os.Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (responsiveShell != null && findViewById(R.id.bottom_navigation) == null) {
            com.ciblorenzo.whatsonmyfood.ui.AppNavigation.install(this, responsiveShell);
        }
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        com.ciblorenzo.whatsonmyfood.utils.GlassMotion.enter(findViewById(android.R.id.content));
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        ThemeManager.applySavedMode(newBase);
        appliedLanguageCode = LanguageManager.getLanguageCode(newBase);
        super.attachBaseContext(LanguageManager.wrap(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean light = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                != android.content.res.Configuration.UI_MODE_NIGHT_YES;
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(light);
        if (this instanceof ScanBarcodeActivity || this instanceof PantryArInsightsActivity) {
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
        }
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
