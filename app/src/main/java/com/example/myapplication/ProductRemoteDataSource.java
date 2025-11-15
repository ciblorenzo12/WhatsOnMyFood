package com.example.myapplication;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProductRemoteDataSource {

    private final OpenFoodFactsApiClient apiClient;

    public ProductRemoteDataSource(File cacheDir) {
        this.apiClient = new OpenFoodFactsApiClient(cacheDir);
    }

    public ProductWithDetails getProductByBarcode(String barcode) throws IOException {
        ProductResponse response = apiClient.getProduct(barcode);
        if (response == null || response.status == 0 || response.product == null || response.product.productName == null) {
            return null;
        }

        ProductResponse.ProductData productData = response.product;
        Product product = new Product(
                barcode,
                productData.productName,
                productData.brands,
                productData.quantity,
                productData.imageUrl,
                productData.labels,
                productData.packaging,
                productData.categories,
                productData.servingSize,
                productData.nutriscoreGrade,
                productData.novaGroup,
                productData.ecoscoreGrade
        );

        Nutriments nutriments = null;
        if (productData.nutriments != null) {
            ProductResponse.NutrimentsData nutrimentsData = productData.nutriments;
            nutriments = new Nutriments(barcode, nutrimentsData.energy, nutrimentsData.fat, nutrimentsData.saturatedFat, nutrimentsData.carbohydrates, nutrimentsData.sugars, nutrimentsData.proteins, nutrimentsData.salt, nutrimentsData.fiber);
        }

        List<Ingredient> ingredients = new ArrayList<>();
        if (productData.ingredients != null) {
            for (ProductResponse.IngredientsData ingredientData : productData.ingredients) {
                if (ingredientData.text != null) {
                    ingredients.add(new Ingredient(barcode, ingredientData.text, ingredientData.rank));
                }
            }
        }

        ProductWithDetails productWithDetails = new ProductWithDetails();
        productWithDetails.product = product;
        productWithDetails.nutriments = nutriments;
        productWithDetails.ingredients = ingredients;

        return productWithDetails;
    }
}
