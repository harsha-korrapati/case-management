package com.casemanagement.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

import java.time.Duration;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) -> new TopicPartition("case-events-dlq", record.partition())
                );

        // Treat a failed DLQ send as a recovery failure.
        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(10));

        return recoverer;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer,
            @Value("${app.kafka.consumer.retry.max-attempts:4}")
            int maxAttempts,
            @Value("${app.kafka.consumer.retry.initial-interval-ms:1000}")
            long initialInterval,
            @Value("${app.kafka.consumer.retry.multiplier:2.0}")
            double multiplier,
            @Value("${app.kafka.consumer.retry.max-interval-ms:10000}")
            long maxInterval
    ) {
        // maxAttempts includes the initial attempt.
        // Therefore, 4 total attempts means 3 retries.
        ExponentialBackOffWithMaxRetries backOff =
                new ExponentialBackOffWithMaxRetries(
                        Math.max(0, maxAttempts - 1)
                );

        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxInterval);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Commit the original offset after successful recovery to the DLQ.
        errorHandler.setCommitRecovered(true);

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(3);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory(
            KafkaProperties kafkaProperties) {

        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties()
        );
    }
}
