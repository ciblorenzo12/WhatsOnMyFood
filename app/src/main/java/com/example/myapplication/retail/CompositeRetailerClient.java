package com.example.myapplication.retail;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates multiple retailer clients and merges their results.
 */
public class CompositeRetailerClient implements RetailerBackendClient {

    private final List<RetailerBackendClient> clients;

    public CompositeRetailerClient() {
        this.clients = new ArrayList<>();
        // Add individual providers
        clients.add(new KrogerRetailerClient());
        clients.add(new InstacartRetailerClient());
        // We keep the mock for others (Amazon, Walmart, Target) until they are implemented
        clients.add(new LegacyMockRetailerClient());
    }

    @Override
    public List<RetailerAvailability> fetchAvailability(RetailerProductQuery query) throws Exception {
        List<RetailerAvailability> allResults = new ArrayList<>();
        for (RetailerBackendClient client : clients) {
            try {
                List<RetailerAvailability> results = client.fetchAvailability(query);
                if (results != null) {
                    allResults.addAll(results);
                }
            } catch (Exception e) {
                // Log error but continue with other clients
                e.printStackTrace();
            }
        }
        return allResults;
    }

    @Override
    public List<RetailerAlternative> fetchAlternatives(RetailerProductQuery query) throws Exception {
        List<RetailerAlternative> allResults = new ArrayList<>();
        for (RetailerBackendClient client : clients) {
            try {
                List<RetailerAlternative> results = client.fetchAlternatives(query);
                if (results != null) {
                    allResults.addAll(results);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return allResults;
    }
}
