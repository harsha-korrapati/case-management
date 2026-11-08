package com.casemanagement.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class ApplicationMetrics {

    private final MeterRegistry meterRegistry;

    public void incrementCasesCreated() {
        incrementAfterCommit("cases.created");
    }

    public void incrementCasesUpdated() {
        incrementAfterCommit("cases.updated");
    }

    public void incrementAlertsCreated() {
        incrementAfterCommit("case.alerts.created");
    }

    public void incrementKafkaEventsConsumed() {
        incrementImmediately("kafka.events.consumed");
    }

    public void incrementKafkaEventsFailed() {
        incrementImmediately("kafka.events.failed");
    }

    private void incrementAfterCommit(String metricName) {

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            incrementImmediately(metricName);
                        }
                    }
            );

        } else {
            incrementImmediately(metricName);
        }
    }

    private void incrementImmediately(String metricName) {
        meterRegistry.counter(metricName).increment();
    }
}
