#!/usr/bin/env python3

import concurrent.futures
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid

BASE_URL = os.getenv("BASE_URL", "http://localhost:8080").rstrip("/")

GET_OR_CREATE_COUNT = int(os.getenv("GET_OR_CREATE_COUNT", "50"))
IDEMPOTENCY_COUNT = int(os.getenv("IDEMPOTENCY_COUNT", "50"))
CONTENTION_COUNT = int(os.getenv("CONTENTION_COUNT", "200"))

# Globals initialized dynamically
WALLET_A = None
WALLET_B = None
TOKEN_A = None
TOKEN_B = None


def api_request(method, path, body=None, token=None, timeout=30):
    url = f"{BASE_URL}{path}"

    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-Correlation-ID": str(uuid.uuid4()),
    }
    if token is not None:
        headers["Authorization"] = f"Bearer {token}"

    encoded_body = None
    if body is not None:
        if isinstance(body, (int, float)):
            encoded_body = str(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        else:
            encoded_body = json.dumps(body).encode("utf-8")

    request = urllib.request.Request(
        url=url,
        method=method,
        headers=headers,
        data=encoded_body,
    )

    started = time.perf_counter()

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode("utf-8")

            try:
                response_body = json.loads(raw) if raw else {}
            except json.JSONDecodeError:
                response_body = raw

            return {
                "status": response.status,
                "body": response_body,
                "elapsed_ms": round(
                    (time.perf_counter() - started) * 1000,
                    2,
                ),
            }

    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8")

        try:
            response_body = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            response_body = raw

        return {
            "status": error.code,
            "body": response_body,
            "elapsed_ms": round(
                (time.perf_counter() - started) * 1000,
                2,
            ),
        }

    except Exception as error:
        return {
            "status": None,
            "body": {
                "error_type": type(error).__name__,
                "error": str(error),
            },
            "elapsed_ms": round(
                (time.perf_counter() - started) * 1000,
                2,
            ),
        }


def assert_http_success(result, operation):
    if result["status"] not in (200, 201):
        raise AssertionError(
            f"{operation} failed:\n"
            f"{json.dumps(result, indent=2, default=str)}"
        )


def wallet_id_from_response(result):
    body = result["body"]
    if isinstance(body, dict):
        return body.get("id") or body.get("wallet_id")
    return None


def balance_from_response(result):
    body = result["body"]
    if isinstance(body, dict):
        if "balance_paise" in body:
            return int(body["balance_paise"])
        if "balance" in body:
            return int(body["balance"])
    raise AssertionError(
        f"Could not find balance_paise in response:\n"
        f"{json.dumps(result, indent=2, default=str)}"
    )


def get_token(user_id):
    result = api_request("POST", "/auth/token", {"userId": user_id})
    if result["status"] == 200:
        return result["body"].get("token")
    return None


def get_balance(wallet_id, token):
    result = api_request("GET", f"/wallets/{wallet_id}", token=token)
    assert_http_success(result, f"GET /wallets/{wallet_id}")
    return balance_from_response(result)


def create_wallet(user_id, token):
    return api_request(
        "POST",
        "/wallets",
        {
            "user_id": user_id,
        },
        token=token,
    )


def credit_wallet(wallet_id, amount_paise, token):
    return api_request(
        "POST",
        f"/wallets/{wallet_id}/credit",
        body=amount_paise,
        token=token,
    )


def create_transfer(from_wallet, to_wallet, amount_paise, key, token):
    return api_request(
        "POST",
        "/transfers",
        {
            "from": from_wallet,
            "to": to_wallet,
            "amount_paise": amount_paise,
            "idempotency_key": key,
        },
        token=token,
    )


def test_health():
    print("[0/3] Health check")
    result = api_request("GET", "/health")
    if result["status"] != 200:
        raise AssertionError(
            f"GET /health failed:\n"
            f"{json.dumps(result, indent=2, default=str)}"
        )
    print("PASS: /health returned 200")


def test_concurrent_get_or_create():
    print("\n[1/3] Concurrent get-or-create")
    user_id = f"BURST-USER-{uuid.uuid4()}"
    token = get_token(user_id)

    with concurrent.futures.ThreadPoolExecutor(
            max_workers=GET_OR_CREATE_COUNT
    ) as executor:
        futures = [
            executor.submit(create_wallet, user_id, token)
            for _ in range(GET_OR_CREATE_COUNT)
        ]
        results = [future.result() for future in futures]

    failures = [
        result
        for result in results
        if result["status"] not in (200, 201)
    ]

    if failures:
        print("First failed response:")
        print(json.dumps(failures[0], indent=2, default=str))
        raise AssertionError(
            f"Expected all {GET_OR_CREATE_COUNT} requests to return "
            f"200 or 201, got statuses: "
            f"{[result['status'] for result in results]}"
        )

    wallet_ids = {
        wallet_id_from_response(result)
        for result in results
    }

    if None in wallet_ids:
        raise AssertionError(
            "At least one wallet response did not contain an id"
        )

    if len(wallet_ids) != 1:
        raise AssertionError(
            f"Expected exactly one wallet ID, got {wallet_ids}"
        )

    wallet_id = next(iter(wallet_ids))
    print(
        f"PASS: {GET_OR_CREATE_COUNT} concurrent requests returned "
        f"one wallet: {wallet_id}"
    )


def test_idempotency_retry_storm():
    print("\n[2/3] Idempotent retry storm")

    # Fetch fresh balance of A and B
    before_a = get_balance(WALLET_A, TOKEN_A)
    before_b = get_balance(WALLET_B, TOKEN_B)

    amount = 100
    key = f"IDEMPOTENCY-{uuid.uuid4()}"

    with concurrent.futures.ThreadPoolExecutor(
            max_workers=IDEMPOTENCY_COUNT
    ) as executor:
        futures = [
            executor.submit(
                create_transfer,
                WALLET_A,
                WALLET_B,
                amount,
                key,
                TOKEN_A,
            )
            for _ in range(IDEMPOTENCY_COUNT)
        ]
        results = [future.result() for future in futures]

    statuses = [result["status"] for result in results]

    # Successful transfer returns 201.
    # An idempotent duplicate may return 201 or 200.
    # A duplicate request with conflict returns 409.
    # An insufficient balance returns 422.
    allowed_statuses = (200, 201, 409, 422)

    unexpected = [
        result
        for result in results
        if result["status"] not in allowed_statuses
    ]

    if unexpected:
        print("Unexpected response:")
        print(json.dumps(unexpected[0], indent=2, default=str))
        raise AssertionError(
            f"Unexpected idempotency statuses: {statuses}"
        )

    after_a = get_balance(WALLET_A, TOKEN_A)
    after_b = get_balance(WALLET_B, TOKEN_B)

    successful = [
        result
        for result in results
        if result["status"] == 201
    ]

    declined = [
        result
        for result in results
        if result["status"] == 422
    ]

    conflicts = [
        result
        for result in results
        if result["status"] == 409
    ]

    # Case 1: insufficient balance.
    if before_a < amount:
        if after_a != before_a:
            raise AssertionError(
                f"Wallet A changed despite insufficient funds: "
                f"before={before_a}, after={after_a}"
            )
        if after_b != before_b:
            raise AssertionError(
                f"Wallet B changed despite insufficient funds: "
                f"before={before_b}, after={after_b}"
            )
        if successful:
            raise AssertionError(
                "A transfer succeeded despite insufficient funds:\n"
                f"{json.dumps(successful[0], indent=2, default=str)}"
            )
        if not declined:
            raise AssertionError(
                "Expected at least one 422 declined response for "
                "insufficient balance"
            )

        print(
            f"PASS: transfer was declined because Wallet A has "
            f"{before_a} paise but needs {amount} paise"
        )
        print(f"      declined responses: {len(declined)}")
        print(f"      HTTP 409 responses: {len(conflicts)}")
        print("PASS: balances remained unchanged and no overdraft occurred")

        # Reusing the same key with a different body must still return 409
        conflict_result = create_transfer(
            WALLET_A,
            WALLET_B,
            amount + 1,
            key,
            TOKEN_A,
        )
        if conflict_result["status"] != 409:
            raise AssertionError(
                "Reusing an idempotency key with a different body should "
                f"return 409, got:\n"
                f"{json.dumps(conflict_result, indent=2, default=str)}"
            )
        print("PASS: same idempotency key with a different body returned 409")
        return

    # Case 2: sufficient balance.
    if not successful:
        raise AssertionError(
            "Wallet A had sufficient funds, but no transfer succeeded. "
            f"Statuses: {statuses}"
        )

    if after_a != before_a - amount:
        raise AssertionError(
            f"Wallet A should be {before_a - amount}, got {after_a}"
        )

    if after_b != before_b + amount:
        raise AssertionError(
            f"Wallet B should be {before_b + amount}, got {after_b}"
        )

    response_ids = {
        result["body"].get("id") or result["body"].get("transfer_id")
        for result in successful
        if isinstance(result["body"], dict)
        and (result["body"].get("id") or result["body"].get("transfer_id")) is not None
    }

    if len(response_ids) != 1:
        raise AssertionError(
            f"Expected one transfer ID, got {response_ids}"
        )

    conflict_result = create_transfer(
        WALLET_A,
        WALLET_B,
        amount + 1,
        key,
        TOKEN_A,
    )
    if conflict_result["status"] != 409:
        raise AssertionError(
            "Reusing an idempotency key with a different body should "
            f"return 409, got:\n"
            f"{json.dumps(conflict_result, indent=2, default=str)}"
        )

    print(
        f"PASS: {IDEMPOTENCY_COUNT} concurrent requests caused "
        f"exactly one {amount}-paise transfer"
    )
    print(f"      successful responses: {len(successful)}")
    print(f"      HTTP 409 responses: {len(conflicts)}")
    print("PASS: same idempotency key with a different body returned 409")


def test_conservation_under_contention():
    print("\n[3/3] Conservation under contention")

    before_a = get_balance(WALLET_A, TOKEN_A)
    before_b = get_balance(WALLET_B, TOKEN_B)
    before_total = before_a + before_b

    def submit_transfer(index):
        if index % 2 == 0:
            source = WALLET_A
            destination = WALLET_B
            token = TOKEN_A
        else:
            source = WALLET_B
            destination = WALLET_A
            token = TOKEN_B

        return create_transfer(
            source,
            destination,
            1,
            f"CONTENTION-{uuid.uuid4()}",
            token,
        )

    with concurrent.futures.ThreadPoolExecutor(
            max_workers=CONTENTION_COUNT
    ) as executor:
        futures = [
            executor.submit(submit_transfer, index)
            for index in range(CONTENTION_COUNT)
        ]
        results = [future.result() for future in futures]

    after_a = get_balance(WALLET_A, TOKEN_A)
    after_b = get_balance(WALLET_B, TOKEN_B)
    after_total = after_a + after_b

    if after_a < 0 or after_b < 0:
        raise AssertionError(
            f"Negative balance detected: A={after_a}, B={after_b}"
        )

    if after_total != before_total:
        raise AssertionError(
            f"Conservation failed: before={before_total}, "
            f"after={after_total}"
        )

    unexpected = [
        result
        for result in results
        if result["status"] not in (200, 201, 409, 422)
    ]

    if unexpected:
        print("Unexpected contention response:")
        print(json.dumps(unexpected[0], indent=2, default=str))
        raise AssertionError(
            f"Unexpected contention status: {unexpected[0]['status']}"
        )

    successful = sum(
        result["status"] in (200, 201)
        for result in results
    )

    declined = sum(
        result["status"] in (409, 422)
        for result in results
    )

    print(
        f"PASS: total remained {after_total} paise"
    )
    print(
        f"PASS: balances are non-negative: "
        f"A={after_a}, B={after_b}"
    )
    print(f"      successful transfers: {successful}")
    print(f"      declined/conflict responses: {declined}")


def main():
    global WALLET_A, WALLET_B, TOKEN_A, TOKEN_B
    print(f"Testing API: {BASE_URL}\n")

    try:
        test_health()

        print("\n[Setup] Dynamically initializing authenticated wallets A and B...")
        user_a = f"BURST-USER-A-{uuid.uuid4()}"
        user_b = f"BURST-USER-B-{uuid.uuid4()}"
        TOKEN_A = get_token(user_a)
        TOKEN_B = get_token(user_b)
        
        # Create wallets
        res_a = create_wallet(user_a, TOKEN_A)
        assert_http_success(res_a, f"Create Wallet A")
        WALLET_A = wallet_id_from_response(res_a)
        
        res_b = create_wallet(user_b, TOKEN_B)
        assert_http_success(res_b, f"Create Wallet B")
        WALLET_B = wallet_id_from_response(res_b)
        
        # Seed balance to Wallet A (100000 paise)
        credit_res = credit_wallet(WALLET_A, 100000, TOKEN_A)
        assert_http_success(credit_res, f"Credit Wallet A")
        
        print(f"Setup complete: Wallet A ({WALLET_A}) and Wallet B ({WALLET_B}) successfully initialized & seeded.")

        test_concurrent_get_or_create()
        test_idempotency_retry_storm()
        test_conservation_under_contention()

    except AssertionError as error:
        print(f"\nFAIL: {error}", file=sys.stderr)
        sys.exit(1)

    print("\nALL TESTS PASSED")


if __name__ == "__main__":
    main()