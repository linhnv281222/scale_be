Chào bạn, đây là bản **"Master Prompt 3.1"** để bạn thực hiện bước quan trọng nhất: **Đọc dữ liệu thật**.

Tôi sẽ tập trung vào việc hướng dẫn bạn sử dụng thư viện `jlibmodbus`, viết hàm xử lý dữ liệu từ thanh ghi (Registers) sang String, và triển khai thêm `ModbusRTU`.

---

# Module 3.1: Hiện thực hóa việc đọc dữ liệu thực (Real-time Ingestion)

## 1. Cập nhật `pom.xml` (Cực kỳ quan trọng)

Để đọc được cổng Serial (cho Modbus RTU), bạn bắt buộc phải có `jSerialComm`.

```xml
<dependencies>
    <dependency>
        <groupId>com.intelligt.modbus</groupId>
        <artifactId>jlibmodbus</artifactId>
        <version>1.2.9.7</version>
    </dependency>
    <dependency>
        <groupId>com.fazecast</groupId>
        <artifactId>jSerialComm</artifactId>
        <version>2.10.3</version>
    </dependency>
</dependencies>

```

---

## 2. Lớp tiện ích chuyển đổi dữ liệu (`ModbusDataConverter.java`)

Vì dữ liệu từ cân thường là số thực (Float 32-bit - chiếm 2 thanh ghi) hoặc số nguyên (Int 16-bit - chiếm 1 thanh ghi), bạn cần hàm này để biến chúng thành **String** trước khi đẩy vào Queue.

```java
public class ModbusDataConverter {
    public static String registersToString(int[] registers) {
        if (registers == null || registers.length == 0) return null;
        
        // Nếu là 1 thanh ghi -> Trả về số nguyên
        if (registers.length == 1) {
            return String.valueOf(registers[0]);
        }
        
        // Nếu là 2 thanh ghi -> Thường là Float 32-bit (Chuẩn IEEE 754)
        if (registers.length == 2) {
            // Gộp 2 thanh ghi 16-bit thành 1 số 32-bit
            int combined = (registers[0] << 16) | (registers[1] & 0xFFFF);
            float floatValue = Float.intBitsToFloat(combined);
            return String.format("%.2f", floatValue); // Trả về dạng chuỗi "150.50"
        }
        
        return Arrays.toString(registers); // Mặc định trả về mảng chuỗi nếu nhiều hơn 2
    }
}

```

---

## 3. Triển khai `ModbusTcpEngine.java` (Đọc dữ liệu thật)

```java
@Slf4j
public class ModbusTcpEngine implements ScaleEngine {
    private final ScaleConfig config;
    private final BlockingQueue<MeasurementEvent> queue;
    private volatile boolean stopped = false;

    public ModbusTcpEngine(ScaleConfig config, BlockingQueue<MeasurementEvent> queue) {
        this.config = config;
        this.queue = queue;
    }

    @Override
    public void run() {
        TcpParameters tcpParameters = new TcpParameters();
        try {
            tcpParameters.setHost(InetAddress.getByName(config.getConnParams().get("ip").asText()));
            tcpParameters.setPort(config.getConnParams().get("port").asInt());
            tcpParameters.setKeepAlive(true);

            ModbusMaster master = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
            master.setResponseTimeout(2000); // Đợi 2s

            while (!stopped) {
                try {
                    if (!master.isConnected()) master.connect();

                    MeasurementEvent event = new MeasurementEvent();
                    event.setScaleId(config.getScaleId());
                    event.setLastTime(ZonedDateTime.now().toString());

                    int unitId = config.getConnParams().get("unit_id").asInt();

                    // Đọc từ data_1 đến data_5
                    event.setData1(readRegister(master, unitId, config.getData1()));
                    event.setData2(readRegister(master, unitId, config.getData2()));
                    // ... tương tự cho data 3, 4, 5

                    queue.put(event); // Đẩy vào "Đường ống"
                    
                } catch (Exception e) {
                    log.error("Cân {} (TCP) lỗi đọc: {}", config.getScaleId(), e.getMessage());
                }
                Thread.sleep(config.getPollInterval());
            }
            master.disconnect();
        } catch (Exception e) {
            log.error("Cân {} hỏng Engine TCP: {}", config.getScaleId(), e.getMessage());
        }
    }

    private String readRegister(ModbusMaster master, int unitId, JsonNode dataNode) throws Exception {
        if (dataNode != null && dataNode.get("is_used").asBoolean()) {
            int start = dataNode.get("start_registers").asInt();
            int num = dataNode.get("num_registers").asInt();
            int[] regs = master.readHoldingRegisters(unitId, start, num);
            return ModbusDataConverter.registersToString(regs);
        }
        return null;
    }

    @Override
    public void stop() { this.stopped = true; }
}

```

