package com.example.myapplication.retail;

import android.app.Application;

import com.example.myapplication.Product;
import com.example.myapplication.ProductRepository;
import com.example.myapplication.ProductWithDetails;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RetailerRepository {
    private static final long AVAILABILITY_CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final long ALTERNATIVES_CACHE_TTL_MS = 30 * 60 * 1000L;
    private static final int MAX_CACHE_ENTRIES = 40;

    private final RetailerBackendClient backendClient;
    private final RetailerBackendClient fallbackClient;
    private final ProductImageLookupClient imageLookupClient;
    private final RetailerAddressLookupClient addressLookupClient;
    private final ExecutorService executorService;
    private final Map<String, CacheEntry<List<RetailerAvailability>>> availabilityCache;
    private final Map<String, CacheEntry<List<RetailerAlternative>>> alternativesCache;

    public RetailerRepository(Application application) {
        this(HttpRetailerBackendClient.isConfigured()
                ? new HttpRetailerBackendClient(com.example.myapplication.BuildConfig.RETAILER_BACKEND_BASE_URL)
                : new MockRetailerBackendClient());
    }

    public RetailerRepository(RetailerBackendClient backendClient) {
        this.backendClient = backendClient;
        this.fallbackClient = backendClient instanceof MockRetailerBackendClient
                ? null
                : new MockRetailerBackendClient();
        this.imageLookupClient = new ProductImageLookupClient();
        this.addressLookupClient = new RetailerAddressLookupClient();
        this.executorService = Executors.newSingleThreadExecutor();
        this.availabilityCache = new LinkedHashMap<>();
        this.alternativesCache = new LinkedHashMap<>();
    }

    public void getAvailability(ProductWithDetails productDetails, ProductRepository.RepositoryCallback<List<RetailerAvailability>> callback) {
        getAvailability(productDetails, null, 0, 0, callback);
    }

    public void getAvailability(ProductWithDetails productDetails, String zipCode, double lat, double lng, ProductRepository.RepositoryCallback<List<RetailerAvailability>> callback) {
        RetailerProductQuery query = buildQuery(productDetails, zipCode, lat, lng);
        executorService.execute(() -> {
            String cacheKey = availabilityCacheKey(query);
            CacheEntry<List<RetailerAvailability>> cached = availabilityCache.get(cacheKey);
            if (isCacheableLocation(query) && isFresh(cached, AVAILABILITY_CACHE_TTL_MS)) {
                callback.onComplete(new ArrayList<>(cached.value));
                return;
            }

            try {
                List<RetailerAvailability> baseResults = backendClient.fetchAvailability(query);
                List<RetailerAvailability> results = sortAndDedupeAvailability(enrichAvailabilityAddresses(baseResults, query));
                cacheAvailability(query, cacheKey, results);
                callback.onComplete(results);
            } catch (Exception e) {
                List<RetailerAvailability> fallbackResults = fallbackAvailability(query);
                if (fallbackResults == null) {
                    callback.onError(e);
                } else {
                    List<RetailerAvailability> results = sortAndDedupeAvailability(enrichAvailabilityAddresses(fallbackResults, query));
                    cacheAvailability(query, cacheKey, results);
                    callback.onComplete(results);
                }
            }
        });
    }

    public void getAlternatives(ProductWithDetails productDetails, ProductRepository.RepositoryCallback<List<RetailerAlternative>> callback) {
        getAlternatives(productDetails, null, 0, 0, callback);
    }

    public void getAlternatives(ProductWithDetails productDetails, String zipCode, double lat, double lng, ProductRepository.RepositoryCallback<List<RetailerAlternative>> callback) {
        RetailerProductQuery query = buildQuery(productDetails, zipCode, lat, lng);
        executorService.execute(() -> {
            String cacheKey = alternativesCacheKey(query);
            CacheEntry<List<RetailerAlternative>> cached = alternativesCache.get(cacheKey);
            if (isCacheableLocation(query) && isFresh(cached, ALTERNATIVES_CACHE_TTL_MS)) {
                callback.onComplete(new ArrayList<>(cached.value));
                return;
            }

            try {
                List<RetailerAlternative> results = sortAndDedupeAlternatives(enrichAlternativeImages(backendClient.fetchAlternatives(query)));
                cacheAlternatives(query, cacheKey, results);
                callback.onComplete(results);
            } catch (Exception e) {
                List<RetailerAlternative> fallbackResults = fallbackAlternatives(query);
                if (fallbackResults == null) {
                    callback.onError(e);
                } else {
                    List<RetailerAlternative> results = sortAndDedupeAlternatives(enrichAlternativeImages(fallbackResults));
                    cacheAlternatives(query, cacheKey, results);
                    callback.onComplete(results);
                }
            }
        });
    }

    public void close() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private List<RetailerAvailability> sortAndDedupeAvailability(List<RetailerAvailability> results) {
        Map<String, RetailerAvailability> unique = new LinkedHashMap<>();
        if (results != null) {
            for (RetailerAvailability result : results) {
                if (result == null || result.retailerName == null) continue;
                String key = result.retailerName.toLowerCase().replaceAll("[^a-z0-9]+", "");
                RetailerAvailability existing = unique.get(key);
                if (existing == null || distanceValue(result) < distanceValue(existing)) {
                    unique.put(key, result);
                }
            }
        }

        List<RetailerAvailability> sorted = new ArrayList<>(unique.values());
        sorted.sort((first, second) -> {
            int availabilityCompare = Boolean.compare(second.available, first.available);
            if (availabilityCompare != 0) return availabilityCompare;
            return Double.compare(distanceValue(first), distanceValue(second));
        });
        return sorted;
    }

    private List<RetailerAvailability> enrichAvailabilityAddresses(List<RetailerAvailability> results, RetailerProductQuery query) {
        if (results == null || results.isEmpty()) return results;
        List<RetailerAvailability> enriched = new ArrayList<>();
        for (RetailerAvailability item : results) {
            if (item == null) continue;
            if (shouldResolvePhysicalStore(item)) {
                RetailerAvailability resolved = addressLookupClient.resolve(item, query);
                enriched.add(resolved);
            } else {
                enriched.add(item);
            }
        }
        return enriched;
    }

    private boolean shouldResolvePhysicalStore(RetailerAvailability item) {
        if (item == null || item.retailerName == null) return false;
        String lower = item.retailerName.toLowerCase();
        return !lower.contains("amazon")
                && !lower.contains("instacart")
                && item.distanceValue < 95.0;
    }

    private double distanceValue(RetailerAvailability item) {
        if (item == null || item.distanceValue <= 0.0) {
            return Double.MAX_VALUE;
        }
        return item.distanceValue;
    }

    private List<RetailerAlternative> sortAndDedupeAlternatives(List<RetailerAlternative> results) {
        Map<String, RetailerAlternative> unique = new LinkedHashMap<>();
        if (results != null) {
            for (RetailerAlternative result : results) {
                if (result == null || result.productName == null) continue;
                String key = ((result.brand != null ? result.brand : "") + result.productName)
                        .toLowerCase()
                        .replaceAll("[^a-z0-9]+", "");
                RetailerAlternative existing = unique.get(key);
                if (existing == null || result.healthScore > existing.healthScore) {
                    unique.put(key, result);
                }
            }
        }

        List<RetailerAlternative> sorted = new ArrayList<>(unique.values());
        sorted.sort((first, second) -> {
            int healthCompare = Integer.compare(second.healthScore, first.healthScore);
            if (healthCompare != 0) return healthCompare;
            int distanceCompare = Double.compare(alternativeDistanceValue(first), alternativeDistanceValue(second));
            if (distanceCompare != 0) return distanceCompare;
            return Double.compare(alternativePriceValue(first), alternativePriceValue(second));
        });
        return sorted;
    }

    private List<RetailerAlternative> enrichAlternativeImages(List<RetailerAlternative> results) {
        if (results == null || results.isEmpty()) return results;
        List<RetailerAlternative> enriched = new ArrayList<>();
        for (RetailerAlternative item : results) {
            if (item == null || !isBlank(item.imageUrl)) {
                enriched.add(item);
                continue;
            }
            String imageUrl = imageLookupClient.findOpenFoodFactsImage(item.brand, item.productName);
            if (isBlank(imageUrl)) {
                enriched.add(item);
            } else {
                enriched.add(new RetailerAlternative(
                        item.productName,
                        item.brand,
                        item.reason,
                        item.healthSignal,
                        item.retailerHint,
                        item.productUrl,
                        imageUrl,
                        item.healthScore,
                        item.priceValue,
                        item.distanceValue
                ));
            }
        }
        return enriched;
    }

    private double alternativeDistanceValue(RetailerAlternative item) {
        if (item == null || item.distanceValue <= 0.0) {
            return Double.MAX_VALUE;
        }
        return item.distanceValue;
    }

    private double alternativePriceValue(RetailerAlternative item) {
        if (item == null || item.priceValue <= 0.0) {
            return Double.MAX_VALUE;
        }
        return item.priceValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void cacheAvailability(RetailerProductQuery query, String cacheKey, List<RetailerAvailability> results) {
        if (!isCacheableLocation(query) || results == null) return;
        availabilityCache.put(cacheKey, new CacheEntry<>(new ArrayList<>(results), System.currentTimeMillis()));
        trimCache(availabilityCache);
    }

    private void cacheAlternatives(RetailerProductQuery query, String cacheKey, List<RetailerAlternative> results) {
        if (!isCacheableLocation(query) || results == null) return;
        alternativesCache.put(cacheKey, new CacheEntry<>(new ArrayList<>(results), System.currentTimeMillis()));
        trimCache(alternativesCache);
    }

    private boolean isFresh(CacheEntry<?> cacheEntry, long ttlMs) {
        return cacheEntry != null && System.currentTimeMillis() - cacheEntry.createdAtMs < ttlMs;
    }

    private boolean isCacheableLocation(RetailerProductQuery query) {
        if (query == null) return false;
        if (!isBlank(query.zipCode)) return true;
        return query.latitude != 0.0 && query.longitude != 0.0;
    }

    private String availabilityCacheKey(RetailerProductQuery query) {
        return "availability:" + productKey(query) + ":" + locationKey(query);
    }

    private String alternativesCacheKey(RetailerProductQuery query) {
        return "alternatives:" + productKey(query) + ":" + locationKey(query);
    }

    private String productKey(RetailerProductQuery query) {
        if (query == null) return "";
        if (!isBlank(query.barcode)) return query.barcode.trim();
        return ((query.brand != null ? query.brand : "") + "|" + (query.productName != null ? query.productName : ""))
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9|]+", "");
    }

    private String locationKey(RetailerProductQuery query) {
        if (query == null) return "unknown";
        if (!isBlank(query.zipCode)) {
            return "zip:" + query.zipCode.trim();
        }
        return String.format(Locale.US, "geo:%.3f,%.3f", query.latitude, query.longitude);
    }

    private <T> void trimCache(Map<String, CacheEntry<T>> cache) {
        Iterator<String> iterator = cache.keySet().iterator();
        while (cache.size() > MAX_CACHE_ENTRIES && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private List<RetailerAvailability> fallbackAvailability(RetailerProductQuery query) {
        if (fallbackClient == null) return null;
        try {
            return fallbackClient.fetchAvailability(query);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<RetailerAlternative> fallbackAlternatives(RetailerProductQuery query) {
        if (fallbackClient == null) return null;
        try {
            return fallbackClient.fetchAlternatives(query);
        } catch (Exception ignored) {
            return null;
        }
    }

    private RetailerProductQuery buildQuery(ProductWithDetails productDetails, String zipCode, double lat, double lng) {
        Product product = productDetails != null ? productDetails.product : null;
        return new RetailerProductQuery(
                product != null ? product.barcode : "",
                product != null ? product.productName : "",
                product != null ? product.brands : "",
                product != null ? product.categories : "",
                zipCode != null ? zipCode : "",
                lat,
                lng
        );
    }

    private static class CacheEntry<T> {
        final T value;
        final long createdAtMs;

        CacheEntry(T value, long createdAtMs) {
            this.value = value;
            this.createdAtMs = createdAtMs;
        }
    }
}
