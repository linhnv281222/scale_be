package org.facenet.service.scale;

import org.facenet.dto.scale.WeighingDataDto;
import org.springframework.data.domain.Page;

/**
 * Service for querying weighing log history.
 */
public interface WeighingLogService {

    /**
     * Get weighing log history with optional filters and pagination.
     */
    Page<WeighingDataDto.LogResponse> getHistory(WeighingDataDto.LogQueryRequest request);
}
