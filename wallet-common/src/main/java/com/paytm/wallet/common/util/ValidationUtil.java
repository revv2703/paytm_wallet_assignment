package com.paytm.wallet.common.util;

public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
