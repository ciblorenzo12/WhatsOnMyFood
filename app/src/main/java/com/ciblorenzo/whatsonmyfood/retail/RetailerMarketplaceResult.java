package com.ciblorenzo.whatsonmyfood.retail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RetailerMarketplaceResult {
    public final List<RetailerAvailability> availability;
    public final List<RetailerAlternative> alternatives;
    public final RetailerEndpointResult.SourceMode sourceMode;

    public RetailerMarketplaceResult(List<RetailerAvailability> availability,
                                     List<RetailerAlternative> alternatives,
                                     RetailerEndpointResult.SourceMode sourceMode) {
        this.availability = immutableCopy(availability);
        this.alternatives = immutableCopy(alternatives);
        this.sourceMode = sourceMode == null ? RetailerEndpointResult.SourceMode.UNKNOWN : sourceMode;
    }

    public boolean isEmpty() {
        return alternatives.isEmpty();
    }

    static RetailerEndpointResult.SourceMode combineSources(
            RetailerEndpointResult.SourceMode first,
            RetailerEndpointResult.SourceMode second,
            boolean hasResults) {
        if (!hasResults) return RetailerEndpointResult.SourceMode.EMPTY;
        boolean live = first == RetailerEndpointResult.SourceMode.LIVE
                || second == RetailerEndpointResult.SourceMode.LIVE
                || first == RetailerEndpointResult.SourceMode.MIXED
                || second == RetailerEndpointResult.SourceMode.MIXED;
        boolean mock = first == RetailerEndpointResult.SourceMode.MOCK
                || second == RetailerEndpointResult.SourceMode.MOCK
                || first == RetailerEndpointResult.SourceMode.MIXED
                || second == RetailerEndpointResult.SourceMode.MIXED;
        if (live && mock) return RetailerEndpointResult.SourceMode.MIXED;
        if (live) return RetailerEndpointResult.SourceMode.LIVE;
        if (mock) return RetailerEndpointResult.SourceMode.MOCK;
        return RetailerEndpointResult.SourceMode.UNKNOWN;
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return values == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}
