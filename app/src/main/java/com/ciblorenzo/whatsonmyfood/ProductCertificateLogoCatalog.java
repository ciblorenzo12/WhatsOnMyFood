package com.ciblorenzo.whatsonmyfood;

import java.util.Locale;

/** Explicit certification aliases only: dietary claims never imply a certifier. */
public final class ProductCertificateLogoCatalog {
    private ProductCertificateLogoCatalog() {}

    public static String findLogoKey(String label) {
        if (label == null) return null;
        String normalized = label.toLowerCase(Locale.US).replaceFirst("^[a-z]{2}:", "")
                .replaceAll("[^a-z0-9]+", " ").trim();
        switch (normalized) {
            case "gfco":
            case "gfco certified":
            case "gluten free certification organization": return "gfco";
            case "usda organic": return "usda_organic";
            case "non gmo project":
            case "nongmo project":
            case "non gmo project verified": return "non_gmo_project_verified";
            case "orthodox union":
            case "orthodox union kosher":
            case "ou kosher":
            case "ou pareve":
            case "ou parve": return "ou_kosher";
            case "vegan action":
            case "vegan action certified":
            case "vegan action certified vegan": return "vegan_action";
            case "fair trade usa":
            case "fair trade usa certified": return "fair_trade";
            case "rainforest alliance":
            case "rainforest alliance certified": return "rainforest";
            case "certified b corporation":
            case "b corp":
            case "b corporation": return "b_corp";
            case "regenerative organic certified":
            case "certified regenerative organic":
            case "roc certified": return "regenerative";
            case "certified humane":
            case "humane certified":
            case "humane farm animal care": return "animal_welfare";
            case "green dot":
            case "the green dot":
            case "point vert":
            case "der grune punkt":
            case "der gruene punkt": return "green_dot";
            case "triman": return "triman";
            default: return null;
        }
    }
}
