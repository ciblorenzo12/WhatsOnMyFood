package com.ciblorenzo.whatsonmyfood;

public class ProductCertificate {
    public final String key;
    public final String displayName;
    public final String badgeText;
    public final String styleKey;
    public final boolean specific;
    public final String sourceLabel;
    public final String logoKey;

    ProductCertificate(String key, String displayName, String badgeText, String styleKey, boolean specific) {
        this(key, displayName, badgeText, styleKey, specific, displayName);
    }

    ProductCertificate(String key, String displayName, String badgeText, String styleKey,
                       boolean specific, String sourceLabel) {
        this.key = key;
        this.displayName = displayName;
        this.badgeText = badgeText;
        this.styleKey = styleKey;
        this.specific = specific;
        this.sourceLabel = sourceLabel;
        this.logoKey = ProductCertificateLogoCatalog.findLogoKey(sourceLabel);
    }
}
