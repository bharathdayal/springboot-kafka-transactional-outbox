package com.example.Kafka_Fault_Tolerant.service;

import com.example.Kafka_Fault_Tolerant.dto.OrderEvent;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface OrderServiceImpl {
    void CreateOrderAndPublish(OrderEvent event) throws JsonProcessingException;
}
