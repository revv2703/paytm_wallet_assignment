package com.paytm.wallet.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateWalletRequest(
        @JsonProperty("user_id") String userId
) {
}
