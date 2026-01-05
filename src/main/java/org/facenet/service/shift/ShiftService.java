package org.facenet.service.shift;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.shift.ShiftDto;

import java.util.List;
import java.util.Map;

public interface ShiftService {

    /**
     * Get all shifts with pagination and filters
     */
    PageResponseDto<ShiftDto.Response> getAll(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get all shifts (non-paginated)
     */
    List<ShiftDto.Response> getAll();

    ShiftDto.Response getById(Long id);

    ShiftDto.Response create(ShiftDto.Request request);

    ShiftDto.Response update(Long id, ShiftDto.UpdateRequest request);

    void delete(Long id);
}
