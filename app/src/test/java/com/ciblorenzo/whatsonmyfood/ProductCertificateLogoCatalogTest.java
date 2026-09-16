package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ProductCertificateLogoCatalogTest {
    @Test public void dietaryClaimsAndUnnamedCertifiersDoNotAcquireOfficialMarks() {
        for (String label : new String[]{"Vegan", "Vegetarian", "Gluten-Free", "Kosher",
                "Organic", "Non-GMO", "Certified Vegan", "Certified Gluten-Free",
                "Fairtrade International", "Fair For Life", "NSF Gluten Free", "IFANCA Halal",
                "Vegan Society", "OU Dairy", "OU-D", "OU-DE", "OU-P"}) {
            assertNull(label, ProductCertificateLogoCatalog.findLogoKey(label));
        }
    }

    @Test public void explicitOrganizationsResolveToTheirOwnArtwork() {
        assertEquals("gfco", ProductCertificateLogoCatalog.findLogoKey("en:gfco"));
        assertEquals("vegan_action", ProductCertificateLogoCatalog.findLogoKey("Vegan Action"));
        assertEquals("ou_kosher", ProductCertificateLogoCatalog.findLogoKey("Orthodox Union Kosher"));
        assertEquals("usda_organic", ProductCertificateLogoCatalog.findLogoKey("en:usda-organic"));
        assertEquals("non_gmo_project_verified",
                ProductCertificateLogoCatalog.findLogoKey("Non-GMO Project Verified"));
        assertEquals("fair_trade", ProductCertificateLogoCatalog.findLogoKey("Fair Trade USA"));
    }

    @Test public void explicitCertificationWinsRegardlessOfLabelOrder() {
        for (String labels : new String[]{"Gluten-Free, GFCO, Certified Gluten-Free",
                "GFCO, Certified Gluten-Free, Gluten-Free"}) {
            List<ProductCertificate> certificates = ProductCertificateParser.findCertificates(labels);
            assertEquals(1, certificates.size());
            assertEquals("gfco", certificates.get(0).logoKey);
            assertEquals("GFCO", certificates.get(0).sourceLabel);
        }
    }

    @Test public void parserKeepsOriginalOrganizationForTextFallback() {
        ProductCertificate certificate = ProductCertificateParser.findCertificates("Vegan Society").get(0);
        assertEquals("Vegan Society", certificate.sourceLabel);
        assertNull(certificate.logoKey);
        assertNull(ProductCertificateParser.findCertificates("Verified Non-GMO").get(0).logoKey);
    }
}
