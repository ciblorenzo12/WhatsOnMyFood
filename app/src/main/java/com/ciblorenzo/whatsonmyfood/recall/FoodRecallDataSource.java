package com.ciblorenzo.whatsonmyfood.recall;

import com.ciblorenzo.whatsonmyfood.Product;

import java.io.IOException;

public interface FoodRecallDataSource {
    FoodRecallDataset search(Product product) throws IOException;
}
