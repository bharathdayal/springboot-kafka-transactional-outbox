package com.example.Kafka_Fault_Tolerant.config;

import com.example.Kafka_Fault_Tolerant.component.FallbackDeadLetterRecoverer;
import com.example.Kafka_Fault_Tolerant.repository.FailedEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.util.function.BiConsumer;

@Configuration
public class ErrorHandlerConfig {


    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(@Qualifier("nonTxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        System.out.println("[Config] creating DeadLetterPublishingRecoverer");
        return new DeadLetterPublishingRecoverer(kafkaTemplate,
                (cr, ex) -> new TopicPartition(cr.topic() + "-dlt", cr.partition()));
    }

    @Bean
    public ConsumerRecordRecoverer fallbackRecoverer(DeadLetterPublishingRecoverer dlpr,
                                                     FailedEventRepository failedEventRepository) {
        System.out.println("[Config] creating FallbackDeadLetterRecoverer");
        return new FallbackDeadLetterRecoverer(dlpr, failedEventRepository);
    }



    @Bean
    public DefaultErrorHandler errorHandler(ConsumerRecordRecoverer fallbackRecoverer) {
        System.out.println("[Config] creating DefaultErrorHandler (with fallbackRecoverer)");
       /* ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000);

        DefaultErrorHandler handler = new DefaultErrorHandler(fallbackRecoverer, backOff);
        // optional: handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;*/
        FixedBackOff fb = new FixedBackOff(0L, 0L); // immediate recovery, no retries
        return new DefaultErrorHandler(fallbackRecoverer, fb);
    }


}
