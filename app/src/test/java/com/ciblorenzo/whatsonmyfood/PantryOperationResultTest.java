package com.ciblorenzo.whatsonmyfood;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PantryOperationResultTest {

    @Test
    public void insertedRow_isReportedAsSaved() {
        assertEquals(
                PantryOperationResult.SaveOutcome.SAVED,
                PantryOperationResult.fromInsertRowId(12L)
        );
    }

    @Test
    public void ignoredDuplicate_isReportedAsAlreadySaved() {
        assertEquals(
                PantryOperationResult.SaveOutcome.ALREADY_SAVED,
                PantryOperationResult.fromInsertRowId(-1L)
        );
    }

    @Test
    public void deletedRow_isReportedAsRemoved() {
        assertEquals(
                PantryOperationResult.RemoveOutcome.REMOVED,
                PantryOperationResult.fromDeletedRows(1)
        );
    }

    @Test
    public void missingRow_isReportedAsAlreadyRemoved() {
        assertEquals(
                PantryOperationResult.RemoveOutcome.ALREADY_REMOVED,
                PantryOperationResult.fromDeletedRows(0)
        );
    }
}
