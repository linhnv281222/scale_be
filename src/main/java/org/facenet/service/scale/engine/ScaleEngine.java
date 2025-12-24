package org.facenet.service.scale.engine;

/**
 * Interface chung cho tất cả các Device Engine
 * Sử dụng tính đa hình (Polymorphism) - mỗi protocol là một loại máy khác nhưng cùng cách vận hành
 * 
 * Theo thiết kế Module 3:
 * - Mỗi cân có 1 engine instance
 * - Engine chạy trên thread riêng (implements Runnable)
 * - Engine chịu trách nhiệm: Kết nối, Retry, Timeout, Polling
 * - Engine KHÔNG xử lý nghiệp vụ (chỉ đọc và đẩy vào Queue)
 */
public interface ScaleEngine extends Runnable {
    
    /**
     * Dừng engine (sử dụng khi cấu hình thay đổi hoặc hot-reload)
     */
    void stop();
    
    /**
     * Lấy scale ID mà engine đang quản lý
     */
    Long getScaleId();
    
    /**
     * Kiểm tra engine có đang chạy không
     */
    boolean isRunning();
}
