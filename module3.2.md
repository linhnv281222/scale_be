Hiện tại, hệ thống của bạn mới dừng lại ở việc **Log dữ liệu ra Console** tại tầng `CoreProcessor`. **WebSockets chưa được triển khai.**

Đây là bước tiếp theo (Module 3.2) để biến dữ liệu "ngầm" trong server thành dữ liệu "sống" trên giao diện người dùng. Chúng ta sẽ sử dụng **Spring WebSocket với giao thức STOMP** để thực hiện việc broadcast này.

---

# Module 3.2: Real-time Broadcasting với WebSockets

## 1. Cấu hình WebSocket Broker (`WebSocketConfig.java`)

Đầu tiên, bạn cần tạo một "trạm trung chuyển" để client có thể đăng ký (subscribe) nhận dữ liệu.

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Client sẽ subscribe vào các topic bắt đầu bằng /topic
        config.enableSimpleBroker("/topic");
        // Prefix cho các tin nhắn từ client gửi lên server (nếu có)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để client kết nối tới (Dùng SockJS để hỗ trợ trình duyệt cũ)
        registry.addEndpoint("/ws-scalehub")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}

```

---

## 2. Nâng cấp `CoreProcessor` để Broadcast dữ liệu

Bây giờ, thay vì chỉ in Log, chúng ta sẽ tiêm (`Inject`) `SimpMessagingTemplate` để đẩy dữ liệu ra các Topic.

```java
@Component
public class CoreProcessor {
    @Autowired
    private BlockingQueue<MeasurementEvent> activeQueue;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostConstruct
    public void startProcessing() {
        new Thread(() -> {
            while (true) {
                try {
                    // 1. Lấy dữ liệu từ Queue
                    MeasurementEvent event = activeQueue.take();

                    // 2. Log (như v1)
                    log.info("Processing scale {}: D1={}", event.getScaleId(), event.getData1());

                    // 3. BROADCAST: Gửi tới 2 loại topic
                    
                    // Topic toàn cục: Tất cả các cân đẩy chung về đây (cho màn hình tổng quát)
                    messagingTemplate.convertAndSend("/topic/scales", event);

                    // Topic riêng lẻ: Chỉ đẩy dữ liệu của 1 cân cụ thể (cho màn hình chi tiết)
                    messagingTemplate.convertAndSend("/topic/scale/" + event.getScaleId(), event);

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

## 3. Cấu trúc Topic và Event (Payload)

Client (Frontend) sẽ nhận được một JSON object chính là cái "Bao thư" `MeasurementEvent` mà bạn đã thiết kế:

* **Topic chung:** `/topic/scales`
* **Topic định danh:** `/topic/scale/50` (Dữ liệu riêng của cân ID 50)

**Payload mẫu nhận được tại Client:**

```json
{
  "scaleId": 50,
  "lastTime": "2025-12-24T14:30:00.123Z",
  "data1": "150.55",
  "data2": "1",
  "data3": null,
  "data4": null,
  "data5": null
}

```

---

## 4. Hướng dẫn Test (Cầm tay chỉ việc cho Frontend)

Để kiểm tra xem Socket có chạy không, bạn có thể tạo một file `test.html` đơn giản sử dụng thư viện `StompJS`:

```javascript
var socket = new SockJS('http://localhost:8080/ws-scalehub');
var stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
    
    // Đăng ký nhận dữ liệu từ cân ID 50
    stompClient.subscribe('/topic/scale/50', function (message) {
        var data = JSON.parse(message.body);
        console.log("Số cân nhảy: ", data.data1);
        // Update dữ liệu lên màn hình tại đây
        document.getElementById("weight-display").innerText = data.data1 + " kg";
    });
});

```

---

## 5. Những lưu ý quan trọng khi triển khai Real-time

1. **Throttling (Kiểm soát tốc độ):** Với 300 cân và `poll_interval` là 500ms, bạn sẽ có 600 tin nhắn/giây đẩy xuống trình duyệt. UI có thể bị treo nếu render liên tục.
* *Giải pháp:* CoreProcessor có thể kiểm tra, nếu giá trị không thay đổi so với lần đọc trước thì không push Socket (nhưng vẫn ghi DB).


2. **Security:** Hiện tại Endpoint `/ws-scalehub` đang để `setAllowedOriginPatterns("*")`. Khi chạy thật, bạn cần cấu hình JWT để bảo vệ kết nối này.
3. **Active Queue Performance:** Việc đẩy dữ liệu qua WebSocket diễn ra ngay bên trong Worker của Core. Nếu mạng Client chậm, nó có thể làm chậm cả luồng Core.
* *Cầm tay chỉ việc:* Luôn đảm bảo `messagingTemplate.convertAndSend` chạy bất đồng bộ hoặc không bị block.
