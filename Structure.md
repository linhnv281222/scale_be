# Weighing System – System Design (v1)

## 1. Tổng quan

Hệ thống này là một **ứng dụng backend chạy local (desktop / on-premise)**, có nhiệm vụ:

* Kết nối và đọc dữ liệu từ **các cân công nghiệp** thông qua:

    * Modbus TCP
    * Modbus RTU
    * Serial
* Xử lý dữ liệu theo thời gian thực
* Lưu trữ lịch sử đo
* Cung cấp API cho UI và báo cáo
* Push dữ liệu realtime qua WebSocket

Hệ thống được thiết kế để vận hành **24/7**, ổn định với quy mô **~300 cân**.

---

## 2. Mục tiêu thiết kế

* Xử lý **dữ liệu dạng stream** (continuous ingestion)
* Tách biệt rõ:

    * I/O thiết bị
    * Xử lý nghiệp vụ
    * Lưu trữ
* Độ trễ thấp (near-realtime)
* Không phụ thuộc hạ tầng bên ngoài (broker, cloud)
* Dễ debug, dễ vận hành
* Có thể mở rộng về sau (v2, v3)

---

## 3. Phạm vi v1 (In-scope)

* Single Spring Boot application
* In-memory processing
* PostgreSQL làm database chính
* Realtime WebSocket cho UI
* REST API cho CRUD và báo cáo
* Active in-memory queue để xử lý dữ liệu cân

---

## 4. Những thứ **không** làm trong v1

* Message broker (RabbitMQ, ActiveMQ, Kafka)
* Microservices
* Distributed system
* Exactly-once semantics
* Disk-backed queue

Những thứ trên chỉ xem xét khi hệ vượt xa phạm vi hiện tại.

---

## 5. Kiến trúc tổng thể

```
┌─────────────────────────────────────────────┐
│             Spring Boot Application          │
│                                             │
│  ┌───────────────┐        ┌───────────────┐ │
│  │   REST API    │        │   WebSocket   │ │
│  └───────▲───────┘        └───────▲───────┘ │
│          │                            │     │
│  ┌───────┴──────────┐                │     │
│  │   Core Processing│◄───────────────┘     │
│  │  (Business Logic)│                      │
│  └───────▲──────────┘                      │
│          │                                  │
│  ┌───────┴──────────┐                      │
│  │ Active In-Memory │                      │
│  │       Queue       │                      │
│  └───────▲──────────┘                      │
│          │                                  │
│  ┌───────┴──────────┐                      │
│  │  Device Engines  │                      │
│  │  (Modbus/Serial) │                      │
│  └───────▲──────────┘                      │
│          │                                  │
│  ┌───────┴──────────┐                      │
│  │ Physical Scales  │                      │
│  │   (~300 devices) │                      │
│  └──────────────────┘                      │
│                                             │
│  ┌────────────────────────────────────────┐ │
│  │               PostgreSQL               │ │
│  └────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

---

## 6. Nguyên tắc thiết kế cốt lõi

### 6.1 Phân tách theo layer

| Layer           | Trách nhiệm              |
| --------------- | ------------------------ |
| Device Engine   | Đọc dữ liệu từ thiết bị  |
| Active Queue    | Đệm và điều phối dữ liệu |
| Core Processing | Xử lý nghiệp vụ          |
| Persistence     | Lưu và truy vấn DB       |
| API / WebSocket | Giao tiếp với client     |

Mỗi layer **không vượt quyền** sang layer khác.

---

### 6.2 Active In-Memory Queue là bắt buộc

Active Queue là:

* Một **queue trong bộ nhớ**
* Có **worker thread chạy liên tục**
* Dùng để:

    * Tách engine khỏi core
    * Chống block khi DB/UI chậm
    * Hấp thụ spike dữ liệu

Active Queue **không phải message broker**.

---

## 7. Luồng dữ liệu chính

### 7.1 Luồng realtime từ cân

```
Scale
 → Device Engine
 → MeasurementEvent
 → Active In-Memory Queue
 → Core Processing Worker
 → DB insert
 → WebSocket push
```

### 7.2 Luồng API / báo cáo

```
Client
 → REST API
 → Service Layer
 → PostgreSQL
```

API **không** truy cập trực tiếp engine hoặc queue.

---

## 8. Active Queue – cấu hình v1

| Thuộc tính     | Giá trị            |
| -------------- | ------------------ |
| Queue type     | ArrayBlockingQueue |
| Capacity       | 100k – 300k events |
| Producer       | Device Engines     |
| Consumer       | Core Workers       |
| Worker threads | 4 – 8              |
| Backpressure   | Bounded queue      |
| Persistence    | Không              |

Queue là **in-memory, bounded, active**.

---

## 9. Device Engine

* Mỗi cân có **1 engine instance**
* Engine chịu trách nhiệm:

    * Kết nối
    * Retry
    * Timeout
    * Polling định kỳ
* Engine **không xử lý nghiệp vụ**

---

## 10. Core Processing

Core chịu trách nhiệm:

* Chuẩn hóa dữ liệu
* Áp dụng mapping register → metric
* Xử lý trạng thái thiết bị
* Ghi:

    * measurements
    * device_events
* Push realtime qua WebSocket

---

## 11. Database

PostgreSQL là:

* Nơi lưu trữ cấu hình
* Lịch sử đo
* Dữ liệu báo cáo

Database **không** tham gia realtime processing.

Schema đã được thiết kế riêng (users, roles, devices, measurements, …).

---

## 12. Quy mô & năng lực

| Tiêu chí       | Mục tiêu        |
| -------------- | --------------- |
| Số cân         | ~300            |
| Polling        | 500ms – 1s      |
| Throughput     | ~300 msg/s      |
| Độ trễ         | < 1s end-to-end |
| Thời gian chạy | 24/7            |
