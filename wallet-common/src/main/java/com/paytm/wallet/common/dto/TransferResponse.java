package com.paytm.wallet.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransferResponse(
        @JsonProperty("id") String id,
        @JsonProperty("from") String from,
        @JsonProperty("to") String to,
        @JsonProperty("amount_paise") Long amountPaise,
        @JsonProperty("status") String status,
        @JsonProperty("idempotency_key") String idempotencyKey
) {
}
