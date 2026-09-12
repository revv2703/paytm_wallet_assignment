package com.paytm.wallet.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTransferRequest(
        @NotBlank(message = "Sender wallet id is required") @JsonProperty("from") String from,
        @NotBlank(message = "Receiver wallet id is required") @JsonProperty("to") String to,
        @NotNull(message = "Amount in paise is required") @Min(value = 1, message = "Amount must be positive") @JsonProperty("amount_paise") Long amountPaise,
        @NotBlank(message = "Idempotency key is required") @JsonProperty("idempotency_key") String idempotencyKey
) {
}
