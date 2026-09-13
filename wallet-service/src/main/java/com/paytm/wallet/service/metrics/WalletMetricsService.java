package com.paytm.wallet.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WalletMetricsService {

    private final Counter transferRequests;
    private final Counter transferCreated;
    private final Counter transferDeclinedInsufficientFunds;
    private final Counter transferIdempotentReplay;
    private final Counter transferErrors;
    private final Timer transferLatency;

    public WalletMetricsService(MeterRegistry meterRegistry) {
        this.transferRequests = Counter.builder("wallet.transfer.requests")
                .description("Total transfer requests received")
                .register(meterRegistry);

        this.transferCreated = Counter.builder("wallet.transfer.created")
                .description("Transfers successfully created")
                .register(meterRegistry);

        this.transferDeclinedInsufficientFunds =
                Counter.builder("wallet.transfer.declined.insufficient_funds")
                        .description("Transfers declined due to insufficient funds")
                        .register(meterRegistry);

        this.transferIdempotentReplay =
                Counter.builder("wallet.transfer.idempotent.replay")
                        .description("Idempotent replays for existing transfers")
                        .register(meterRegistry);

        this.transferErrors = Counter.builder("wallet.transfer.errors")
                .description("Transfer processing errors")
                .register(meterRegistry);

        this.transferLatency = Timer.builder("wallet.transfer.latency")
                .description("Latency of transfer processing")
                .publishPercentiles(0.99)
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(250),
                        Duration.ofSeconds(1)
                )
                .register(meterRegistry);
    }

    public void incrementTransferRequest() {
        transferRequests.increment();
    }

    public void incrementTransferCreated() {
        transferCreated.increment();
    }

    public void incrementTransferDeclinedInsufficientFunds() {
        transferDeclinedInsufficientFunds.increment();
    }

    public void incrementTransferIdempotentReplay() {
        transferIdempotentReplay.increment();
    }

    public void incrementTransferError() {
        transferErrors.increment();
    }

    public Timer.Sample startTransferTimer() {
        return Timer.start();
    }

    public void finishTransferTimer(Timer.Sample sample) {
        if (sample != null) {
            sample.stop(transferLatency);
        }
    }

    public Map<String, Object> snapshot() {
        double requests = transferRequests.count();
        double errors = transferErrors.count();

        Map<String, Object> domainCounters = new HashMap<>();
        domainCounters.put(
                "transfersCreated",
                transferCreated.count()
        );
        domainCounters.put(
                "transfersDeclinedInsufficientFunds",
                transferDeclinedInsufficientFunds.count()
        );
        domainCounters.put(
                "transfersIdempotentReplay",
                transferIdempotentReplay.count()
        );
        domainCounters.put(
                "transferRequests",
                requests
        );
        domainCounters.put(
                "transferErrors",
                errors
        );

        Map<String, Object> data = new HashMap<>();
        data.put("requestCount", requests);
        data.put("errorRatePercent", calculateErrorRatePercent(requests, errors));
        data.put("latencyP99Ms", latencyP99Ms());
        data.put("domainCounters", domainCounters);

        return data;
    }

    private double calculateErrorRatePercent(
            double requests,
            double errors
    ) {
        if (requests == 0.0) {
            return 0.0;
        }

        return (errors / requests) * 100.0;
    }

    private double latencyP99Ms() {
        if (transferLatency.count() == 0) {
            return 0.0;
        }

        return transferLatency.percentile(0.99, TimeUnit.MILLISECONDS);
    }
}
