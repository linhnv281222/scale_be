package org.facenet.common.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Error response following API specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    @Builder.Default
    private boolean success = false;
    
    private String error;
    
    private String message;
    
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();
    
    private String path;
    
    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
                .success(false)
                .error(errorCode)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
