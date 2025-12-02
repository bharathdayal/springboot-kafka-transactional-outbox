package com.example.Kafka_Fault_Tolerant.service;

import com.example.Kafka_Fault_Tolerant.dto.OrderEvent;
import com.example.Kafka_Fault_Tolerant.model.Order;
import com.example.Kafka_Fault_Tolerant.model.OutboxEvent;
import com.example.Kafka_Fault_Tolerant.repository.OrderRepository;
import com.example.Kafka_Fault_Tolerant.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class OrderService implements OrderServiceImpl {

    private final OrderRepository repository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository repository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void CreateOrderAndPublish(OrderEvent event) throws JsonProcessingException {
        Order order= new Order(event.orderId(),event.product(),event.qty());
        repository.save(order);

        OutboxEvent oe = new OutboxEvent();
        oe.setAggregateType("Order");
        oe.setAggregateId(event.orderId());
        oe.setType("OrderCreated");
        oe.setPayload(objectMapper.writeValueAsString(event));
        oe.setPublished(false);
        outboxRepository.save(oe);

       // producer.sendOrder(event);
    }

    @Override
    public void UpdateOrderAndPublish(OrderEvent event) throws JsonProcessingException {
        Order exiting =repository.findById(Long.valueOf(event.orderId())).orElseThrow( ()->new RuntimeException("Order not found: " + event.orderId()));

        exiting.setProduct(event.product());
        exiting.setQty(event.qty());
        repository.save(exiting);

        OutboxEvent oe =outboxRepository.findById(event.orderId()).orElseThrow( ()->new RuntimeException("OrderEvent not found: " + event.orderId()));
        oe.setAggregateType("Order");
        oe.setAggregateId(event.orderId());
        oe.setType("OrderUpdated");
        oe.setPayload(objectMapper.writeValueAsString(event));
        oe.setPublished(false);
        outboxRepository.save(oe);

    }
}
