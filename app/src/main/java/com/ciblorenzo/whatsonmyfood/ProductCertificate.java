package com.ciblorenzo.whatsonmyfood;

public class ProductCertificate {
    public final String key;
    public final String displayName;
    public final String badgeText;
    public final String styleKey;
    public final boolean specific;

    ProductCertificate(String key, String displayName, String badgeText, String styleKey, boolean specific) {
        this.key = key;
        this.displayName = displayName;
        this.badgeText = badgeText;
        this.styleKey = styleKey;
        this.specific = specific;
    }
}
