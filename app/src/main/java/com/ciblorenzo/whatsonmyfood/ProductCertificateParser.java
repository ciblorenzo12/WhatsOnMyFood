package com.ciblorenzo.whatsonmyfood;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ProductCertificateParser {

    private ProductCertificateParser() {
    }

    public static List<ProductCertificate> findCertificates(String labels) {
        Map<String, ProductCertificate> certificates = new LinkedHashMap<>();
        for (String rawLabel : splitLabels(labels)) {
            ProductCertificate certificate = matchCertificate(rawLabel);
            if (certificate != null) {
                addCertificate(certificates, certificate);
            }
        }
        return new ArrayList<>(certificates.values());
    }

    public static String formatLabelsForDisplay(String labels) {
        List<String> formatted = new ArrayList<>();
        for (String rawLabel : splitLabels(labels)) {
            String pretty = prettify(rawLabel);
            if (!pretty.isEmpty() && !containsIgnoreCase(formatted, pretty)) {
                formatted.add(pretty);
            }
        }
        return join(formatted);
    }

    private static void addCertificate(Map<String, ProductCertificate> certificates, ProductCertificate certificate) {
        ProductCertificate existing = certificates.get(certificate.key);
        if (existing == null || (!existing.specific && certificate.specific)) {
            certificates.put(certificate.key, certificate);
        }
    }

    private static ProductCertificate matchCertificate(String rawLabel) {
        String normalized = normalize(rawLabel);
        if (normalized.isEmpty()) return null;

        if (containsAny(normalized, "usda organic")) {
            return certificate("organic", "USDA Organic", "USDA\nORGANIC", "organic", true);
        }
        if (containsAny(normalized, "eu organic", "european union organic", "ab agriculture biologique",
                "canada organic", "jas organic", "japanese agricultural standard organic",
                "ccof organic", "qai organic", "oregon tilth organic")) {
            return certificate("organic", "Organic Certified", "ORGANIC", "organic", true);
        }
        if (containsAny(normalized, "certified organic", "organic farming", "organic agriculture",
                "ccof certified organic", "qai certified organic", "oregon tilth certified organic")
                || normalized.equals("organic")
                || normalized.equals("bio")) {
            return certificate("organic", "Organic Certified", "ORGANIC", "organic", false);
        }

        if (containsAny(normalized, "non gmo project", "nongmo project", "non gmo verified",
                "non gmo project verified", "verified non gmo")) {
            return certificate("non_gmo", "Non-GMO Project Verified", "NON GMO\nPROJECT\nVERIFIED", "non_gmo", true);
        }
        if (containsAny(normalized, "non gmo", "no gmos", "gmo free", "without gmos")) {
            return certificate("non_gmo", "Non-GMO", "NON GMO", "non_gmo", false);
        }

        if (containsAny(normalized, "orthodox union", "ou kosher", "ou d", "ou dairy",
                "ou pareve", "ou parve", "u kosher")) {
            return certificate("kosher", "Orthodox Union Kosher", "OU\nKOSHER", "kosher", true);
        }
        if (containsAny(normalized, "ok kosher", "star k", "star k kosher", "kof k",
                "kof k kosher", "crc kosher", "klbd kosher", "kosher certified",
                "kosher pareve", "kosher parve", "kosher dairy", "kosher")) {
            return certificate("kosher", "Kosher Certified", "KOSHER", "kosher", false);
        }

        if (containsAny(normalized, "certified gluten free", "gluten free certification organization",
                "gfco", "nsf gluten free", "beyond celiac gluten free")) {
            return certificate("gluten_free", "Certified Gluten-Free", "CERTIFIED\nGLUTEN-FREE", "gluten_free", true);
        }
        if (containsAny(normalized, "gluten free", "no gluten", "without gluten")) {
            return certificate("gluten_free", "Gluten-Free", "GLUTEN-FREE", "gluten_free", false);
        }

        if (containsAny(normalized, "fair trade", "fairtrade", "fair trade certified",
                "fair trade usa", "fairtrade international", "fair for life", "fairtrade america")) {
            return certificate("fair_trade", "Fair Trade Certified", "FAIR TRADE\nCERTIFIED", "fair_trade", true);
        }

        if (containsAny(normalized, "rainforest alliance", "rainforest alliance certified")) {
            return certificate("rainforest", "Rainforest Alliance Certified", "RAINFOREST\nALLIANCE", "rainforest", true);
        }

        if (containsAny(normalized, "certified vegan", "vegan society", "vegan action")) {
            return certificate("vegan", "Certified Vegan", "CERTIFIED\nVEGAN", "vegan", true);
        }
        if (normalized.equals("vegan")) {
            return certificate("vegan", "Vegan", "VEGAN", "vegan", false);
        }

        if (containsAny(normalized, "certified plant based", "plant based foods association", "pbfa certified")) {
            return certificate("plant_based", "Certified Plant-Based", "CERTIFIED\nPLANT\nBASED", "plant_based", true);
        }
        if (containsAny(normalized, "plant based")) {
            return certificate("plant_based", "Plant-Based", "PLANT\nBASED", "plant_based", false);
        }

        if (containsAny(normalized, "certified vegetarian", "vegetarian society")) {
            return certificate("vegetarian", "Certified Vegetarian", "CERTIFIED\nVEGETARIAN", "vegetarian", true);
        }
        if (normalized.equals("vegetarian")) {
            return certificate("vegetarian", "Vegetarian", "VEGETARIAN", "vegetarian", false);
        }

        if (containsAny(normalized, "halal certified", "ifanca", "islamic food and nutrition council",
                "hfsaa", "halal food standards alliance", "halal")) {
            return certificate("halal", "Halal Certified", "HALAL", "halal", normalized.contains("certified") || normalized.contains("ifanca"));
        }

        if (containsAny(normalized, "certified b corporation", "b corp", "b corporation")) {
            return certificate("b_corp", "Certified B Corporation", "B CORP\nCERTIFIED", "b_corp", true);
        }

        if (containsAny(normalized, "regenerative organic certified", "certified regenerative organic", "roc certified")) {
            return certificate("regenerative", "Regenerative Organic Certified", "REGENERATIVE\nORGANIC", "regenerative", true);
        }
        if (containsAny(normalized, "certified regenerative", "land to market verified")) {
            return certificate("regenerative", "Regenerative Certified", "REGENERATIVE\nCERTIFIED", "regenerative", true);
        }

        if (containsAny(normalized, "certified humane", "humane certified", "humane farm animal care")) {
            return certificate("animal_welfare", "Certified Humane", "CERTIFIED\nHUMANE", "animal_welfare", true);
        }
        if (containsAny(normalized, "animal welfare approved", "awa certified")) {
            return certificate("animal_welfare", "Animal Welfare Approved", "ANIMAL\nWELFARE\nAPPROVED", "animal_welfare", true);
        }
        if (containsAny(normalized, "global animal partnership", "gap certified", "gap step",
                "animal welfare certified")) {
            return certificate("animal_welfare", "Animal Welfare Certified", "ANIMAL\nWELFARE", "animal_welfare", true);
        }

        if (containsAny(normalized, "marine stewardship council", "msc certified", "msc")) {
            return certificate("msc", "MSC Certified Sustainable Seafood", "MSC\nCERTIFIED", "marine", true);
        }

        if (containsAny(normalized, "aquaculture stewardship council", "asc certified", "asc")) {
            return certificate("asc", "ASC Certified Seafood", "ASC\nCERTIFIED", "marine", true);
        }

        if (containsAny(normalized, "best aquaculture practices", "bap certified", "bap")) {
            return certificate("bap", "Best Aquaculture Practices", "BAP\nCERTIFIED", "marine", true);
        }

        if (containsAny(normalized, "dolphin safe", "dolphin safe certified")) {
            return certificate("dolphin_safe", "Dolphin Safe Certified", "DOLPHIN\nSAFE", "marine", true);
        }

        if (containsAny(normalized, "whole grain council", "whole grain stamp")) {
            return certificate("whole_grain", "Whole Grain Council", "WHOLE GRAIN\nSTAMP", "whole_grain", true);
        }

        if (containsAny(normalized, "keto certified", "certified keto", "ketogenic certified")) {
            return certificate("keto", "Keto Certified", "KETO\nCERTIFIED", "keto", true);
        }

        if (containsAny(normalized, "paleo certified", "certified paleo", "paleo foundation")) {
            return certificate("paleo", "Paleo Certified", "PALEO\nCERTIFIED", "paleo", true);
        }

        if (containsAny(normalized, "heart check certified", "american heart association heart check",
                "heart check mark")) {
            return certificate("heart_check", "Heart-Check Certified", "HEART\nCHECK", "heart_check", true);
        }

        if (containsAny(normalized, "nsf certified for sport", "informed sport certified",
                "informed choice certified")) {
            return certificate("sport_certified", "Sport Certified", "SPORT\nCERTIFIED", "sport", true);
        }

        if (containsAny(normalized, "green dot", "the green dot", "point vert", "der grune punkt",
                "der gruene punkt")) {
            return certificate("green_dot", "Green Dot", "GREEN\nDOT", "green_dot", true);
        }

        if (containsAny(normalized, "triman", "info tri", "info tri point vert")) {
            return certificate("triman", "Triman", "TRIMAN", "triman", true);
        }

        if (containsAny(normalized, "recyclable packaging", "recyclable", "recycling",
                "recycle", "mobius loop", "widely recycled")) {
            return certificate("recyclable", "Recyclable Packaging", "RECYCLABLE", "recycling", false);
        }

        if (containsAny(normalized, "fsc", "forest stewardship council", "fsc mix",
                "fsc recycled", "fsc 100")) {
            return certificate("fsc", "FSC Certified", "FSC", "forest", true);
        }

        if (containsAny(normalized, "pefc", "programme for the endorsement of forest certification")) {
            return certificate("pefc", "PEFC Certified", "PEFC", "forest", true);
        }

        if (containsAny(normalized, "compostable", "home compostable", "industrially compostable",
                "bpi compostable", "ok compost", "tuv austria ok compost")) {
            return certificate("compostable", "Compostable Packaging", "COMPOSTABLE", "compostable", true);
        }

        if (containsAny(normalized, "plastic free", "plastic free trust mark")) {
            return certificate("plastic_free", "Plastic Free", "PLASTIC\nFREE", "plastic_free", true);
        }

        if (containsAny(normalized, "carbon neutral", "climate neutral", "carbon trust",
                "carbon neutral certified", "climate neutral certified")) {
            return certificate("carbon_neutral", "Carbon/Climate Certified", "CARBON\nNEUTRAL", "carbon", true);
        }

        if (containsAny(normalized, "eu ecolabel", "ecolabel", "environmental choice",
                "blue angel", "natureplus")) {
            return certificate("ecolabel", "Eco Label", "ECO\nLABEL", "ecolabel", true);
        }

        if (containsAny(normalized, "certified", "certification", "verified", "approved")) {
            String pretty = prettify(rawLabel);
            return certificate("generic_" + normalized.replace(' ', '_'), pretty, pretty.toUpperCase(Locale.US), "generic", true);
        }

        return null;
    }

    private static ProductCertificate certificate(String key, String displayName, String badgeText, String styleKey, boolean specific) {
        return new ProductCertificate(key, displayName, badgeText, styleKey, specific);
    }

    private static List<String> splitLabels(String labels) {
        List<String> result = new ArrayList<>();
        if (labels == null || labels.trim().isEmpty()) {
            return result;
        }

        String[] parts = labels.split("[,;|\\n]+");
        for (String part : parts) {
            String cleaned = cleanLabel(part);
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private static String cleanLabel(String label) {
        if (label == null) return "";
        String cleaned = label.trim();
        if (cleaned.matches("^[a-z]{2}:.+")) {
            cleaned = cleaned.substring(3);
        }
        return cleaned.replace('_', ' ').replace('-', ' ').trim();
    }

    private static String prettify(String rawLabel) {
        String normalized = cleanLabel(rawLabel).replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();
        for (String word : normalized.split(" ")) {
            if (word.isEmpty()) continue;
            if (builder.length() > 0) builder.append(" ");
            String lowerWord = word.toLowerCase(Locale.US);
            if ("usda".equals(lowerWord)
                    || "gmo".equals(lowerWord)
                    || "ou".equals(lowerWord)
                    || "eu".equals(lowerWord)
                    || "msc".equals(lowerWord)
                    || "asc".equals(lowerWord)
                    || "gfco".equals(lowerWord)) {
                builder.append(lowerWord.toUpperCase(Locale.US));
            } else {
                builder.append(word.substring(0, 1).toUpperCase(Locale.US));
                if (word.length() > 1) {
                    builder.append(lowerWord.substring(1));
                }
            }
        }
        return builder.toString();
    }

    private static String normalize(String label) {
        return cleanLabel(label)
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static boolean containsAny(String value, String... terms) {
        String searchable = " " + value + " ";
        for (String term : terms) {
            if (searchable.contains(" " + normalize(term) + " ")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(value);
        }
        return builder.toString();
    }
}
