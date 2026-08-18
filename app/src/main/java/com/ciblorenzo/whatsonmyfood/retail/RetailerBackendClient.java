package com.ciblorenzo.whatsonmyfood.retail;

import java.util.List;

public interface RetailerBackendClient {
    List<RetailerAvailability> fetchAvailability(RetailerProductQuery query) throws Exception;
    List<RetailerAlternative> fetchAlternatives(RetailerProductQuery query) throws Exception;

    default RetailerEndpointResult<RetailerAvailability> fetchAvailabilityResult(RetailerProductQuery query) throws Exception {
        return RetailerEndpointResult.unknown(fetchAvailability(query));
    }

    default RetailerEndpointResult<RetailerAlternative> fetchAlternativesResult(RetailerProductQuery query) throws Exception {
        return RetailerEndpointResult.unknown(fetchAlternatives(query));
    }
}
