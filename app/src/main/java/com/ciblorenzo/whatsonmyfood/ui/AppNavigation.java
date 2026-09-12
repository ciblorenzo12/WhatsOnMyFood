package com.ciblorenzo.whatsonmyfood.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import androidx.appcompat.content.res.AppCompatResources;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.ciblorenzo.whatsonmyfood.*;

public final class AppNavigation {
    private AppNavigation() {}
    public static void install(Activity activity, ResponsiveContentContainer shell) {
        int selected;
        if (activity instanceof MainActivity) selected = R.id.navigation_home;
        else if (activity instanceof PantryActivity) selected = R.id.navigation_pantry;
        else if (activity instanceof AdditiveDatabaseActivity) selected = R.id.navigation_database;
        else if (activity instanceof ProfileActivity) selected = R.id.navigation_profile;
        else if (activity instanceof ProductDetailsActivity) selected = R.id.navigation_scan;
        else return;
        boolean rail = activity.getResources().getConfiguration().screenWidthDp >= 840;
        NavigationBarView nav = rail ? new NavigationRailView(activity) : new BottomNavigationView(activity);
        nav.setId(R.id.bottom_navigation);
        nav.inflateMenu(R.menu.bottom_nav_menu);
        nav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        nav.setItemIconTintList(AppCompatResources.getColorStateList(activity, R.color.navigation_item_color));
        nav.setItemTextColor(AppCompatResources.getColorStateList(activity, R.color.navigation_item_color));
        nav.setItemActiveIndicatorColor(AppCompatResources.getColorStateList(activity, R.color.ui_selected));
        nav.setBackgroundColor(activity.getColor(R.color.surface));
        nav.getMenu().findItem(selected).setChecked(true);
        View scan = nav.findViewById(R.id.navigation_scan);
        if (scan != null) {
            scan.setContentDescription(activity.getString(R.string.ui_scan_product));
            scan.setBackgroundResource(R.drawable.ui_scan_navigation);
        }
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selected && id != R.id.navigation_scan) return true;
            Class<?> target = id == R.id.navigation_home ? MainActivity.class
                    : id == R.id.navigation_pantry ? PantryActivity.class
                    : id == R.id.navigation_database ? AdditiveDatabaseActivity.class
                    : id == R.id.navigation_profile ? ProfileActivity.class : ScanBarcodeActivity.class;
            activity.startActivity(new Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            return false; // The current screen remains selected when returning from another Activity.
        });
        shell.setNavigation(nav, rail);
    }
}
