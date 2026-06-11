package com.example.myapplication;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProductCertificateParserTest {

    @Test
    public void findsCommonCertificatesFromLabels() {
        List<ProductCertificate> certificates = ProductCertificateParser.findCertificates(
                "Organic, Non-GMO Project Verified, Orthodox Union Kosher"
        );

        assertEquals(3, certificates.size());
        assertEquals("Organic Certified", certificates.get(0).displayName);
        assertEquals("Non-GMO Project Verified", certificates.get(1).displayName);
        assertEquals("Orthodox Union Kosher", certificates.get(2).displayName);
    }

    @Test
    public void ignoresNonCertificateLabelsButKeepsPackagingMarks() {
        List<ProductCertificate> certificates = ProductCertificateParser.findCertificates(
                "No additives, Recyclable packaging, Made in USA"
        );

        assertEquals(1, certificates.size());
        assertEquals("Recyclable Packaging", certificates.get(0).displayName);
    }

    @Test
    public void deduplicatesGenericAndSpecificCertificates() {
        List<ProductCertificate> certificates = ProductCertificateParser.findCertificates(
                "en:organic, en:usda-organic, en:fair-trade-certified"
        );

        assertEquals(2, certificates.size());
        assertEquals("USDA Organic", certificates.get(0).displayName);
        assertEquals("Fair Trade Certified", certificates.get(1).displayName);
    }

    @Test
    public void findsExpandedFoodCertificationFamilies() {
        List<ProductCertificate> certificates = ProductCertificateParser.findCertificates(
                "GFCO, IFANCA Halal, Certified Vegan, Certified Plant Based, "
                        + "Regenerative Organic Certified, Certified Humane, MSC Certified, "
                        + "Keto Certified, Heart Check Certified"
        );

        assertEquals(9, certificates.size());
        assertEquals("Certified Gluten-Free", certificates.get(0).displayName);
        assertEquals("Halal Certified", certificates.get(1).displayName);
        assertEquals("Certified Vegan", certificates.get(2).displayName);
        assertEquals("Certified Plant-Based", certificates.get(3).displayName);
        assertEquals("Regenerative Organic Certified", certificates.get(4).displayName);
        assertEquals("Certified Humane", certificates.get(5).displayName);
        assertEquals("MSC Certified Sustainable Seafood", certificates.get(6).displayName);
        assertEquals("Keto Certified", certificates.get(7).displayName);
        assertEquals("Heart-Check Certified", certificates.get(8).displayName);
    }

    @Test
    public void findsOpenFoodFactsNoGlutenAndNonGmoProjectLabels() {
        List<ProductCertificate> certificates = ProductCertificateParser.findCertificates(
                "No gluten, No GMOs, Certified gluten-free, Non GMO project"
        );

        assertEquals(2, certificates.size());
        assertEquals("Certified Gluten-Free", certificates.get(0).displayName);
        assertEquals("Non-GMO Project Verified", certificates.get(1).displayName);
    }

    @Test
    public void findsOpenFoodFactsPackagingAndEnvironmentalLabels() {
        List<ProductCertificate> certificates = ProductCertificateParser.findCertificates(
                "Green Dot, Triman, FSC Mix, BPI Compostable, Carbon Neutral Certified, EU Ecolabel"
        );

        assertEquals(6, certificates.size());
        assertEquals("Green Dot", certificates.get(0).displayName);
        assertEquals("Triman", certificates.get(1).displayName);
        assertEquals("FSC Certified", certificates.get(2).displayName);
        assertEquals("Compostable Packaging", certificates.get(3).displayName);
        assertEquals("Carbon/Climate Certified", certificates.get(4).displayName);
        assertEquals("Eco Label", certificates.get(5).displayName);
    }
}
