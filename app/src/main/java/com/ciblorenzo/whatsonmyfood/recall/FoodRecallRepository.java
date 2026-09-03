package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import java.io.IOException;

public final class FoodRecallRepository {
    private final FoodRecallDataSource dataSource;
    private final FoodRecallMatcher matcher;

    public FoodRecallRepository() {
        this(new BackendFoodRecallDataSource(), new FoodRecallMatcher());
    }

    FoodRecallRepository(FoodRecallDataSource dataSource, FoodRecallMatcher matcher) {
        this.dataSource = dataSource;
        this.matcher = matcher;
    }

    public FoodRecallCheckResult check(Product product) throws IOException {
        return matcher.match(product, dataSource.search(product));
    }
}
