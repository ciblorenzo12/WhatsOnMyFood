package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AiExplanationResponseValidatorTest {

    @Test
    public void acceptsCompleteSourceBackedExplanation() {
        String json = "{"
                + "\"verdict\":\"HEALTHY\","
                + "\"summary\":\"<b>Why this rating</b><br>Whole grain oats are the main ingredient. "
                + "<b>Portion guidance</b><br>Use the serving printed on the package. "
                + "<b>Fact check</b><br>The label guidance was checked against the cited source.\","
                + "\"findings\":[{\"rule\":\"Whole grain base\"}],"
                + "\"sources\":[{\"name\":\"FDA Nutrition Facts\",\"url\":\"https://www.fda.gov/food\"}]"
                + "}";

        AiExplanationResponseValidator.Result result = AiExplanationResponseValidator.validate(json);

        assertTrue(result.usable);
        assertTrue(result.json.contains("Why this rating"));
    }

    @Test
    public void rejectsMalformedIncompleteAndUnverifiedResponses() {
        assertFalse(AiExplanationResponseValidator.validate("not-json").usable);
        assertFalse(AiExplanationResponseValidator.validate(
                "{\"verdict\":\"HEALTHY\",\"summary\":\"Too short\",\"findings\":[],"
                        + "\"sources\":[{\"url\":\"https://www.fda.gov\"}]}"
        ).usable);
        assertFalse(AiExplanationResponseValidator.validate(
                "{\"verdict\":\"HEALTHY\","
                        + "\"summary\":\"This explanation is long enough to read but has no verified web source.\","
                        + "\"findings\":[],\"sources\":[{\"url\":\"javascript:alert(1)\"}]}"
        ).usable);
    }
}
