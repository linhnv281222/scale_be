package org.facenet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time data streaming
 * 
 * Module 3.2: Real-time Broadcasting với WebSockets
 * - Sử dụng STOMP protocol để broadcast dữ liệu cân
 * - Client subscribe vào /topic/scales (toàn bộ) hoặc /topic/scale/{scaleId} (riêng lẻ)
 * - Endpoint: /ws-scalehub với SockJS fallback
 */
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
        // Endpoint tại root (backward compatible)
        registry.addEndpoint("/ws-scalehub")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://localhost:5173"
                )
                .withSockJS();
        
        // Endpoint với context-path /api/v1 (primary)
        // Note: Spring Boot sẽ tự động thêm context-path, không cần thêm /api/v1 vào endpoint
    }
}
