package org.facenet.service.shift;

import org.facenet.dto.shift.ShiftDto;

import java.util.List;

public interface ShiftService {

    List<ShiftDto.Response> getAll();

    ShiftDto.Response getById(Long id);

    ShiftDto.Response create(ShiftDto.Request request);

    ShiftDto.Response update(Long id, ShiftDto.UpdateRequest request);

    void delete(Long id);
}
