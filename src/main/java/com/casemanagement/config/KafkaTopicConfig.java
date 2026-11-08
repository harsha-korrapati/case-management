package com.casemanagement.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic caseEventsTopic() {
        return TopicBuilder.name("case-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic caseEventsDlqTopic() {
        return TopicBuilder.name("case-events-dlq")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
