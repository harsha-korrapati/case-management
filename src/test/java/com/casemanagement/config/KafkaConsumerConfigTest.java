package com.casemanagement.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.BiFunction;

class KafkaConsumerConfigTest {

    private final KafkaConsumerConfig config = new KafkaConsumerConfig();

    @Test
    void deadLetterRecovererRoutesToDlqKeepingPartition() {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        DeadLetterPublishingRecoverer recoverer = config.deadLetterPublishingRecoverer(template);
        @SuppressWarnings("unchecked")
        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> resolver =
                (BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition>)
                        ReflectionTestUtils.getField(recoverer, "destinationResolver");
        TopicPartition destination = resolver.apply(
                new ConsumerRecord<>("case-events", 2, 0, "key", "value"),
                new IllegalStateException("failure"));

        assertEquals(new TopicPartition("case-events-dlq", 2), destination);
        assertEquals(true, ReflectionTestUtils.getField(recoverer, "failIfSendResultIsError"));
    }

    @Test
    void errorHandlerBuildsRetryPolicyAndCommitsRecoveredOffsets() {
        DeadLetterPublishingRecoverer recoverer =
                config.deadLetterPublishingRecoverer(mock(KafkaTemplate.class));

        DefaultErrorHandler handler = config.kafkaErrorHandler(recoverer, 4, 100, 2.0, 1000);

        assertEquals(true, ReflectionTestUtils.getField(handler, "commitRecovered"));
        Object tracker = ReflectionTestUtils.getField(handler, "failureTracker");
        assertEquals(3, ((org.springframework.kafka.support.ExponentialBackOffWithMaxRetries)
                ReflectionTestUtils.getField(tracker, "backOff")).getMaxRetries());
    }

    @Test
    void listenerFactoryUsesManualImmediateAckAndConfiguredConcurrency() {
        ConsumerFactory<String, String> consumerFactory = mock(ConsumerFactory.class);
        DefaultErrorHandler errorHandler = mock(DefaultErrorHandler.class);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                config.kafkaListenerContainerFactory(consumerFactory, errorHandler);

        assertEquals(consumerFactory, factory.getConsumerFactory());
        assertEquals(3, ReflectionTestUtils.getField(factory, "concurrency"));
        assertEquals(ContainerProperties.AckMode.MANUAL_IMMEDIATE,
                factory.getContainerProperties().getAckMode());
        assertEquals(errorHandler, ReflectionTestUtils.getField(factory, "commonErrorHandler"));
    }
}
