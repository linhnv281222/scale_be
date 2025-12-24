**Module 3: Real-time Operations**, chúng ta sẽ đi qua từng bước triển khai cụ thể như một kiến trúc sư đang hướng dẫn thợ xây. Chúng ta sẽ lấy stack là **Java Spring Boot** làm chuẩn (vì nó hỗ trợ tốt cho quản lý Thread và BlockingQueue).

---

# Hướng dẫn chi tiết triển khai Module 3: Luồng dữ liệu Real-time

## Bước 1: Khai báo "Bao thư" chứa dữ liệu (`MeasurementEvent`)

Đây là vật thể duy nhất chạy xuyên suốt từ Engine qua Queue đến Core.

```java
// Folder: com.scalehub.model
public class MeasurementEvent {
    private Long scaleId;        // ID của cân
    private String lastTime;    // Thời điểm đọc được từ thiết bị (ISO-8601)
    
    // 5 trường dữ liệu thô đọc được, luôn để String
    private String data1; 
    private String data2;
    private String data3;
    private String data4;
    private String data5;

    // Getters, Setters, Constructor...
}

```

---

## Bước 2: Thiết lập "Kho chứa đệm" (Active Queue)

Chúng ta sẽ tạo một Bean quản lý Queue tập trung để mọi Engine đều có thể đẩy hàng vào.

```java
// Folder: com.scalehub.config
@Configuration
public class QueueConfig {
    @Bean
    public BlockingQueue<MeasurementEvent> activeQueue() {
        // Dung lượng 200,000 sự kiện. Nếu đầy, Engine sẽ tự động đợi.
        return new ArrayBlockingQueue<>(200000);
    }
}

```

---

## Bước 3: Xây dựng "Động cơ" đọc dữ liệu (Scale Engine)

Chúng ta sử dụng tính đa hình (Polymorphism). Mỗi loại giao thức là một loại máy khác nhau nhưng có chung cách vận hành.

### 3.1. Giao diện chung (Interface)

```java
public interface ScaleEngine extends Runnable {
    void stop(); // Dùng để tắt engine khi cấu hình thay đổi (Hot-reload)
}

```

### 3.2. Triển khai Engine Modbus TCP (Ví dụ đại diện)

Đây là nơi bạn "cầm tay" xử lý logic đọc:

1. **Lấy Config:** Engine giữ một bản copy của `scale_configs`.
2. **Loop:** Chạy một vòng lặp `while(!stopped)`.
3. **Read:** Dùng thư viện Modbus đọc thanh ghi.
4. **Convert:** Ép kết quả về String.
5. **Push:** Đẩy vào Queue.

```java
public class ModbusTcpEngine implements ScaleEngine {
    private ScaleConfig config; // Lấy từ DB
    private BlockingQueue<MeasurementEvent> queue;
    private volatile boolean stopped = false;

    @Override
    public void run() {
        while (!stopped) {
            try {
                // 1. Kết nối tới IP/Port trong config.getConnParams()
                // 2. Kiểm tra data_1 đến data_5
                MeasurementEvent event = new MeasurementEvent();
                event.setScaleId(config.getScaleId());
                event.setLastTime(ZonedDateTime.now().toString());

                if (config.getData1().isUsed()) {
                    // Logic Modbus: readHoldingRegisters(start, num)
                    String rawValue = modbusClient.read(config.getData1()); 
                    event.setData1(rawValue); 
                }
                // Tương tự cho data_2 -> data_5...

                // 3. Đẩy vào Queue
                queue.put(event); 

                // 4. Nghỉ theo poll_interval
                Thread.sleep(config.getPollInterval());
            } catch (Exception e) {
                log.error("Lỗi đọc cân {}: {}", config.getScaleId(), e.getMessage());
            }
        }
    }
}

```

---

## Bước 4: Xây dựng "Bộ não" xử lý (Core Worker)

Core là một "kẻ tham ăn" dữ liệu. Nó chỉ làm một việc duy nhất: **Thấy Queue có hàng là lấy ra ngay.**

```java
// Folder: com.scalehub.core
@Component
public class CoreProcessor {
    @Autowired
    private BlockingQueue<MeasurementEvent> activeQueue;

    @PostConstruct
    public void startProcessing() {
        // Chạy một Thread riêng biệt để không block ứng dụng chính
        new Thread(() -> {
            while (true) {
                try {
                    // Lấy dữ liệu ra (Nếu queue trống, nó sẽ treo máy chờ ở đây)
                    MeasurementEvent event = activeQueue.take();

                    // V1: Ghi LOG ra console để kiểm tra
                    System.out.println("-----------------------------------");
                    System.out.println("[CORE] Nhận dữ liệu từ Cân ID: " + event.getScaleId());
                    System.out.println("[CORE] Thời gian: " + event.getLastTime());
                    System.out.println("[CORE] D1: " + (event.getData1() != null ? event.getData1() : "N/A"));
                    System.out.println("[CORE] D2: " + (event.getData2() != null ? event.getData2() : "N/A"));
                    System.out.println("-----------------------------------");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
}

```

---

## Bước 5: Điều phối và Khởi chạy (Orchestration)

Khi ứng dụng khởi động (`ApplicationReadyEvent`), bạn cần một dịch vụ quét DB và "đề máy" cho tất cả các cân.

```java
@Service
public class EngineManager {
    @Autowired
    private ScaleConfigRepository repo;
    
    private Map<Long, ScaleEngine> runningEngines = new HashMap<>();

    public void startAllEngines() {
        List<ScaleConfig> activeConfigs = repo.findAllActive();
        for (ScaleConfig config : activeConfigs) {
            ScaleEngine engine = EngineFactory.create(config); // Tạo engine tương ứng protocol
            new Thread(engine).start();
            runningEngines.put(config.getScaleId(), engine);
        }
    }
}

```

---

## Những điểm "Chỉ tay" bạn cần lưu ý nhất:

1. **Tính ổn định:** Tại bước 3.2, nếu cân bị mất mạng, `modbusClient` sẽ ném lỗi. Bạn phải bao bọc bằng `try-catch` cực kỳ cẩn thận để Engine không bị chết hẳn (Crash). Phải có cơ chế tự động kết nối lại.
2. **Dữ liệu dạng String:** Tại sao tôi bắt bạn để String? Vì khi Engine đọc Modbus, nó có thể trả về `0001` hoặc `1.0`. Nếu để String, bạn có thể log chính xác những gì Engine "thấy". Việc ép kiểu sang Double để tính toán sẽ làm ở bước sau (Core v2).
3. **Active Queue là then chốt:** Nếu sau này bạn có 1000 cân, bạn chỉ cần tăng số lượng Thread ở `CoreProcessor` (Bước 4) là hệ thống sẽ xử lý mượt mà, không sợ treo UI.
