package com.ciblorenzo.whatsonmyfood.retail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RetailerEndpointResult<T> {
    public enum SourceMode { LIVE, MOCK, MIXED, EMPTY, UNKNOWN }

    public final List<T> results;
    public final SourceMode sourceMode;
    public final String providerMode;

    public RetailerEndpointResult(List<T> results, SourceMode sourceMode, String providerMode) {
        this.results = results == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(results));
        this.sourceMode = sourceMode == null ? SourceMode.UNKNOWN : sourceMode;
        this.providerMode = providerMode == null ? "" : providerMode;
    }

    public static <T> RetailerEndpointResult<T> unknown(List<T> results) {
        SourceMode mode = results == null || results.isEmpty() ? SourceMode.EMPTY : SourceMode.UNKNOWN;
        return new RetailerEndpointResult<>(results, mode, "");
    }

    public static <T> RetailerEndpointResult<T> mock(List<T> results) {
        SourceMode mode = results == null || results.isEmpty() ? SourceMode.EMPTY : SourceMode.MOCK;
        return new RetailerEndpointResult<>(results, mode, "MockRetailerProvider");
    }

    public static SourceMode parseSourceMode(String value, boolean hasResults) {
        if (!hasResults) return SourceMode.EMPTY;
        if (value == null) return SourceMode.UNKNOWN;
        switch (value.trim().toLowerCase()) {
            case "live": return SourceMode.LIVE;
            case "mock": return SourceMode.MOCK;
            case "mixed": return SourceMode.MIXED;
            case "empty": return SourceMode.EMPTY;
            default: return SourceMode.UNKNOWN;
        }
    }
}
