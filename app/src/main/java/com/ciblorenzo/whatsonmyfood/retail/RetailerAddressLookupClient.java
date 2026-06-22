package com.ciblorenzo.whatsonmyfood.retail;

import android.net.Uri;

import com.ciblorenzo.whatsonmyfood.BuildConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class RetailerAddressLookupClient {
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";
    private static final String OSRM_ROUTE_URL = "https://router.project-osrm.org/route/v1/driving/";
    private static final double METERS_PER_MILE = 1609.344;
    private static final long MIN_REQUEST_INTERVAL_MS = 1100L;
    private static final Object RATE_LIMIT_LOCK = new Object();
    private static long lastRequestAt;

    private final OkHttpClient client = new OkHttpClient();

    public RetailerAvailability resolve(RetailerAvailability item, RetailerProductQuery query) {
        if (item == null || query == null || !hasLocation(query.latitude, query.longitude)) {
            return item;
        }
        if (hasLocation(item.latitude, item.longitude) && !isBlank(item.address)) {
            return item;
        }

        String chainName = searchablePhysicalStoreName(item.retailerName);
        if (chainName.isEmpty()) {
            return item;
        }

        try {
            double radiusMiles = item.distanceValue > 0.0 && item.distanceValue < 50.0
                    ? Math.max(15.0, item.distanceValue + 5.0)
                    : 15.0;
            JsonObject store = findNearestStore(chainName, query.latitude, query.longitude, radiusMiles);
            if (store == null) {
                store = findNearestStoreByNominatim(chainName, query.latitude, query.longitude, radiusMiles);
            }
            if (store == null) return item;

            double latitude = readElementLatitude(store);
            double longitude = readElementLongitude(store);
            if (!hasLocation(latitude, longitude)) return item;

            JsonObject tags = store.has("tags") && store.get("tags").isJsonObject()
                    ? store.getAsJsonObject("tags")
                    : new JsonObject();
            String address = buildTaggedAddress(tags);
            if (isBlank(address)) {
                address = reverseAddress(latitude, longitude);
            }
            String mapUrl = "https://www.google.com/maps/search/?api=1&query="
                    + Uri.encode(latitude + "," + longitude + " " + chainName);
            double straightLineMiles = distanceMeters(query.latitude, query.longitude, latitude, longitude) / METERS_PER_MILE;
            double actualDistanceMiles = drivingDistanceMiles(query.latitude, query.longitude, latitude, longitude, straightLineMiles);
            return copyWithLocation(item, address, latitude, longitude, mapUrl, actualDistanceMiles);
        } catch (Exception ignored) {
            return item;
        }
    }

    private JsonObject findNearestStore(String chainName, double latitude, double longitude, double radiusMiles) throws Exception {
        int radiusMeters = (int) Math.round(radiusMiles * METERS_PER_MILE);
        String regex = overpassNameRegex(chainName);
        String overpassQuery = "[out:json][timeout:8];("
                + "node(around:" + radiusMeters + "," + latitude + "," + longitude + ")[\"shop\"][\"name\"~\"" + regex + "\",i];"
                + "way(around:" + radiusMeters + "," + latitude + "," + longitude + ")[\"shop\"][\"name\"~\"" + regex + "\",i];"
                + "relation(around:" + radiusMeters + "," + latitude + "," + longitude + ")[\"shop\"][\"name\"~\"" + regex + "\",i];"
                + "node(around:" + radiusMeters + "," + latitude + "," + longitude + ")[\"shop\"][\"brand\"~\"" + regex + "\",i];"
                + "way(around:" + radiusMeters + "," + latitude + "," + longitude + ")[\"shop\"][\"brand\"~\"" + regex + "\",i];"
                + "relation(around:" + radiusMeters + "," + latitude + "," + longitude + ")[\"shop\"][\"brand\"~\"" + regex + "\",i];"
                + ");out center tags 20;";

        throttle();
        Request request = new Request.Builder()
                .url(OVERPASS_URL)
                .post(new FormBody.Builder().add("data", overpassQuery).build())
                .header("User-Agent", BuildConfig.APPLICATION_ID + "/1.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray elements = root.has("elements") && root.get("elements").isJsonArray()
                    ? root.getAsJsonArray("elements")
                    : new JsonArray();
            return nearestElement(elements, latitude, longitude);
        }
    }

    private JsonObject findNearestStoreByNominatim(String chainName, double latitude, double longitude,
                                                   double radiusMiles) throws Exception {
        String viewBox = buildViewBox(latitude, longitude, radiusMiles);
        String url = Uri.parse("https://nominatim.openstreetmap.org/search").buildUpon()
                .appendQueryParameter("format", "jsonv2")
                .appendQueryParameter("addressdetails", "1")
                .appendQueryParameter("limit", "5")
                .appendQueryParameter("bounded", "1")
                .appendQueryParameter("viewbox", viewBox)
                .appendQueryParameter("q", chainName)
                .build()
                .toString();

        throttle();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", BuildConfig.APPLICATION_ID + "/1.0")
                .header("Accept-Language", Locale.getDefault().toLanguageTag())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return null;
            JsonArray results = JsonParser.parseString(response.body().string()).getAsJsonArray();
            JsonObject best = null;
            double bestDistance = Double.MAX_VALUE;
            for (JsonElement element : results) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                if (!isRetailShopResult(object, chainName)) continue;
                String displayName = readString(object, "display_name");
                if (!matchesChain(displayName, chainName)) continue;
                double itemLat = parseDouble(object, "lat");
                double itemLng = parseDouble(object, "lon");
                if (!hasLocation(itemLat, itemLng)) continue;
                double distance = distanceMeters(latitude, longitude, itemLat, itemLng);
                if (distance < bestDistance) {
                    JsonObject normalized = new JsonObject();
                    normalized.addProperty("lat", itemLat);
                    normalized.addProperty("lon", itemLng);
                    JsonObject tags = new JsonObject();
                    tags.addProperty("name", chainName);
                    tags.addProperty("display_name", displayName);
                    normalized.add("tags", tags);
                    best = normalized;
                    bestDistance = distance;
                }
            }
            return best;
        }
    }

    private String reverseAddress(double latitude, double longitude) throws Exception {
        String url = Uri.parse(NOMINATIM_REVERSE_URL).buildUpon()
                .appendQueryParameter("format", "jsonv2")
                .appendQueryParameter("lat", String.valueOf(latitude))
                .appendQueryParameter("lon", String.valueOf(longitude))
                .appendQueryParameter("zoom", "18")
                .appendQueryParameter("addressdetails", "1")
                .build()
                .toString();
        throttle();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", BuildConfig.APPLICATION_ID + "/1.0")
                .header("Accept-Language", Locale.getDefault().toLanguageTag())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return "";
            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return cleanAddress(readString(root, "display_name"));
        }
    }

    private double drivingDistanceMiles(double startLat, double startLng, double endLat, double endLng,
                                        double fallbackMiles) {
        try {
            String coordinatePath = startLng + "," + startLat + ";" + endLng + "," + endLat;
            String url = Uri.parse(OSRM_ROUTE_URL + coordinatePath).buildUpon()
                    .appendQueryParameter("overview", "false")
                    .appendQueryParameter("alternatives", "false")
                    .appendQueryParameter("steps", "false")
                    .build()
                    .toString();
            throttle();
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", BuildConfig.APPLICATION_ID + "/1.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return fallbackMiles;
                JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
                JsonArray routes = root.has("routes") && root.get("routes").isJsonArray()
                        ? root.getAsJsonArray("routes")
                        : new JsonArray();
                if (routes.size() == 0 || !routes.get(0).isJsonObject()) return fallbackMiles;
                JsonObject route = routes.get(0).getAsJsonObject();
                if (!route.has("distance")) return fallbackMiles;
                return route.get("distance").getAsDouble() / METERS_PER_MILE;
            }
        } catch (Exception ignored) {
            return fallbackMiles;
        }
    }

    private String buildViewBox(double latitude, double longitude, double radiusMiles) {
        double latDelta = radiusMiles / 69.0;
        double lngDelta = radiusMiles / Math.max(1.0, 69.0 * Math.cos(Math.toRadians(latitude)));
        double left = longitude - lngDelta;
        double right = longitude + lngDelta;
        double top = latitude + latDelta;
        double bottom = latitude - latDelta;
        return String.format(Locale.US, "%.6f,%.6f,%.6f,%.6f", left, top, right, bottom);
    }

    private JsonObject nearestElement(JsonArray elements, double latitude, double longitude) {
        JsonObject best = null;
        double bestDistance = Double.MAX_VALUE;
        for (JsonElement element : elements) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            JsonObject tags = object.has("tags") && object.get("tags").isJsonObject()
                    ? object.getAsJsonObject("tags")
                    : new JsonObject();
            if (!isAcceptedRetailShop(readString(tags, "shop"))) continue;
            double itemLat = readElementLatitude(object);
            double itemLng = readElementLongitude(object);
            if (!hasLocation(itemLat, itemLng)) continue;
            double distance = distanceMeters(latitude, longitude, itemLat, itemLng);
            if (distance < bestDistance) {
                best = object;
                bestDistance = distance;
            }
        }
        return best;
    }

    private RetailerAvailability copyWithLocation(RetailerAvailability item, String address,
                                                  double latitude, double longitude, String mapUrl,
                                                  double actualDistanceMiles) {
        return new RetailerAvailability(
                item.retailerName,
                item.providerName,
                inventoryStatus(item),
                item.price,
                formatDistance(actualDistanceMiles),
                item.fulfillment,
                item.productUrl,
                inventoryNote(item),
                item.available,
                item.priceValue,
                actualDistanceMiles,
                address,
                latitude,
                longitude,
                mapUrl
        );
    }

    private static void throttle() throws InterruptedException {
        synchronized (RATE_LIMIT_LOCK) {
            long now = System.currentTimeMillis();
            long waitMs = MIN_REQUEST_INTERVAL_MS - (now - lastRequestAt);
            if (waitMs > 0) {
                Thread.sleep(waitMs);
            }
            lastRequestAt = System.currentTimeMillis();
        }
    }

    private String searchablePhysicalStoreName(String retailerName) {
        if (retailerName == null) return "";
        String lower = retailerName.toLowerCase(Locale.US);
        if (lower.contains("amazon") || lower.contains("instacart")) return "";
        if (lower.contains("walmart")) return "Walmart";
        if (lower.contains("target")) return "Target";
        if (lower.contains("whole foods")) return "Whole Foods Market";
        if (lower.contains("trader joe")) return "Trader Joe";
        if (lower.contains("sprouts")) return "Sprouts Farmers Market";
        if (lower.contains("costco")) return "Costco";
        if (lower.contains("safeway")) return "Safeway";
        if (lower.contains("publix")) return "Publix";
        if (lower.contains("kroger")) return "Kroger";
        return "";
    }

    private boolean matchesChain(String value, String chainName) {
        if (value == null || chainName == null) return false;
        String normalizedValue = normalize(value);
        String normalizedChain = normalize(chainName);
        if (normalizedValue.contains(normalizedChain)) return true;
        if (normalizedChain.equals("trader joe")) return normalizedValue.contains("trader joe");
        if (normalizedChain.equals("whole foods market")) return normalizedValue.contains("whole foods");
        if (normalizedChain.equals("sprouts farmers market")) return normalizedValue.contains("sprouts");
        return false;
    }

    private boolean isRetailShopResult(JsonObject result, String chainName) {
        String category = normalize(readString(result, "category"));
        String type = normalize(readString(result, "type"));
        String objectClass = normalize(readString(result, "class"));
        String combined = category + " " + type + " " + objectClass + " " + normalize(readString(result, "display_name"));
        if (!matchesChain(combined, chainName)) return false;
        return combined.contains("shop")
                || combined.contains("supermarket")
                || combined.contains("department store")
                || combined.contains("wholesale")
                || combined.contains("convenience")
                || combined.contains("grocery");
    }

    private boolean isAcceptedRetailShop(String shop) {
        String normalized = normalize(shop);
        return normalized.equals("supermarket")
                || normalized.equals("department store")
                || normalized.equals("convenience")
                || normalized.equals("wholesale")
                || normalized.equals("grocery")
                || normalized.equals("variety store")
                || normalized.equals("general");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String inventoryStatus(RetailerAvailability item) {
        if (item.providerName != null
                && !item.providerName.contains("Mock")
                && !item.providerName.equals("KrogerAPI")
                && !item.providerName.equals("InstacartConnect")) {
            return item.availabilityStatus;
        }
        return "Check nearby store";
    }

    private String inventoryNote(RetailerAvailability item) {
        String base = item.note != null ? item.note.trim() : "";
        String inventoryMessage = "Check the retailer for current stock before you go.";
        if (isInternalNote(base)) return inventoryMessage;
        if (base.isEmpty()) return inventoryMessage;
        if (base.toLowerCase(Locale.US).contains("current stock")) return base;
        if (base.toLowerCase(Locale.US).contains("confirm this product")) return base;
        if (base.toLowerCase(Locale.US).contains("current price and availability")) return base;
        return base + " " + inventoryMessage;
    }

    private boolean isInternalNote(String note) {
        String lower = note == null ? "" : note.toLowerCase(Locale.US);
        return lower.contains("api")
                || lower.contains("mock")
                || lower.contains("demo result")
                || lower.contains("phase 1")
                || lower.contains("provider")
                || lower.contains("verified")
                || lower.contains("retailer confirmation");
    }

    private String formatDistance(double distanceMiles) {
        if (distanceMiles <= 0.0) return "Nearby";
        return String.format(Locale.US, "%.1f mi drive", distanceMiles);
    }

    private String overpassNameRegex(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", ".?");
    }

    private String buildTaggedAddress(JsonObject tags) {
        String displayName = readString(tags, "display_name");
        if (!displayName.isEmpty()) {
            return cleanAddress(displayName);
        }
        String houseNumber = readString(tags, "addr:housenumber");
        String street = readString(tags, "addr:street");
        String city = readString(tags, "addr:city");
        String state = readString(tags, "addr:state");
        String postcode = readString(tags, "addr:postcode");

        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, (houseNumber + " " + street).trim());
        appendAddressPart(builder, city);
        appendAddressPart(builder, state);
        appendAddressPart(builder, postcode);
        return builder.toString();
    }

    private void appendAddressPart(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (builder.length() > 0) builder.append(", ");
        builder.append(value.trim());
    }

    private String cleanAddress(String address) {
        if (address == null) return "";
        String[] parts = address.split(",");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 5); i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            if (builder.length() > 0) builder.append(", ");
            builder.append(part);
        }
        return builder.toString();
    }

    private double readElementLatitude(JsonObject object) {
        if (object.has("lat")) return object.get("lat").getAsDouble();
        if (object.has("center") && object.get("center").isJsonObject()) {
            JsonObject center = object.getAsJsonObject("center");
            if (center.has("lat")) return center.get("lat").getAsDouble();
        }
        return 0.0;
    }

    private double readElementLongitude(JsonObject object) {
        if (object.has("lon")) return object.get("lon").getAsDouble();
        if (object.has("center") && object.get("center").isJsonObject()) {
            JsonObject center = object.getAsJsonObject("center");
            if (center.has("lon")) return center.get("lon").getAsDouble();
        }
        return 0.0;
    }

    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2.0) * Math.sin(dLng / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return earthRadius * c;
    }

    private String readString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private double parseDouble(JsonObject object, String key) {
        try {
            return Double.parseDouble(readString(object, key));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private boolean hasLocation(double latitude, double longitude) {
        return latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0
                && !(Math.abs(latitude) < 0.00001 && Math.abs(longitude) < 0.00001);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
