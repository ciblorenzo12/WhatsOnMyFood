package com.ciblorenzo.whatsonmyfood.recall;

import java.io.IOException;

public final class FoodRecallServiceException extends IOException {
    public final boolean temporarilyUnavailable;

    FoodRecallServiceException(String message, boolean temporarilyUnavailable) {
        super(message);
        this.temporarilyUnavailable = temporarilyUnavailable;
    }
}
