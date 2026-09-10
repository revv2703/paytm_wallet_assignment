package com.paytm.wallet.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTransferRequest(
        @NotBlank(message = "Sender wallet id is required") String from,
        @NotBlank(message = "Receiver wallet id is required") String to,
        @NotNull(message = "Amount in paise is required") @Min(value = 1, message = "Amount must be positive") Long amountPaise,
        @NotBlank(message = "Idempotency key is required") String idempotencyKey
) {
}
