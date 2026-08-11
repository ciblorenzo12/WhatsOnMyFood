package com.ciblorenzo.whatsonmyfood;

/** Interprets Room insert/delete counts without exposing database sentinel values to the UI. */
public final class PantryOperationResult {

    public enum SaveOutcome {
        SAVED,
        ALREADY_SAVED
    }

    public enum RemoveOutcome {
        REMOVED,
        ALREADY_REMOVED
    }

    private PantryOperationResult() {
    }

    public static SaveOutcome fromInsertRowId(long rowId) {
        return rowId == -1L ? SaveOutcome.ALREADY_SAVED : SaveOutcome.SAVED;
    }

    public static RemoveOutcome fromDeletedRows(int deletedRows) {
        return deletedRows > 0 ? RemoveOutcome.REMOVED : RemoveOutcome.ALREADY_REMOVED;
    }
}