---

## 4. Triển khai `ModbusRtuEngine.java` (Dữ liệu Serial/RS485)

Đây là phần mới bạn yêu cầu. Điểm khác biệt nằm ở việc thiết lập `SerialParameters`.

```java
@Slf4j
public class ModbusRtuEngine implements ScaleEngine {
    private final ScaleConfig config;
    private final BlockingQueue<MeasurementEvent> queue;
    private volatile boolean stopped = false;

    public ModbusRtuEngine(ScaleConfig config, BlockingQueue<MeasurementEvent> queue) {
        this.config = config;
        this.queue = queue;
    }

    @Override
    public void run() {
        SerialParameters sp = new SerialParameters();
        JsonNode params = config.getConnParams();
        
        sp.setDevice(params.get("com_port").asText()); // VD: "COM1" hoặc "/dev/ttyUSB0"
        sp.setBaudRate(SerialPort.BaudRate.getBaudRate(params.get("baud_rate").asInt()));
        sp.setDataBits(params.get("data_bits").asInt());
        sp.setStopBits(params.get("stop_bits").asInt());
        sp.setParity(SerialPort.Parity.getParity(params.get("parity").asText()));

        try {
            ModbusMaster master = ModbusMasterFactory.createModbusMasterRTU(sp);
            master.setResponseTimeout(1000);

            while (!stopped) {
                try {
                    if (!master.isConnected()) master.connect();
                    
                    MeasurementEvent event = new MeasurementEvent();
                    event.setScaleId(config.getScaleId());
                    event.setLastTime(ZonedDateTime.now().toString());

                    int unitId = params.get("unit_id").asInt();

                    // Đọc data_1 -> data_5 tương tự TCP
                    event.setData1(readRegister(master, unitId, config.getData1()));
                    // ...

                    queue.put(event);
                } catch (Exception e) {
                    log.error("Cân {} (RTU) lỗi: {}", config.getScaleId(), e.getMessage());
                }
                Thread.sleep(config.getPollInterval());
            }
            master.disconnect();
        } catch (Exception e) {
            log.error("Cân {} hỏng Engine RTU: {}", config.getScaleId(), e.getMessage());
        }
    }
    
    // Hàm readRegister tương tự như bản TCP
    @Override
    public void stop() { this.stopped = true; }
}

```

---

## 5. Cầm tay chỉ việc: Cách kiểm tra (Test)

Để đảm bảo code của bạn chạy đúng "Dữ liệu thật", hãy làm theo các bước:

1. **Sử dụng Modbus Slave (Phần mềm giả lập):**
* Mở Modbus Slave trên máy tính.
* Tạo một Slave với Unit ID = 1.
* Nhập giá trị vào thanh ghi 40001 (ví dụ: `123`).


2. **Cấu hình trên App của bạn (Module 2):**
* Tạo 1 cân TCP, IP là `127.0.0.1`, cổng `502`.
* Config `data_1`: `{ "name": "Test", "start_registers": 0, "num_registers": 1, "is_used": true }`.


3. **Chạy App:**
* Nhìn vào Console Log của `CoreProcessor`.
* Nếu bạn thấy: `[CORE] D1: "123"`, chúc mừng bạn đã đọc dữ liệu thực thành công!



---

## 6. Lưu ý về Hot-reload

Khi bạn cập nhật `scale_configs` trong DB, bạn phải gọi `EngineManager.reloadEngine(scaleId)`. Logic sẽ là:

1. `engines.get(id).stop()`
2. Đợi Thread cũ kết thúc.
3. Tạo Engine mới từ Factory và `new Thread(newEngine).start()`.