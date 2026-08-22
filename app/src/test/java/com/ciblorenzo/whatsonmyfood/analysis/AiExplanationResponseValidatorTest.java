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
                + "\"ingredients\":[\"whole grain oats\"],"
                + "\"findings\":[{\"rule\":\"Whole grain base\",\"impact\":\"positive\","
                + "\"explanation\":\"Whole-grain oats are the first listed ingredient.\"}],"
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
                        + "\"ingredients\":[],\"sources\":[{\"url\":\"https://www.fda.gov\"}]}"
        ).usable);
        assertFalse(AiExplanationResponseValidator.validate(
                "{\"verdict\":\"HEALTHY\","
                        + "\"summary\":\"<b>Why this rating</b><br>This explanation is long enough to read. "
                        + "<b>Portion guidance</b><br>Use the package serving. "
                        + "<b>Fact check</b><br>This has no verified web source.\","
                        + "\"ingredients\":[],\"findings\":[],\"sources\":[{\"url\":\"javascript:alert(1)\"}]}"
        ).usable);
    }

    @Test
    public void rejectsHtmlAndUnsafeMedicalClaims() {
        assertFalse(AiExplanationResponseValidator.validate(
                "<!DOCTYPE html><title>Waiting for service</title>"
        ).usable);

        String unsafe = "{"
                + "\"verdict\":\"HEALTHY\","
                + "\"verdict_reason\":\"This product can cure diabetes.\","
                + "\"ingredients\":[\"oats\"],"
                + "\"summary\":\"<b>Why this rating</b><br>Whole-grain oats are the supplied ingredient. "
                + "<b>Portion guidance</b><br>Use the serving printed on the package. "
                + "<b>Fact check</b><br>The explanation was checked against the cited nutrition source.\","
                + "\"findings\":[{\"rule\":\"Whole grain base\",\"impact\":\"positive\","
                + "\"explanation\":\"Whole-grain oats are the first listed ingredient.\"}],"
                + "\"sources\":[{\"name\":\"FDA\",\"url\":\"https://www.fda.gov/food\"}]"
                + "}";

        assertFalse(AiExplanationResponseValidator.validate(unsafe).usable);
    }

    @Test
    public void rejectsReviewVerdictAndMissingIngredients() {
        String completeSummary = "<b>Why this rating</b><br>Whole-grain oats are the supplied ingredient. "
                + "<b>Portion guidance</b><br>Use the serving printed on the package. "
                + "<b>Fact check</b><br>The explanation was checked against the cited nutrition source.";
        String common = ",\"summary\":\"" + completeSummary + "\","
                + "\"findings\":[],\"sources\":[{\"name\":\"FDA\",\"url\":\"https://www.fda.gov/food\"}]}";

        assertFalse(AiExplanationResponseValidator.validate(
                "{\"verdict\":\"REVIEW\",\"ingredients\":[\"oats\"]" + common
        ).usable);
        assertFalse(AiExplanationResponseValidator.validate(
                "{\"verdict\":\"HEALTHY\",\"ingredients\":[]" + common
        ).usable);
    }

    @Test
    public void rejectsInternalScoringLanguage() {
        String response = "{"
                + "\"verdict\":\"NOT_HEALTHY\","
                + "\"verdict_reason\":\"The app's rules lowered the score.\","
                + "\"ingredients\":[\"sugar\",\"oats\"],"
                + "\"summary\":\"<b>Why this rating</b><br>The app's rules lowered the score because a predefined criterion was triggered. "
                + "<b>Portion guidance</b><br>Use the serving printed on the package. "
                + "<b>Fact check</b><br>The explanation was checked against the cited nutrition source.\","
                + "\"findings\":[{\"rule\":\"Added sugar\",\"impact\":\"warning\","
                + "\"explanation\":\"Sugar is listed near the beginning of the ingredient list.\"}],"
                + "\"sources\":[{\"name\":\"FDA\",\"url\":\"https://www.fda.gov/food\"}]"
                + "}";

        AiExplanationResponseValidator.Result result = AiExplanationResponseValidator.validate(response);

        assertFalse(result.usable);
        assertTrue(result.error.contains("food label"));
    }
}
