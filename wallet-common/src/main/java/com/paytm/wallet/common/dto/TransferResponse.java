package com.paytm.wallet.common.dto;

public record TransferResponse(String id, String from, String to, Long amountPaise, String status, String idempotencyKey) {
}
