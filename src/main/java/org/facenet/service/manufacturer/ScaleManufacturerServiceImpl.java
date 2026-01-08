package org.facenet.service.manufacturer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.specification.GenericSpecification;
import org.facenet.dto.manufacturer.ScaleManufacturerDto;
import org.facenet.entity.manufacturer.ScaleManufacturer;
import org.facenet.mapper.ScaleManufacturerMapper;
import org.facenet.repository.manufacturer.ScaleManufacturerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service implementation for ScaleManufacturer operations
 * Implements 5 core operations: GetList, GetById, Create, Update, Delete
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScaleManufacturerServiceImpl implements ScaleManufacturerService {

    private final ScaleManufacturerRepository manufacturerRepository;

    @Override
    public PageResponseDto<ScaleManufacturerDto.Response> getAllManufacturers(PageRequestDto pageRequest, Map<String, String> filters) {
        log.debug("Getting manufacturers with pagination: page={}, size={}, filters={}", 
                  pageRequest.getPage(), pageRequest.getSize(), filters);
        
        GenericSpecification<ScaleManufacturer> spec = new GenericSpecification<>();
        Specification<ScaleManufacturer> specification = spec.buildSpecification(filters);
        
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            Specification<ScaleManufacturer> searchSpec = spec.buildSearchSpecification(
                pageRequest.getSearch(), "name", "code", "country", "description"
            );
            specification = specification.and(searchSpec);
        }
        
        PageRequest springPageRequest = pageRequest.toPageRequest();
        Page<ScaleManufacturer> page = manufacturerRepository.findAll(specification, springPageRequest);
        Page<ScaleManufacturerDto.Response> responsePage = page.map(ScaleManufacturerMapper::toResponseDto);
        
        return PageResponseDto.from(responsePage);
    }

    @Override
    @Cacheable(value = "manufacturers", key = "#id")
    public ScaleManufacturerDto.Response getManufacturerById(@Param("id") Long id) {
        log.debug("Getting manufacturer by id: {}", id);
        ScaleManufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", "id", id));
        return ScaleManufacturerMapper.toResponseDto(manufacturer);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manufacturers", allEntries = true)
    })
    public ScaleManufacturerDto.Response createManufacturer(ScaleManufacturerDto.Request request) {
        log.debug("Creating manufacturer with code: {}", request.getCode());
        
        // Validate code uniqueness
        if (manufacturerRepository.existsByCode(request.getCode())) {
            throw new AlreadyExistsException("Manufacturer", "code", request.getCode());
        }

        ScaleManufacturer manufacturer = ScaleManufacturer.builder()
                .code(request.getCode())
                .name(request.getName())
                .country(request.getCountry())
                .website(request.getWebsite())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        manufacturer = manufacturerRepository.save(manufacturer);
        log.info("Created manufacturer with id: {} and code: {}", manufacturer.getId(), manufacturer.getCode());
        
        return ScaleManufacturerMapper.toResponseDto(manufacturer);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manufacturers", key = "#id"),
        @CacheEvict(value = "manufacturers", allEntries = true)
    })
    public ScaleManufacturerDto.Response updateManufacturer(Long id, ScaleManufacturerDto.Request request) {
        log.debug("Updating manufacturer with id: {}", id);
        
        ScaleManufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", "id", id));

        // Validate code uniqueness if changed
        if (!manufacturer.getCode().equals(request.getCode())) {
            if (manufacturerRepository.existsByCode(request.getCode())) {
                throw new AlreadyExistsException("Manufacturer", "code", request.getCode());
            }
            manufacturer.setCode(request.getCode());
        }

        // Update fields
        manufacturer.setName(request.getName());
        manufacturer.setCountry(request.getCountry());
        manufacturer.setWebsite(request.getWebsite());
        manufacturer.setPhone(request.getPhone());
        manufacturer.setEmail(request.getEmail());
        manufacturer.setAddress(request.getAddress());
        manufacturer.setDescription(request.getDescription());
        
        if (request.getIsActive() != null) {
            manufacturer.setIsActive(request.getIsActive());
        }

        manufacturer = manufacturerRepository.save(manufacturer);
        log.info("Updated manufacturer with id: {}", id);
        
        return ScaleManufacturerMapper.toResponseDto(manufacturer);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manufacturers", key = "#id"),
        @CacheEvict(value = "manufacturers", allEntries = true)
    })
    public void deleteManufacturer(Long id) {
        log.debug("Deleting manufacturer with id: {}", id);
        
        ScaleManufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", "id", id));

        // TODO: Check if manufacturer is used by any scales before deleting
        
        manufacturerRepository.delete(manufacturer);
        log.info("Deleted manufacturer with id: {}", id);
    }
}
