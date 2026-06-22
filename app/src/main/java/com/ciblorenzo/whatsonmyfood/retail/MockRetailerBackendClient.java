package com.ciblorenzo.whatsonmyfood.retail;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MockRetailerBackendClient implements RetailerBackendClient {

    private final CompositeRetailerClient compositeClient = new CompositeRetailerClient();

    @Override
    public List<RetailerAvailability> fetchAvailability(RetailerProductQuery query) throws Exception {
        return compositeClient.fetchAvailability(query);
    }

    @Override
    public List<RetailerAlternative> fetchAlternatives(RetailerProductQuery query) throws Exception {
        List<RetailerAlternative> mockAlternatives = getMockAlternatives(query);
        List<RetailerAlternative> apiAlternatives = compositeClient.fetchAlternatives(query);
        
        if (apiAlternatives != null && !apiAlternatives.isEmpty()) {
            mockAlternatives.addAll(apiAlternatives);
        }
        return mockAlternatives;
    }

    private List<RetailerAlternative> getMockAlternatives(RetailerProductQuery query) {
        String productContext = normalize((query.category != null ? query.category : "") + " "
                + (query.productName != null ? query.productName : ""));

        if (containsAny(productContext, "soda", "sodas", "cola", "soft drink", "soft drinks")) {
            return sodaAlternatives();
        }
        if (containsAny(productContext, "protein bar", "protein bars", "energy bar", "energy bars",
                "nutrition bar", "nutrition bars", "snack bar", "snack bars", "granola bar", "granola bars")) {
            return barAlternatives();
        }
        if (containsAny(productContext, "cereal", "cereals", "breakfast cereal", "breakfast cereals")) {
            return cerealAlternatives();
        }
        if (containsAny(productContext, "granola", "muesli")) {
            return granolaAlternatives();
        }
        if (containsAny(productContext, "yogurt", "yogurts", "yoghurt", "yoghurts", "skyr")) {
            return yogurtAlternatives();
        }
        if (containsAny(productContext, "chocolate", "chocolates", "candy", "candies", "sweets")) {
            return chocolateAlternatives();
        }
        if (containsAny(productContext, "cookie", "cookies", "biscuit", "biscuits")) {
            return cookieAlternatives();
        }
        if (containsAny(productContext, "chip", "chips", "crisps", "cracker", "crackers", "popcorn",
                "pretzel", "pretzels", "puffs", "tortilla chips", "potato chips")) {
            return snackAlternatives();
        }
        if (containsAny(productContext, "bread", "breads", "bagel", "bagels", "bun", "buns", "tortilla", "wrap", "wraps")) {
            return breadAlternatives();
        }
        if (containsAny(productContext, "pasta", "spaghetti", "macaroni", "noodle", "noodles")) {
            return pastaAlternatives();
        }
        if (containsAny(productContext, "sauce", "sauces", "dressing", "dressings", "ketchup",
                "mayonnaise", "mayo", "marinara", "salsa")) {
            return sauceAlternatives();
        }
        if (containsAny(productContext, "peanut butter", "almond butter", "cashew butter", "nut butter", "nut butters")) {
            return nutButterAlternatives();
        }
        if (containsAny(productContext, "milk", "milks", "almond milk", "oat milk", "soy milk", "plant milk", "plant milks")) {
            return milkAlternatives();
        }
        if (containsAny(productContext, "drink", "drinks", "beverage", "beverages", "water", "juice", "lemonade", "tea")) {
            return beverageAlternatives();
        }
        return new ArrayList<>();
    }

    private List<RetailerAlternative> sodaAlternatives() {
        return alternatives(
                alternative(
                        "Vintage Cola",
                        "Olipop",
                        "Same cola-style shelf, with prebiotic fiber and much less sugar than regular soda.",
                        "Related soda swap",
                        "Available at Target, Walmart, Kroger, and Instacart",
                        92, 2.49, 1.2
                ),
                alternative(
                        "Classic Cola",
                        "Poppi",
                        "A real cola alternative positioned around lower sugar and prebiotics.",
                        "Lower sugar cola",
                        "Available at many grocery retailers",
                        90, 2.49, 1.5
                ),
                alternative(
                        "Zero Sugar Cola",
                        "Zevia",
                        "A zero-sugar cola option without synthetic colors.",
                        "Zero sugar soda",
                        "Available online and at major grocery stores",
                        85, 5.99, 0.0
                )
        );
    }

    private List<RetailerAlternative> beverageAlternatives() {
        return alternatives(
                alternative(
                        "Sparkling Water",
                        "Spindrift",
                        "Same ready-to-drink category, made with sparkling water and real fruit juice.",
                        "Cleaner beverage option",
                        "Available at Target, Walmart, Kroger, and Instacart",
                        95, 6.49, 1.2
                ),
                alternative(
                        "Organic Juice Drink",
                        "Honest Kids",
                        "Related juice-box style option with lower sugar per pouch than many juice drinks.",
                        "Lower sugar juice drink",
                        "Available at Target, Walmart, Kroger, and Instacart",
                        75, 4.99, 1.2
                ),
                alternative(
                        "Coconut Water, No Sugar Added",
                        "Harmless Harvest",
                        "A simple beverage swap with no added sugar.",
                        "No added sugar beverage",
                        "Available online and in health grocery stores",
                        88, 3.99, 2.0
                )
        );
    }

    private List<RetailerAlternative> barAlternatives() {
        return alternatives(
                alternative(
                        "Chocolate Sea Salt Protein Bar",
                        "RXBAR",
                        "Same snack-bar format with a short, recognizable ingredient list.",
                        "Short ingredient bar",
                        "Available online and near many grocery stores"
                ),
                alternative(
                        "Peanut Butter Chocolate Chip Bar",
                        "Larabar",
                        "A related bar made from dates, nuts, and chocolate chips.",
                        "Simple snack bar",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Organic Plant Based Protein Bar",
                        "ALOHA",
                        "A bar-category alternative with plant protein and fiber.",
                        "Plant protein bar",
                        "Available online and at select grocery retailers"
                )
        );
    }

    private List<RetailerAlternative> cerealAlternatives() {
        return alternatives(
                alternative(
                        "Unsweetened Cereal",
                        "Three Wishes",
                        "Same breakfast-cereal aisle, with no added sugar and more protein.",
                        "Lower sugar cereal",
                        "Available online and at select grocery retailers"
                ),
                alternative(
                        "Heritage Flakes",
                        "Nature's Path",
                        "A related flake cereal built around whole grains.",
                        "Whole grain cereal",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Ezekiel 4:9 Sprouted Grain Cereal",
                        "Food For Life",
                        "A cereal alternative made with sprouted grains and legumes.",
                        "Sprouted grain cereal",
                        "Available online and in health grocery stores"
                )
        );
    }

    private List<RetailerAlternative> granolaAlternatives() {
        return alternatives(
                alternative(
                        "Original Ancient Grain Granola",
                        "Purely Elizabeth",
                        "Same granola category, with recognizable grains, seeds, and nuts.",
                        "Cleaner granola option",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Peanut Butter Granola",
                        "Michele's Granola",
                        "A related granola with a short ingredient list.",
                        "Short ingredient granola",
                        "Available online and at select grocery retailers"
                ),
                alternative(
                        "Organic Honey Almond Granola",
                        "Cascadian Farm",
                        "A grocery-accessible granola option with organic ingredients.",
                        "Organic granola",
                        "Available at major grocery stores"
                )
        );
    }

    private List<RetailerAlternative> yogurtAlternatives() {
        return alternatives(
                alternative(
                        "Plain Skyr",
                        "Siggi's",
                        "Same yogurt case, with high protein and no added sugar in the plain version.",
                        "Plain high protein yogurt",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Organic Plain Greek Yogurt",
                        "Stonyfield",
                        "A related plain yogurt option without sweet mix-ins.",
                        "Plain organic yogurt",
                        "Available at many grocery retailers"
                ),
                alternative(
                        "Less Sugar Greek Yogurt",
                        "Chobani",
                        "Same single-serve yogurt format with reduced sugar.",
                        "Lower sugar yogurt",
                        "Available at Target, Walmart, Kroger, and Instacart"
                )
        );
    }

    private List<RetailerAlternative> chocolateAlternatives() {
        return alternatives(
                alternative(
                        "Simple Dark Chocolate",
                        "Hu",
                        "Same chocolate category, with a shorter ingredient list.",
                        "Cleaner chocolate option",
                        "Available online and at select grocery retailers"
                ),
                alternative(
                        "Dark Chocolate Gems",
                        "UNREAL",
                        "A related candy-coated chocolate with simpler colors and ingredients.",
                        "Better candy swap",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Organic Dark Chocolate Bar",
                        "Alter Eco",
                        "A chocolate-bar alternative with organic ingredients.",
                        "Organic chocolate",
                        "Available online and in health grocery stores"
                )
        );
    }

    private List<RetailerAlternative> cookieAlternatives() {
        return alternatives(
                alternative(
                        "Crunchy Chocolate Chip Cookies",
                        "Simple Mills",
                        "Same cookie shelf, made with a nut-and-seed flour blend.",
                        "Cleaner cookie option",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Soft Baked Mini Cookies",
                        "MadeGood",
                        "A related cookie pack with organic ingredients.",
                        "Organic cookie snack",
                        "Available at many grocery retailers"
                ),
                alternative(
                        "Grain-Free Chocolate Chip Cookies",
                        "Hu",
                        "A cookie alternative with a shorter ingredient list.",
                        "Short ingredient cookie",
                        "Available online and at select grocery retailers"
                )
        );
    }

    private List<RetailerAlternative> snackAlternatives() {
        return alternatives(
                alternative(
                        "Sea Salt Grain Free Tortilla Chips",
                        "Siete",
                        "Same chip/snack category, made with avocado oil and cassava flour.",
                        "Better chip swap",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Himalayan Pink Salt Popcorn",
                        "LesserEvil",
                        "A related salty snack with a simple ingredient list.",
                        "Simple salty snack",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Sea Salt Sweet Potato Chips",
                        "Jackson's",
                        "Same crunchy snack lane, made with sweet potatoes and avocado oil.",
                        "Cleaner chip option",
                        "Available online and at select grocery retailers"
                )
        );
    }

    private List<RetailerAlternative> breadAlternatives() {
        return alternatives(
                alternative(
                        "Thin-Sliced 21 Whole Grains and Seeds",
                        "Dave's Killer Bread",
                        "Same bread aisle, with whole grains and seeds.",
                        "Whole grain bread",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Ezekiel 4:9 Sprouted Grain Bread",
                        "Food For Life",
                        "A related bread made from sprouted grains and legumes.",
                        "Sprouted grain bread",
                        "Available online and in health grocery stores"
                ),
                alternative(
                        "Sprouted Power Bread",
                        "Silver Hills",
                        "Same loaf format with sprouted whole grains.",
                        "Sprouted bread",
                        "Available at select grocery retailers"
                )
        );
    }

    private List<RetailerAlternative> pastaAlternatives() {
        return alternatives(
                alternative(
                        "Chickpea Pasta",
                        "Banza",
                        "Same pasta shape use case, with more protein and fiber from chickpeas.",
                        "Higher protein pasta",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Whole Grain Spaghetti",
                        "Barilla",
                        "A related pasta option with whole grain wheat.",
                        "Whole grain pasta",
                        "Available at major grocery stores"
                ),
                alternative(
                        "Brown Rice Pasta",
                        "Jovial",
                        "Same pasta meal role, made with brown rice.",
                        "Simple gluten-free pasta",
                        "Available online and at select grocery retailers"
                )
        );
    }

    private List<RetailerAlternative> sauceAlternatives() {
        return alternatives(
                alternative(
                        "Marinara Sauce",
                        "Rao's Homemade",
                        "Same sauce category, known for a short ingredient list and no added sugar.",
                        "Cleaner pasta sauce",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Avocado Oil Ranch Dressing",
                        "Primal Kitchen",
                        "A related dressing made with avocado oil and no added sugar.",
                        "Cleaner dressing",
                        "Available online and at many grocery stores"
                ),
                alternative(
                        "Organic Apple Cider Vinaigrette",
                        "Bragg",
                        "Same dressing use case with a straightforward ingredient profile.",
                        "Simple vinaigrette",
                        "Available at major grocery stores"
                )
        );
    }

    private List<RetailerAlternative> nutButterAlternatives() {
        return alternatives(
                alternative(
                        "Dark Roasted Creamy Peanut Butter",
                        "Santa Cruz Organic",
                        "Same nut-butter shelf, with organic peanuts and minimal ingredients.",
                        "Simple peanut butter",
                        "Available at Target, Walmart, Kroger, and Instacart"
                ),
                alternative(
                        "Organic Creamy Peanut Butter",
                        "Once Again",
                        "A related peanut butter with a short ingredient list.",
                        "Organic nut butter",
                        "Available online and in health grocery stores"
                ),
                alternative(
                        "Classic Almond Butter",
                        "Justin's",
                        "Same spread/snack role, made from dry roasted almonds.",
                        "Almond butter swap",
                        "Available at major grocery stores"
                )
        );
    }

    private List<RetailerAlternative> milkAlternatives() {
        return alternatives(
                alternative(
                        "Unsweetened Almond Milk",
                        "MALK",
                        "Same milk alternative category, with a short ingredient list and no gums.",
                        "Short ingredient milk",
                        "Available at Whole Foods, Target, and select grocery retailers"
                ),
                alternative(
                        "Unsweetened Milked Oats",
                        "Elmhurst",
                        "A related oat milk with simple ingredients and no added gums.",
                        "Simple oat milk",
                        "Available online and at select grocery retailers"
                ),
                alternative(
                        "Unsweetened Almondmilk",
                        "Califia Farms",
                        "Same dairy-alternative use case with no added sugar.",
                        "No added sugar milk",
                        "Available at Target, Walmart, Kroger, and Instacart"
                )
        );
    }

    private List<RetailerAlternative> alternatives(RetailerAlternative... items) {
        List<RetailerAlternative> alternatives = new ArrayList<>();
        for (RetailerAlternative item : items) {
            alternatives.add(item);
        }
        return alternatives;
    }

    private RetailerAlternative alternative(String productName, String brand, String reason,
                                            String healthSignal, String retailerHint,
                                            int healthScore, double price, double distance) {
        return new RetailerAlternative(
                productName,
                brand,
                reason,
                healthSignal,
                retailerHint,
                googleSearch(brand + " " + productName),
                "",
                healthScore,
                price,
                distance
        );
    }

    private RetailerAlternative alternative(String productName, String brand, String reason,
                                            String healthSignal, String retailerHint) {
        return alternative(productName, brand, reason, healthSignal, retailerHint, 100, 0.0, 0.0);
    }

    private boolean containsAny(String context, String... terms) {
        String searchable = " " + context + " ";
        for (String term : terms) {
            if (searchable.contains(" " + normalize(term) + " ")) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private String googleSearch(String query) {
        return "https://www.google.com/search?q=" + Uri.encode(query);
    }
}
