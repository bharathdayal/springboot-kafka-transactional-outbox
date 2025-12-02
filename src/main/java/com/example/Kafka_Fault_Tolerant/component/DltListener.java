package com.example.Kafka_Fault_Tolerant.component;

import com.example.Kafka_Fault_Tolerant.model.FailedEvent;
import com.example.Kafka_Fault_Tolerant.repository.FailedEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DltListener {

    private final FailedEventRepository repository;


    public DltListener(FailedEventRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "orders-dlt",groupId = "orders-service", containerFactory = "kafkaListenerContainerFactory")
    public void listenDlt(ConsumerRecord<String, String> record) {
        System.out.println("[DLT Listener] got record from " + record.topic() + " key=" + record.key());

        try {
            FailedEvent fe = new FailedEvent();
            fe.setTopic(record.topic());
            fe.setPartitionNo(record.partition());
            fe.setRecordKey(record.key() == null ? null : String.valueOf(record.key()));
            fe.setPayload(record.value() == null ? null : String.valueOf(record.value()));
            fe.setErrorMessage("DLT arrival");
            FailedEvent saved = repository.save(fe);
            System.out.println("[DLT Listener] persisted FailedEvent id=" + saved.getId());
        } catch (Exception ex) {
            System.err.println("[DLT Listener] failed to persist DLT record: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
