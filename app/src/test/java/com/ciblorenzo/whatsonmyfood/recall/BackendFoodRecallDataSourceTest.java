package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class BackendFoodRecallDataSourceTest {

    @Test
    public void requestUsesProtectedBackendWithoutProviderKey() {
        BackendFoodRecallDataSource source = new BackendFoodRecallDataSource(
                new OkHttpClient(),
                "https://backend.example.test",
                "application-token"
        );

        Request request = source.buildRequest(product());

        assertEquals("https://backend.example.test/v1/food-recalls", request.url().newBuilder().query(null).build().toString());
        assertEquals("012345678905", request.url().queryParameter("barcode"));
        assertEquals("Oat Cereal", request.url().queryParameter("productName"));
        assertEquals("Sample Foods", request.url().queryParameter("brand"));
        assertEquals("application-token", request.header("X-APP-TOKEN"));
        assertNull(request.url().queryParameter("api_key"));
        assertFalse(request.toString().contains("OPENFDA"));
    }

    @Test
    public void parserNormalizesOfficialEnforcementFields() throws Exception {
        String response = "{\"meta\":{\"last_updated\":\"2026-08-19\"},\"results\":[{"
                + "\"recall_number\":\"F-0001-2026\","
                + "\"product_description\":\"Cookie dough ice cream\","
                + "\"recalling_firm\":\"Example Foods\","
                + "\"classification\":\"Class I\","
                + "\"reason_for_recall\":\"Undeclared allergen\","
                + "\"code_info\":\"UPC 123456789012\","
                + "\"report_date\":\"20260819\","
                + "\"status\":\"Ongoing\"}]}";

        FoodRecallDataset dataset = BackendFoodRecallDataSource.parseResponse(response);

        assertEquals("2026-08-19", dataset.sourceUpdatedAt);
        assertEquals(1, dataset.records.size());
        assertEquals("F-0001-2026", dataset.records.get(0).recallNumber);
        assertEquals("UPC 123456789012", dataset.records.get(0).codeInfo);
    }

    @Test
    public void parserAcceptsAValidEmptyResultSet() throws Exception {
        FoodRecallDataset dataset = BackendFoodRecallDataSource.parseResponse(
                "{\"meta\":{\"last_updated\":\"2026-08-19\"},\"results\":[]}"
        );

        assertEquals(0, dataset.records.size());
        assertEquals("2026-08-19", dataset.sourceUpdatedAt);
    }

    private static Product product() {
        return new Product(
                "012345678905", "Oat Cereal", "Sample Foods", "12 oz", "", "", "", "", "",
                "", "", ""
        );
    }
}
