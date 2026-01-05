package org.facenet.service.manufacturer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.exception.ValidationException;
import org.facenet.dto.manufacturer.ScaleManufacturerDto;
import org.facenet.entity.manufacturer.ScaleManufacturer;
import org.facenet.mapper.ScaleManufacturerMapper;
import org.facenet.repository.manufacturer.ScaleManufacturerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for ScaleManufacturer operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScaleManufacturerServiceImpl implements ScaleManufacturerService {

    private final ScaleManufacturerRepository manufacturerRepository;

    @Override
    @Cacheable(value = "manufacturers")
    public List<ScaleManufacturerDto.Response> getAllManufacturers() {
        log.debug("Getting all manufacturers");
        List<ScaleManufacturer> manufacturers = manufacturerRepository.findAll();
        return ScaleManufacturerMapper.toResponseDtoList(manufacturers);
    }

    @Override
    @Cacheable(value = "activeManufacturers")
    public List<ScaleManufacturerDto.Response> getAllActiveManufacturers() {
        log.debug("Getting all active manufacturers");
        List<ScaleManufacturer> manufacturers = manufacturerRepository.findAllActive();
        return ScaleManufacturerMapper.toResponseDtoList(manufacturers);
    }

    @Override
    @Cacheable(value = "manufacturers", key = "#id")
    public ScaleManufacturerDto.Response getManufacturerById(Long id) {
        log.debug("Getting manufacturer by id: {}", id);
        ScaleManufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", "id", id));
        return ScaleManufacturerMapper.toResponseDto(manufacturer);
    }

    @Override
    public List<ScaleManufacturerDto.Response> searchManufacturers(String search) {
        log.debug("Searching manufacturers with keyword: {}", search);
        if (search == null || search.isBlank()) {
            return getAllManufacturers();
        }
        List<ScaleManufacturer> manufacturers = manufacturerRepository.searchByNameOrCode(search);
        return ScaleManufacturerMapper.toResponseDtoList(manufacturers);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manufacturers", allEntries = true),
        @CacheEvict(value = "activeManufacturers", allEntries = true)
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
        @CacheEvict(value = "manufacturers", allEntries = true),
        @CacheEvict(value = "activeManufacturers", allEntries = true)
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
        @CacheEvict(value = "manufacturers", allEntries = true),
        @CacheEvict(value = "activeManufacturers", allEntries = true)
    })
    public void deleteManufacturer(Long id) {
        log.debug("Deleting manufacturer with id: {}", id);
        
        ScaleManufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", "id", id));

        // TODO: Check if manufacturer is used by any scales before deleting
        // This would require adding relationship with Scale entity
        
        manufacturerRepository.delete(manufacturer);
        log.info("Deleted manufacturer with id: {}", id);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manufacturers", key = "#id"),
        @CacheEvict(value = "manufacturers", allEntries = true),
        @CacheEvict(value = "activeManufacturers", allEntries = true)
    })
    public ScaleManufacturerDto.Response toggleActiveStatus(Long id) {
        log.debug("Toggling active status for manufacturer with id: {}", id);
        
        ScaleManufacturer manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", "id", id));

        manufacturer.setIsActive(!manufacturer.getIsActive());
        manufacturer = manufacturerRepository.save(manufacturer);
        
        log.info("Toggled active status for manufacturer with id: {} to {}", id, manufacturer.getIsActive());
        
        return ScaleManufacturerMapper.toResponseDto(manufacturer);
    }
}
