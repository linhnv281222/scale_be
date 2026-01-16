package org.facenet.service.shift;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.specification.GenericSpecification;
import org.facenet.dto.shift.ShiftDto;
import org.facenet.entity.shift.Shift;
import org.facenet.mapper.ShiftMapper;
import org.facenet.repository.shift.ShiftRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    
    // Use @Lazy to avoid circular dependency
    @Lazy
    private final org.facenet.service.schedule.ShiftReadingSchedulerService shiftReadingSchedulerService;

    @Override
    public PageResponseDto<ShiftDto.Response> getAll(PageRequestDto pageRequest, Map<String, String> filters) {
        GenericSpecification<Shift> spec = new GenericSpecification<>();
        Specification<Shift> specification = spec.buildSpecification(filters);
        
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            Specification<Shift> searchSpec = spec.buildSearchSpecification(
                pageRequest.getSearch(), "name", "code", "description"
            );
            specification = specification.and(searchSpec);
        }
        
        PageRequest springPageRequest = pageRequest.toPageRequest();
        Page<Shift> page = shiftRepository.findAll(specification, springPageRequest);
        Page<ShiftDto.Response> responsePage = page.map(ShiftMapper::toResponseDto);
        
        return PageResponseDto.from(responsePage);
    }

    @Override
    public List<ShiftDto.Response> getAll() {
        return ShiftMapper.toResponseDtoList(shiftRepository.findAll());
    }

    @Override
    public ShiftDto.Response getById(Long id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", "id", id));
        return ShiftMapper.toResponseDto(shift);
    }

    @Override
    @Transactional
    public ShiftDto.Response create(ShiftDto.Request request) {
        if (shiftRepository.existsByCode(request.getCode())) {
            throw new AlreadyExistsException("Shift", "code", request.getCode());
        }

        Shift shift = ShiftMapper.toEntity(request);
        shift = shiftRepository.save(shift);
        
        // Refresh shift reading schedules after creating new shift
        if (Boolean.TRUE.equals(shift.getIsActive())) {
            refreshShiftSchedules();
        }
        
        return ShiftMapper.toResponseDto(shift);
    }

    @Override
    @Transactional
    public ShiftDto.Response update(Long id, ShiftDto.UpdateRequest request) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", "id", id));

        boolean timeChanged = false;
        
        if (request.getName() != null) {
            shift.setName(request.getName());
        }
        if (request.getStartTime() != null) {
            shift.setStartTime(request.getStartTime());
            timeChanged = true;
        }
        if (request.getEndTime() != null) {
            shift.setEndTime(request.getEndTime());
            timeChanged = true;
        }
        if (request.getIsActive() != null) {
            shift.setIsActive(request.getIsActive());
            timeChanged = true; // Active status change also requires schedule refresh
        }

        shift = shiftRepository.save(shift);
        
        // Refresh shift reading schedules if times or active status changed
        if (timeChanged) {
            refreshShiftSchedules();
        }
        
        return ShiftMapper.toResponseDto(shift);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", "id", id));
        
        // Soft delete: set is_active to false
        shift.setIsActive(false);
        shiftRepository.save(shift);
        
        // Refresh schedules to remove deleted shift's schedules
        refreshShiftSchedules();
    }
    
    /**
     * Refresh shift reading schedules
     * Safely handles case where scheduler service is not available
     */
    private void refreshShiftSchedules() {
        try {
            if (shiftReadingSchedulerService != null) {
                shiftReadingSchedulerService.refreshSchedules();
            }
        } catch (Exception e) {
            // Log but don't fail the operation
            // Scheduler might not be available during tests or initialization
        }
    }
}
