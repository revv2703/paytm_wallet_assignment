package com.paytm.wallet.common.util;

public class Constants {
    public static final String WALLET_ID_PREFIX = "WALLET_";
    public static final String TRANSFER_ID_PREFIX = "TRANSFER_";


    public static final String WALLET_STATUS_ACTIVE = "ACTIVE";
    public static final String WALLET_NOT_FOUND = "Wallet not found";
    public static final String WALLET_ID_REQUIRED = "Wallet id is required";
    public static final String USER_ID_REQUIRED = "User id is required";
    public static final String WALLET_ALREADY_EXISTS = "Wallet already exists for the user";
    public static final String WALLET_CREATION_FAILED = "Wallet creation failed";


    public static final String TRANSFER_CREATION_FAILED = "Transfer creation failed";
    public static final String TRANSFER_NOT_FOUND = "Transfer not found";
    public static final String TRANSFER_INSUFFICIENT_BALANCE = "Insufficient balance for transfer";
    public static final String TRANSFER_INVALID_AMOUNT = "Invalid transfer amount";
    public static final String TRANSFER_ID_REQUIRED = "Transfer id is required";
    public static final String IDEMPOTENCY_KEY_REQUIRED = "Idempotency key is required";
    public static final String TRANSFER_NOT_FOUND_FOR_IDEMPOTENCY_KEY = "No transfer found for idempotency key";
    public static final String TRANSFER_COMPLETED = "COMPLETED";
    public static final String TRANSFER_DECLINED = "DECLINED";
    public static final String TRANSFER_IDEMPOTENCY_KEY_CONFLICT = "Idempotency key conflict: different payload for the same key";
    public static final String TRANSFER_SENDER_RECEIVER_SAME = "Sender and receiver wallet must be different";
    public static final String TRANSFER_REQUEST_REQUIRED = "Transfer request is required";


    public static final String MESSAGE = "message";
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String UNEXPECTED_SERVER_ERROR = "Unexpected server error";
}
