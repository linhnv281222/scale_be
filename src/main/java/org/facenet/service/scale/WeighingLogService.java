package org.facenet.service.scale;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.scale.WeighingDataDto;
import org.springframework.data.domain.Page;

import java.util.Map;

/**
 * Service for querying weighing log history.
 */
public interface WeighingLogService {

    /**
     * Get weighing log history with optional filters and pagination.
     */
    Page<WeighingDataDto.LogResponse> getHistory(WeighingDataDto.LogQueryRequest request);
    
    /**
     * Get weighing history with advanced filtering
     * Supports filters: scaleId, scaleCode, direction, locationId, protocolId, startTime, endTime
     * Supports search on: scaleName, scaleCode
     */
    PageResponseDto<WeighingDataDto.HistoryResponse> getWeighingHistory(
        PageRequestDto pageRequest, 
        Map<String, String> filters
    );
}
