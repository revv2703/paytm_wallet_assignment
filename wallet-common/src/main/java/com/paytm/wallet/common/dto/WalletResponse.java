package com.paytm.wallet.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WalletResponse(
        @JsonProperty("id") String id,
        @JsonProperty("user_id") String userId,
        @JsonProperty("balance_paise") Long balancePaise,
        @JsonProperty("status") String status
) {
}
