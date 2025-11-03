# 🏗️ Spring Boot + Kafka + MySQL – Fault Tolerant Event System (Outbox Pattern)

This project demonstrates how to build a **fault-tolerant, event-driven system** using **Spring Boot**, **Kafka**, and **MySQL**, with full transactional consistency between the database and Kafka.

It implements the **Outbox Pattern**, ensuring reliable event delivery even when Kafka is temporarily unavailable.

---

## 🚀 Features

- ✅ **Transactional Outbox Pattern** to guarantee DB and Kafka consistency  
- ✅ **Spring Boot + Gradle** based clean architecture  
- ✅ **Kafka Producer, Consumer, and DLQ (Dead Letter Queue)**  
- ✅ **JPA Transaction with Kafka Transaction synchronization**  
- ✅ **Resilient retry mechanism** for failed Kafka publishes  
- ✅ **Configurable production-ready Kafka and DB settings**

---

## 🧩 Architecture Overview

Client → REST Controller → Service (Transactional)
↓
MySQL (Order Table + Outbox Table)
↓
OutboxPublisher → Kafka Topic (order-events)
↓
Kafka Consumer → DB / DLQ

yaml


- The **Service Layer** saves both the order and the event into the DB (in one transaction).
- The **OutboxPublisher** polls the `outbox_event` table, publishes events to Kafka, and marks them as sent.
- The **Consumer** processes incoming events. If a failure occurs, the message goes to the **DLQ** (Dead Letter Queue).

---

## 🧠 Why Outbox Pattern?

When you need to **maintain state consistency** between a **database** and **Kafka**,  
the Outbox Pattern ensures:
- Atomic writes to DB and event table
- Event persistence during Kafka downtime
- Guaranteed delivery (no message loss)
- Event replay and recovery

> 💡 Ideal for microservices where DB updates and Kafka events must be synchronized.

---

## 🧰 Tech Stack

| Component | Technology |
|------------|-------------|
| Framework | Spring Boot 3.x |
| Messaging | Apache Kafka |
| Database | MySQL |
| Build Tool | Gradle |
| ORM | Spring Data JPA |
| Language | Java 17+ |
| Serializer | JSON (Jackson) |

---
▶️ Running the Application
Step 1 — Start Kafka
Start Kafka  on your local machine or Docker.

bash
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
.\bin\windows\kafka-server-start.bat .\config\server.properties
.\bin\windows\kafka-console-consumer.bat --topic orders --from-beginning --bootstrap-server localhost:9092
.\bin\windows\kafka-console-consumer.bat --topic orders-topic-dlq --from-beginning --bootstrap-server localhost:9092
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092


🧪 Testing with Postman
1️⃣ Create an Order

POST http://localhost:8080/api/orders
Content-Type: application/json
Request Body

json

{
  "orderId": "1A-Mobile",
  "product": "Home Appliances-Mobile",
  "qty": 40
}
✅ Expected Result:

Order data stored in MySQL (orders table)

Corresponding event stored in outbox_event

OutboxPublisher will publish the event to Kafka topic order-events

Consumer logs received event


🧾 Example Logs
When Kafka is up
✅ Order saved
✅ Outbox entry created
✅ Kafka event published successfully
✅ Consumer received: OrderEvent{orderId='...', product='Laptop', qty=2}

When Kafka is down
⚠️ Kafka unavailable — event stored in Outbox
🔁 Will retry once Kafka is back online
🧱 Key Learnings
Maintain atomicity between DB and Kafka with Outbox Pattern

Use @Transactional to ensure consistency

Handle failures gracefully using DLQ

Decouple event publishing logic with OutboxPublisher

📘 References
Spring for Apache Kafka Docs

Outbox Pattern by Chris Richardson

Kafka Transactions
