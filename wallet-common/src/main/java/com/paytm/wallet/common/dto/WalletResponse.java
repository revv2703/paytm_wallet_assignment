package com.paytm.wallet.common.dto;

public record WalletResponse(String id, String userId, Long balancePaise, String status) {
}
