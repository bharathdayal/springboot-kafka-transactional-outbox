package com.example.Kafka_Fault_Tolerant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // -----------------------
    // Transactional producer (used by OutboxPublisher)
    // -----------------------
    @Bean
    public ProducerFactory<String, String> producerFactory(){
        Map<String,Object> props=new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // send raw JSON string

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        DefaultKafkaProducerFactory<String,String> pf = new DefaultKafkaProducerFactory<>(props);
        pf.setTransactionIdPrefix("tx-"); // transactional
        return pf;
    }

    @Bean("stringKafkaTemplate")
    public KafkaTemplate<String,String> stringKafkaTemplate(@Qualifier("producerFactory") ProducerFactory<String,String> pf) {
        return new KafkaTemplate<>(pf);
    }

    // -----------------------
    // Non-transactional producer for DLQ/DeadLetterPublishingRecoverer and Retry topics
    // -----------------------
    @Bean
    public ProducerFactory<String,String> nonTxProducerFactory() {
        Map<String,Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // IMPORTANT: do NOT set transaction id prefix here
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean("nonTxKafkaTemplate")
    public KafkaTemplate<String,String> nonTxKafkaTemplate(@Qualifier("nonTxProducerFactory") ProducerFactory<String,String> nonTxProducerFactory) {
        return new KafkaTemplate<>(nonTxProducerFactory);
    }

    /**
     * Expose the non-transactional template under bean names used by RetryableTopics and other components:
     * - defaultRetryTopicKafkaTemplate (used by RetryableTopics)
     * - kafkaTemplate (a conventional default name)
     *
     * This returns the same instance as nonTxKafkaTemplate (no extra allocation).
     */
    @Bean(name = {"defaultRetryTopicKafkaTemplate", "kafkaTemplate"})
    public KafkaTemplate<String,String> defaultRetryTopicKafkaTemplate(
            @Qualifier("nonTxKafkaTemplate") KafkaTemplate<String,String> nonTxKafkaTemplate) {
        return nonTxKafkaTemplate;
    }

    // -----------------------
    // Consumer
    // -----------------------
    @Bean
    public ConsumerFactory<String,String> consumerFactory(){
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        JsonDeserializer<String> jsonDeserializer = new JsonDeserializer<>(String.class);
        jsonDeserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }


    // -----------------------
    // Listener factory (use injected consumerFactory param)
    // -----------------------
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String,String> consumerFactory,
            org.springframework.kafka.listener.DefaultErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory); // use injected param (not consumerFactory())
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler);
        factory.setConcurrency(1);
        System.out.println("[Config] kafkaListenerContainerFactory created and wired with DefaultErrorHandler");
        return factory;
    }

    // -----------------------
    // Topics
    // -----------------------
    @Bean
    public org.apache.kafka.clients.admin.NewTopic ordersTopic() {
        return new org.apache.kafka.clients.admin.NewTopic("orders",3,(short)1);
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic ordersDLQ() {
        return new org.apache.kafka.clients.admin.NewTopic("orders.DLT", 3, (short) 1);
    }
}
