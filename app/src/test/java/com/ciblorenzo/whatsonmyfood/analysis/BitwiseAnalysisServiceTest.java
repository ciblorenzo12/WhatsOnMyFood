package com.ciblorenzo.whatsonmyfood.analysis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BitwiseAnalysisServiceTest {

    @Test
    public void allowsProtectedSourceVerificationToFinish() {
        assertEquals(65_000L, BitwiseAnalysisService.ANALYSIS_TIMEOUT_MS);
    }
}
