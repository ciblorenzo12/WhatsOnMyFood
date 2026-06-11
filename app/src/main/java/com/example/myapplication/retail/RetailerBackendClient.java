package com.example.myapplication.retail;

import java.util.List;

public interface RetailerBackendClient {
    List<RetailerAvailability> fetchAvailability(RetailerProductQuery query) throws Exception;
    List<RetailerAlternative> fetchAlternatives(RetailerProductQuery query) throws Exception;
}

