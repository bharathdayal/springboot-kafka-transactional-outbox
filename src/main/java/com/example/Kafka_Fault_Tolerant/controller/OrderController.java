package com.example.Kafka_Fault_Tolerant.controller;

import com.example.Kafka_Fault_Tolerant.dto.OrderEvent;
import com.example.Kafka_Fault_Tolerant.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service=service;
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody OrderEvent event) throws JsonProcessingException {
        service.CreateOrderAndPublish(event);
        return  ResponseEntity.accepted().body(event.orderId());
    }
}
