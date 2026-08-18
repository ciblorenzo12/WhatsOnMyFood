package com.ciblorenzo.whatsonmyfood.retail;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import okhttp3.HttpUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RetailerBackendContractTest {

    @Test
    public void alternativesRequestIncludesProductIdentifiersCategoryAndLocation() {
        HttpRetailerBackendClient client = new HttpRetailerBackendClient("https://retailer.example");
        RetailerProductQuery query = new RetailerProductQuery(
                "012345678905", "Example cereal", "Example brand", "Breakfast cereals",
                "32789", 28.6, -81.3);

        HttpUrl url = client.buildUrl(query, "alternatives");

        assertEquals("/api/retail/products/012345678905/alternatives", url.encodedPath());
        assertEquals("Example cereal", url.queryParameter("productName"));
        assertEquals("Example brand", url.queryParameter("brand"));
        assertEquals("Breakfast cereals", url.queryParameter("category"));
        assertEquals("32789", url.queryParameter("zip"));
        assertEquals("28.6", url.queryParameter("lat"));
        assertEquals("-81.3", url.queryParameter("lng"));
        assertTrue(HttpRetailerBackendClient.CALL_TIMEOUT_SECONDS <= 10);
    }

    @Test
    public void androidBuildDoesNotDeclareRetailerCredentials() throws Exception {
        String buildFile = readProjectFile("build.gradle.kts").toUpperCase();

        assertFalse(buildFile.contains("WALMART_CONSUMER_ID"));
        assertFalse(buildFile.contains("WALMART_PRIVATE_KEY"));
        assertFalse(buildFile.contains("AMAZON_SP_API_CLIENT_SECRET"));
        assertFalse(buildFile.contains("AMAZON_SP_API_REFRESH_TOKEN"));
        assertTrue(buildFile.contains("RETAILER_BACKEND_BASE_URL"));
    }

    private static String readProjectFile(String relativePath) throws Exception {
        Path modulePath = Path.of(relativePath);
        Path repositoryPath = Path.of("app").resolve(relativePath);
        Path selected = Files.exists(modulePath) ? modulePath : repositoryPath;
        return new String(Files.readAllBytes(selected), StandardCharsets.UTF_8);
    }
}
