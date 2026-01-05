package org.facenet.common.pagination;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic pagination response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDto<T> {
    
    @JsonProperty("content")
    private List<T> content;
    
    @JsonProperty("page")
    private Integer page;
    
    @JsonProperty("size")
    private Integer size;
    
    @JsonProperty("total_elements")
    private Long totalElements;
    
    @JsonProperty("total_pages")
    private Integer totalPages;
    
    @JsonProperty("is_first")
    private Boolean isFirst;
    
    @JsonProperty("is_last")
    private Boolean isLast;
    
    @JsonProperty("has_next")
    private Boolean hasNext;
    
    @JsonProperty("has_previous")
    private Boolean hasPrevious;
    
    /**
     * Create from Spring Page
     */
    public static <T> PageResponseDto<T> from(org.springframework.data.domain.Page<T> page) {
        return PageResponseDto.<T>builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .isFirst(page.isFirst())
            .isLast(page.isLast())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }
}
