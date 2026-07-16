package com.ciblorenzo.whatsonmyfood;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public class NetworkUtils {

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        // An emulator can report Ethernet or another supported transport rather than Wi-Fi.
        // Internet capability is the meaningful condition for product lookups.
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
