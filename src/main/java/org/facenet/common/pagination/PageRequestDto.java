package org.facenet.common.pagination;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic pagination request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {
    
    @JsonProperty("page")
    @Builder.Default
    private Integer page = 0; // 0-indexed
    
    @JsonProperty("size")
    @Builder.Default
    private Integer size = 10;
    
    @JsonProperty("sort")
    private String sort; // Format: field,direction (e.g., "name,asc" or "createdAt,desc")
    
    @JsonProperty("search")
    private String search; // Global search keyword
    
    /**
     * Convert to Spring PageRequest
     */
    public org.springframework.data.domain.PageRequest toPageRequest() {
        if (sort != null && !sort.isBlank()) {
            String[] sortParts = sort.split(",");
            String field = sortParts[0];
            org.springframework.data.domain.Sort.Direction direction = 
                sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) 
                    ? org.springframework.data.domain.Sort.Direction.DESC 
                    : org.springframework.data.domain.Sort.Direction.ASC;
            return org.springframework.data.domain.PageRequest.of(
                page, 
                size, 
                org.springframework.data.domain.Sort.by(direction, field)
            );
        }
        return org.springframework.data.domain.PageRequest.of(page, size);
    }
}
