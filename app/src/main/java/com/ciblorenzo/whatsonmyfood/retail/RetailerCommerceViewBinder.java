package com.ciblorenzo.whatsonmyfood.retail;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciblorenzo.whatsonmyfood.ProductRepository;
import com.ciblorenzo.whatsonmyfood.ProductWithDetails;
import com.ciblorenzo.whatsonmyfood.R;
import com.ciblorenzo.whatsonmyfood.utils.GlassMotion;
import com.ciblorenzo.whatsonmyfood.utils.LocationHelper;

import java.util.List;

public class RetailerCommerceViewBinder {

    public interface Host {
        void runOnUiThread(Runnable runnable);
        boolean isActive();
    }

    private final Context context;
    private final RetailerRepository repository;
    private final Host host;
    private final View commerceLayout;
    private final Button comparisonButton;
    private final TextView comparisonUnavailableText;
    private final View whereToBuySection;
    private final View alternativesSection;
    private final TextView whereToBuyEmptyText;
    private final TextView alternativesEmptyText;
    private final RetailerAvailabilityMapView availabilityMapView;
    private final RetailerAvailabilityAdapter availabilityAdapter;
    private final RetailerAlternativeAdapter alternativeAdapter;

    private ProductWithDetails currentProduct;
    private List<RetailerAlternative> cachedAlternatives;
    private String currentZipCode;
    private double currentLat, currentLng;
    private final LocationHelper locationHelper;

    public RetailerCommerceViewBinder(Context context, View root, RetailerRepository repository, Host host) {
        this.context = context;
        this.repository = repository;
        this.host = host;
        this.locationHelper = new LocationHelper(context);
        
        commerceLayout = root.findViewById(R.id.retailer_commerce_layout);
        comparisonButton = root.findViewById(R.id.comparison_view_button);
        comparisonUnavailableText = root.findViewById(R.id.comparison_unavailable_text);

        RecyclerView whereToBuyRecyclerView = root.findViewById(R.id.where_to_buy_recycler_view);
        RecyclerView alternativesRecyclerView = root.findViewById(R.id.better_alternatives_recycler_view);
        whereToBuySection = root.findViewById(R.id.where_to_buy_section);
        alternativesSection = root.findViewById(R.id.better_alternatives_section);
        whereToBuyEmptyText = root.findViewById(R.id.where_to_buy_empty_text);
        alternativesEmptyText = root.findViewById(R.id.better_alternatives_empty_text);
        availabilityMapView = root.findViewById(R.id.where_to_buy_map_view);

        availabilityAdapter = new RetailerAvailabilityAdapter(context);
        alternativeAdapter = new RetailerAlternativeAdapter(context);

        whereToBuyRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        whereToBuyRecyclerView.setAdapter(availabilityAdapter);
        alternativesRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        alternativesRecyclerView.setAdapter(alternativeAdapter);

        setSectionVisible(commerceLayout, false);
    }

    public void bind(ProductWithDetails productDetails) {
        currentProduct = productDetails;
        cachedAlternatives = null;
        availabilityAdapter.submitList(null);
        if (availabilityMapView != null) {
            availabilityMapView.submitList(null);
        }
        alternativeAdapter.submitList(null);
        setSectionVisible(whereToBuySection, false);
        setSectionVisible(alternativesSection, false);
        setMessage(whereToBuyEmptyText, "");
        setMessage(alternativesEmptyText, "");
        setSectionVisible(commerceLayout, currentProduct != null && currentProduct.product != null);
        boolean comparisonAvailable = MarketplaceNavigation.isAvailable(currentProduct);
        MarketplaceNavigation.bindAction(
                context,
                comparisonButton,
                comparisonUnavailableText,
                currentProduct
        );
        if (comparisonAvailable) {
            refreshLocationAndLoad();
        }
    }

    private void refreshLocationAndLoad() {
        locationHelper.getLastLocation(new LocationHelper.LocationListener() {
            @Override
            public void onLocationFound(double lat, double lng, String zipCode) {
                currentLat = lat;
                currentLng = lng;
                currentZipCode = zipCode;
                loadAvailability();
                loadAlternatives();
            }

            @Override
            public void onLocationError(String error) {
                // Fallback to defaults
                currentLat = 0;
                currentLng = 0;
                currentZipCode = null;
                loadAvailability();
                loadAlternatives();
            }
        });
    }

    private void loadAvailability() {
        if (currentProduct == null) return;
        setSectionVisible(whereToBuySection, true);
        setMessage(whereToBuyEmptyText, context.getString(R.string.checking_retailers));
        repository.getAvailability(currentProduct, currentZipCode, currentLat, currentLng, new ProductRepository.RepositoryCallback<List<RetailerAvailability>>() {
            @Override
            public void onComplete(List<RetailerAvailability> result) {
                host.runOnUiThread(() -> {
                    if (!host.isActive()) return;
                    availabilityAdapter.submitList(result);
                    if (availabilityMapView != null) {
                        availabilityMapView.submitList(result);
                    }
                    setMessage(whereToBuyEmptyText, result == null || result.isEmpty() ? context.getString(R.string.no_retailers_found) : "");
                    GlassMotion.enter(whereToBuySection, 0L);
                });
            }

            @Override
            public void onError(Exception e) {
                host.runOnUiThread(() -> {
                    if (!host.isActive()) return;
                    setMessage(whereToBuyEmptyText, context.getString(R.string.retailer_lookup_failed));
                });
            }
        });
    }

    private void loadAlternatives() {
        if (currentProduct == null) return;
        setSectionVisible(alternativesSection, true);
        if (cachedAlternatives != null) {
            alternativeAdapter.submitList(cachedAlternatives);
            setMessage(alternativesEmptyText, cachedAlternatives.isEmpty() ? context.getString(R.string.no_alternatives_found) : "");
            if (cachedAlternatives.isEmpty()) {
                setSectionVisible(alternativesSection, false);
            } else {
                GlassMotion.enter(alternativesSection, 0L);
            }
            return;
        }

        setMessage(alternativesEmptyText, context.getString(R.string.finding_better_alternatives));
        repository.getAlternatives(currentProduct, currentZipCode, currentLat, currentLng, new ProductRepository.RepositoryCallback<List<RetailerAlternative>>() {
            @Override
            public void onComplete(List<RetailerAlternative> result) {
                host.runOnUiThread(() -> {
                    if (!host.isActive()) return;
                    cachedAlternatives = result;
                    alternativeAdapter.submitList(result);
                    boolean hasAlternatives = result != null && !result.isEmpty();
                    setMessage(alternativesEmptyText, hasAlternatives ? "" : context.getString(R.string.no_alternatives_found));
                    setSectionVisible(alternativesSection, hasAlternatives);
                    if (hasAlternatives) {
                        GlassMotion.enter(alternativesSection, 0L);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                host.runOnUiThread(() -> {
                    if (!host.isActive()) return;
                    setMessage(alternativesEmptyText, context.getString(R.string.alternative_lookup_failed));
                });
            }
        });
    }

    private void setSectionVisible(View section, boolean visible) {
        if (section != null) {
            section.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setMessage(TextView textView, String message) {
        if (textView == null) return;
        boolean hasMessage = message != null && !message.trim().isEmpty();
        textView.setText(hasMessage ? message : "");
        textView.setVisibility(hasMessage ? View.VISIBLE : View.GONE);
    }
}
