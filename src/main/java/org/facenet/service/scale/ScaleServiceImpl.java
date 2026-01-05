package org.facenet.service.scale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.specification.GenericSpecification;
import org.facenet.dto.scale.ScaleConfigDto;
import org.facenet.dto.scale.ScaleDto;
import org.facenet.entity.location.Location;
import org.facenet.entity.manufacturer.ScaleManufacturer;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.ConfigChangedEvent;
import org.facenet.mapper.ScaleMapper;
import org.facenet.repository.location.LocationRepository;
import org.facenet.repository.manufacturer.ScaleManufacturerRepository;
import org.facenet.repository.scale.ScaleConfigRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for Scale operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScaleServiceImpl implements ScaleService {

    private final ScaleRepository scaleRepository;
    private final ScaleConfigRepository scaleConfigRepository;
    private final LocationRepository locationRepository;
    private final ScaleManufacturerRepository manufacturerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponseDto<ScaleDto.Response> getAllScales(PageRequestDto pageRequest, Map<String, String> filters) {
        GenericSpecification<Scale> spec = new GenericSpecification<>();
        Specification<Scale> specification = spec.buildSpecification(filters);
        
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            Specification<Scale> searchSpec = spec.buildSearchSpecification(
                pageRequest.getSearch(), "name", "model"
            );
            specification = specification.and(searchSpec);
        }
        
        PageRequest springPageRequest = pageRequest.toPageRequest();
        Page<Scale> page = scaleRepository.findAll(specification, springPageRequest);
        Page<ScaleDto.Response> responsePage = page.map(ScaleMapper::toResponseDto);
        
        return PageResponseDto.from(responsePage);
    }

    @Override
    @Cacheable(value = "scales")
    public List<ScaleDto.Response> getAllScales() {
        List<Scale> scales = scaleRepository.findAll();
        return scales.stream()
                .map(ScaleMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "scalesByLocation", key = "#locationId")
    public List<ScaleDto.Response> getScalesByLocation(Long locationId) {
        List<Scale> scales = scaleRepository.findByLocationId(locationId);
        return scales.stream()
                .map(ScaleMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScaleDto.Response> getScalesByLocations(List<Long> locationIds) {
        List<Scale> scales = scaleRepository.findByLocationIdIn(locationIds);
        return scales.stream()
                .map(ScaleMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScaleDto.Response> getScalesByManufacturer(Long manufacturerId) {
        List<Scale> scales = scaleRepository.findByManufacturerId(manufacturerId);
        return scales.stream()
                .map(ScaleMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScaleDto.Response> getScalesByDirection(String direction) {
        org.facenet.entity.scale.ScaleDirection scaleDirection = 
            org.facenet.entity.scale.ScaleDirection.valueOf(direction);
        List<Scale> scales = scaleRepository.findByDirection(scaleDirection);
        return scales.stream()
                .map(ScaleMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScaleDto.Response> getActiveScales() {
        List<Scale> scales = scaleRepository.findByIsActive(true);
        return scales.stream()
                .map(ScaleMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "scales", key = "#id")
    public ScaleDto.Response getScaleById(Long id) {
        Scale scale = scaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scale", "id", id));
        return ScaleMapper.toResponseDto(scale);
    }

    @Override
    @Transactional
    public ScaleDto.Response createScale(ScaleDto.Request request) {
        // Validate location exists
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", request.getLocationId()));

        // Validate manufacturer exists if provided
        ScaleManufacturer manufacturer = null;
        if (request.getManufacturerId() != null) {
            manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", "id", request.getManufacturerId()));
        }

        Scale scale = Scale.builder()
                .name(request.getName())
                .location(location)
                .manufacturer(manufacturer)
                .model(request.getModel())
                .direction(request.getDirection() != null ? org.facenet.entity.scale.ScaleDirection.valueOf(request.getDirection()) : null)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        scale = scaleRepository.save(scale);

        // Create default config
        createDefaultConfig(scale);

        // Publish event for cache invalidation
        eventPublisher.publishEvent(new ConfigChangedEvent(this, scale.getId(), "SCALE_CREATE"));

        return ScaleMapper.toResponseDto(scale);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "scales", key = "#id"),
        @CacheEvict(value = "scales", allEntries = true),
        @CacheEvict(value = "scalesByLocation", allEntries = true)
    })
    public ScaleDto.Response updateScale(Long id, ScaleDto.Request request) {
        Scale scale = scaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scale", "id", id));

        // Validate location exists
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", request.getLocationId()));

        // Validate manufacturer exists if provided
        ScaleManufacturer manufacturer = null;
        if (request.getManufacturerId() != null) {
            manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", "id", request.getManufacturerId()));
        }

        scale.setName(request.getName());
        scale.setLocation(location);
        scale.setManufacturer(manufacturer);
        scale.setModel(request.getModel());
        scale.setDirection(request.getDirection() != null ? org.facenet.entity.scale.ScaleDirection.valueOf(request.getDirection()) : null);
        scale.setIsActive(request.getIsActive());

        scale = scaleRepository.save(scale);
        
        // Publish event
        eventPublisher.publishEvent(new ConfigChangedEvent(this, scale.getId(), "SCALE_UPDATE"));
        
        return ScaleMapper.toResponseDto(scale);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "scales", key = "#id"),
        @CacheEvict(value = "scales", allEntries = true),
        @CacheEvict(value = "scalesByLocation", allEntries = true),
        @CacheEvict(value = "scaleConfig", key = "#id")
    })
    public void deleteScale(Long id) {
        Scale scale = scaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scale", "id", id));

        // Config and current state will be deleted via cascade
        scaleRepository.delete(scale);
        
        // Publish event
        eventPublisher.publishEvent(new ConfigChangedEvent(this, id, "SCALE_DELETE"));
    }

    @Override
    @Cacheable(value = "scaleConfig", key = "#scaleId")
    public ScaleConfigDto.Response getScaleConfig(Long scaleId) {
        ScaleConfig config = scaleConfigRepository.findById(scaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Scale config", "scaleId", scaleId));
        return ScaleMapper.toConfigDto(config);
    }

    @Override
    @Transactional
    @CacheEvict(value = "scaleConfig", key = "#scaleId")
    public ScaleConfigDto.Response updateScaleConfig(Long scaleId, ScaleConfigDto.Request request) {
        try {
            // Convert Maps to JSON strings for native query
            String connParamsJson = request.getConnParams() != null ? 
                objectMapper.writeValueAsString(request.getConnParams()) : null;
            String data1Json = request.getData1() != null ? 
                objectMapper.writeValueAsString(request.getData1()) : null;
            String data2Json = request.getData2() != null ? 
                objectMapper.writeValueAsString(request.getData2()) : null;
            String data3Json = request.getData3() != null ? 
                objectMapper.writeValueAsString(request.getData3()) : null;
            String data4Json = request.getData4() != null ? 
                objectMapper.writeValueAsString(request.getData4()) : null;
            String data5Json = request.getData5() != null ? 
                objectMapper.writeValueAsString(request.getData5()) : null;

            // Use native query to update - bypasses @MapsId issues
            int updatedRows = scaleConfigRepository.updateScaleConfig(
                scaleId,
                request.getProtocol(),
                request.getPollInterval() != null ? request.getPollInterval() : 1000,
                connParamsJson,
                data1Json,
                data2Json,
                data3Json,
                data4Json,
                data5Json,
                "system" // TODO: Get from security context
            );

            if (updatedRows == 0) {
                throw new ResourceNotFoundException("Scale config", "scaleId", scaleId);
            }

            // Clear entity manager cache to ensure fresh data
            entityManager.clear();

            // Fetch updated config
            ScaleConfig updatedConfig = scaleConfigRepository.findById(scaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Scale config", "scaleId", scaleId));

            // Publish event for hot-reload and cache invalidation
            eventPublisher.publishEvent(new ConfigChangedEvent(this, scaleId, "CONFIG_UPDATE"));

            return ScaleMapper.toConfigDto(updatedConfig);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize config data", e);
        }
    }

    private void createDefaultConfig(Scale scale) {
        Map<String, Object> defaultConnParams = new HashMap<>();
        defaultConnParams.put("ip", "192.168.1.10");
        defaultConnParams.put("port", 502);

        Map<String, Object> defaultData1 = new HashMap<>();
        defaultData1.put("name", "Weight");
        defaultData1.put("start_registers", 40001);
        defaultData1.put("num_registers", 2);
        defaultData1.put("is_used", true);

        ScaleConfig config = ScaleConfig.builder()
                .scale(scale)
                .protocol("MODBUS_TCP")
                .pollInterval(1000)
                .connParams(defaultConnParams)
                .data1(defaultData1)
                .data2(Map.of("is_used", false))
                .data3(Map.of("is_used", false))
                .data4(Map.of("is_used", false))
                .data5(Map.of("is_used", false))
                .build();

        scaleConfigRepository.save(config);
    }
}