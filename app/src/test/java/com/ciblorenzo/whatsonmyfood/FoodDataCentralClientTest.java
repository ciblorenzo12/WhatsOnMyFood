package com.ciblorenzo.whatsonmyfood;

import org.junit.Test;
import java.io.IOException;
import java.net.SocketTimeoutException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import static org.junit.Assert.*;

public class FoodDataCentralClientTest {
    private static final String BARCODE = "012345678905";
    private static final String SUCCESS = "{\"status\":1,\"provenance\":{"
            + "\"fdcId\":123,\"gtinUpc\":\"00012345678905\",\"nutrientBasis\":\"per100g\"},"
            + "\"product\":{\"product_name\":\"Example cereal\",\"ingredients_text\":\"Oats, sugar\","
            + "\"nutriments\":{\"energy-kcal_100g\":350,\"sugars_100g\":18,"
            + "\"added-sugars_100g\":12,\"sodium_100g\":0.4,\"salt_100g\":1}}}";

    @Test public void sendsBarcodeAndAppTokenOnlyToProtectedBackend() throws Exception {
        Request request = new FoodDataCentralClient(new OkHttpClient(),
                "https://backend.example.test/proxy/", "app-token").buildRequest(BARCODE);
        assertEquals("https://backend.example.test/proxy/v1/food-data/usda?barcode=" + BARCODE,
                request.url().toString());
        assertEquals("app-token", request.header("X-APP-TOKEN"));
        assertNull(request.url().queryParameter("api_key"));
        assertFalse(request.url().host().contains("usda"));
    }

    @Test public void preservesDistinctSugarAmountsAndMissingValues() throws Exception {
        ProductResponse result = FoodDataCentralClient.parseResponse(SUCCESS, BARCODE);
        assertEquals("USDA FoodData Central", result.source);
        assertEquals(350, result.product.nutriments.energy, 0.0001);
        assertEquals(18, result.product.nutriments.sugars, 0.0001);
        assertEquals(12, result.product.nutriments.addedSugars, 0.0001);
        assertEquals(0.4, result.product.nutriments.sodium, 0.0001);
        assertNull(result.product.nutriments.saturatedFat);
        assertNull(result.product.novaGroup);
        assertNull(result.product.nutriscoreGrade);
    }

    @Test public void missingAddedSugarIsNotZero() throws Exception {
        ProductResponse result = FoodDataCentralClient.parseResponse(
                SUCCESS.replace("\"added-sugars_100g\":12,", ""), BARCODE);
        assertNull(result.product.nutriments.addedSugars);
        assertEquals(18, result.product.nutriments.sugars, 0.0001);
    }

    @Test public void doesNotRelabelVolumeOrUnknownNutritionAsPer100g() throws Exception {
        for (String basis : new String[] {"per100ml_not_converted", "unknown", ""}) {
            ProductResponse result = FoodDataCentralClient.parseResponse(
                    SUCCESS.replace("per100g", basis), BARCODE);
            assertNotNull(result.product);
            assertNull(result.product.nutriments);
        }
    }

    @Test public void validNoMatchReturnsNull() throws Exception {
        assertNull(FoodDataCentralClient.parseResponse("{\"status\":0}", BARCODE));
    }

    @Test public void malformedOrIncompletePayloadsFailSafely() {
        for (String body : new String[] {"not JSON", "[]", "{}", "{\"status\":1}",
                "{\"status\":\"0\"}", "{\"status\":2}", SUCCESS.replace("\"fdcId\":123", "\"fdcId\":0"),
                SUCCESS.replace("Example cereal", ""), SUCCESS.replace("\"sugars_100g\":18", "\"sugars_100g\":-1"),
                SUCCESS.replace("\"sugars_100g\":18", "\"sugars_100g\":\"NaN\"")}) {
            assertThrows(IOException.class, () -> FoodDataCentralClient.parseResponse(body, BARCODE));
        }
    }

    @Test public void wrongOrPartialBarcodeIsNeverAccepted() {
        for (String other : new String[] {"036000291452", "678905", "012345678906"}) {
            assertThrows(IOException.class, () -> FoodDataCentralClient.parseResponse(
                    SUCCESS.replace("00012345678905", other), BARCODE));
        }
    }

    @Test public void invalidBarcodeStopsBeforeNetwork() {
        FoodDataCentralClient source = new FoodDataCentralClient(new OkHttpClient(),
                "https://backend.example.test", "token");
        assertThrows(IOException.class, () -> source.buildRequest("123456"));
        assertThrows(IOException.class, () -> source.buildRequest("012345678906"));
    }

    @Test public void remotePlaintextAndMissingTokenAreRejected() {
        assertThrows(IOException.class, () -> new FoodDataCentralClient(new OkHttpClient(),
                "http://backend.example.test", "token").buildRequest(BARCODE));
        assertThrows(IOException.class, () -> new FoodDataCentralClient(new OkHttpClient(),
                "https://backend.example.test", "").buildRequest(BARCODE));
        assertThrows(IOException.class, () -> new FoodDataCentralClient(new OkHttpClient(),
                "https://backend.example.test", "token\ninvalid").buildRequest(BARCODE));
    }

    @Test public void androidBuildDoesNotExposeAUsdaProviderKeyField() {
        assertThrows(NoSuchFieldException.class, () -> BuildConfig.class.getField("FDC_API_KEY"));
    }

    @Test public void backendErrorsRemainFailuresAndDoNotExposeBody() {
        for (int status : new int[] {401, 404, 429, 502, 503, 504}) {
            IOException error = assertThrows(IOException.class, () -> source(status, "secret-key")
                    .getProduct(BARCODE));
            assertFalse(error.getMessage().contains("secret-key"));
        }
    }

    @Test public void protectedResponseIsParsed() throws Exception {
        assertEquals("Example cereal", source(200, SUCCESS).getProduct(BARCODE).product.productName);
    }

    @Test public void oversizedResponseIsRejected() {
        assertThrows(IOException.class, () -> source(200, "x".repeat(512 * 1024 + 1)).getProduct(BARCODE));
    }

    @Test public void timeoutRemainsAnIOExceptionForTheFallbackChain() {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            throw new SocketTimeoutException("fixture timeout");
        }).build();
        assertThrows(IOException.class, () -> new FoodDataCentralClient(
                client, "https://backend.example.test", "token").getProduct(BARCODE));
    }

    @Test public void totalTimeoutAndRedirectPolicyAreBounded() {
        OkHttpClient client = FoodDataCentralClient.defaultHttpClient();
        assertEquals(18000, client.callTimeoutMillis());
        assertFalse(client.followRedirects());
        assertFalse(client.followSslRedirects());
        assertFalse(client.retryOnConnectionFailure());
    }

    private static FoodDataCentralClient source(int code, String body) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain ->
                new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                        .code(code).message("Fixture").body(ResponseBody.create(body,
                                MediaType.get("application/json"))).build()).build();
        return new FoodDataCentralClient(client, "https://backend.example.test", "token");
    }
}
