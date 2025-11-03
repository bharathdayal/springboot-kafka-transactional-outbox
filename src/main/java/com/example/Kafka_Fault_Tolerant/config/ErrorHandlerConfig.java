package com.example.Kafka_Fault_Tolerant.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class ErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        ExponentialBackOff ex = new ExponentialBackOff();
        ex.setInitialInterval(1000L);
        ex.setMultiplier(2.0);
        ex.setMaxInterval(30000L);

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (cr, exx) -> new TopicPartition(cr.topic() + ".DLT", cr.partition()));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, ex);
        // optionally: handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
