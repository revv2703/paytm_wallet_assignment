package com.paytm.wallet.common.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequest(
        @NotBlank(message = "User id is required")
        String userId
) {
}
