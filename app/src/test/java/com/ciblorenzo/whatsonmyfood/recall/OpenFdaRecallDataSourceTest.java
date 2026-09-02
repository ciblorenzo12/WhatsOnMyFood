package com.ciblorenzo.whatsonmyfood.recall;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OpenFdaRecallDataSourceTest {

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

        FoodRecallDataset dataset = OpenFdaRecallDataSource.parseResponse(response);

        assertEquals("2026-08-19", dataset.sourceUpdatedAt);
        assertEquals(1, dataset.records.size());
        assertEquals("F-0001-2026", dataset.records.get(0).recallNumber);
        assertEquals("UPC 123456789012", dataset.records.get(0).codeInfo);
    }

    @Test
    public void parserAcceptsAValidEmptyResultSet() throws Exception {
        FoodRecallDataset dataset = OpenFdaRecallDataSource.parseResponse(
                "{\"meta\":{\"last_updated\":\"2026-08-19\"},\"results\":[]}"
        );

        assertEquals(0, dataset.records.size());
        assertEquals("2026-08-19", dataset.sourceUpdatedAt);
    }
}
