package org.facenet.service.scale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.specification.GenericSpecification;
import org.facenet.dto.scale.ScaleDto;
import org.facenet.entity.location.Location;
import org.facenet.entity.manufacturer.ScaleManufacturer;
import org.facenet.entity.protocol.Protocol;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.entity.scale.ScaleDirection;
import org.facenet.event.ConfigChangedEvent;
import org.facenet.mapper.ScaleMapper;
import org.facenet.repository.location.LocationRepository;
import org.facenet.repository.manufacturer.ScaleManufacturerRepository;
import org.facenet.repository.protocol.ProtocolRepository;
import org.facenet.repository.scale.ScaleConfigRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

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
    private final ProtocolRepository protocolRepository;
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
    @Cacheable(value = "scales", key = "#id")
    public ScaleDto.Response getScaleById(@Param("id") Long id) {
        Scale scale = scaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scale", "id", id));
        return ScaleMapper.toResponseDto(scale);
    }

    @Override
    @Transactional
    @CacheEvict(value = "scales", allEntries = true)
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

        // Validate protocol exists if provided
        Protocol protocol = null;
        if (request.getProtocolId() != null) {
            protocol = protocolRepository.findById(request.getProtocolId())
                    .orElseThrow(() -> new ResourceNotFoundException("Protocol", "id", request.getProtocolId()));
        }

        Scale scale = Scale.builder()
                .name(request.getName())
                .location(location)
                .manufacturer(manufacturer)
                .protocol(protocol)
                .model(request.getModel())
                .direction(request.getDirection() != null ? ScaleDirection.valueOf(request.getDirection()) : null)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        scale = scaleRepository.save(scale);

        // Create config with data from request
        createScaleConfig(scale, request);

        // Publish event for cache invalidation
        eventPublisher.publishEvent(new ConfigChangedEvent(this, scale.getId(), "SCALE_CREATE"));

        return ScaleMapper.toResponseDto(scale);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "scales", key = "#id"),
        @CacheEvict(value = "scales", allEntries = true)
    })
    public ScaleDto.Response updateScale(@Param("id") Long id, ScaleDto.Request request) {
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

        // Validate protocol exists if provided
        Protocol protocol = null;
        if (request.getProtocolId() != null) {
            protocol = protocolRepository.findById(request.getProtocolId())
                    .orElseThrow(() -> new ResourceNotFoundException("Protocol", "id", request.getProtocolId()));
        }

        scale.setName(request.getName());
        scale.setLocation(location);
        scale.setManufacturer(manufacturer);
        scale.setProtocol(protocol);
        scale.setModel(request.getModel());
        scale.setDirection(request.getDirection() != null ? ScaleDirection.valueOf(request.getDirection()) : null);
        scale.setIsActive(request.getIsActive());

        scale = scaleRepository.save(scale);
        
        // Update config with data from request
        updateScaleConfig(scale, request);
        
        // Publish event
        eventPublisher.publishEvent(new ConfigChangedEvent(this, scale.getId(), "SCALE_UPDATE"));
        
        return ScaleMapper.toResponseDto(scale);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "scales", key = "#id"),
        @CacheEvict(value = "scales", allEntries = true)
    })
    public void deleteScale(@Param("id") Long id) {
        Scale scale = scaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scale", "id", id));

        // Soft delete: set is_active to false
        scale.setIsActive(false);
        scaleRepository.save(scale);
        
        // Publish event
        eventPublisher.publishEvent(new ConfigChangedEvent(this, id, "SCALE_SOFT_DELETE"));
    }

    private void createScaleConfig(Scale scale, ScaleDto.Request request) {
        try {
            // Use values from request or defaults
            String protocol = request.getProtocol() != null ? request.getProtocol() : "MODBUS_TCP";
            Integer pollInterval = request.getPollInterval() != null ? request.getPollInterval() : 1000;
            
            Map<String, Object> connParams = request.getConnParams() != null ? request.getConnParams() : createDefaultConnParams();
            Map<String, Object> data1 = request.getData1() != null ? request.getData1() : createDefaultData1();
            Map<String, Object> data2 = request.getData2() != null ? request.getData2() : Map.of("is_used", false);
            Map<String, Object> data3 = request.getData3() != null ? request.getData3() : Map.of("is_used", false);
            Map<String, Object> data4 = request.getData4() != null ? request.getData4() : Map.of("is_used", false);
            Map<String, Object> data5 = request.getData5() != null ? request.getData5() : Map.of("is_used", false);

            ScaleConfig config = ScaleConfig.builder()
                    .scale(scale)
                    .protocol(protocol)
                    .pollInterval(pollInterval)
                    .connParams(connParams)
                    .data1(data1)
                    .data2(data2)
                    .data3(data3)
                    .data4(data4)
                    .data5(data5)
                    .build();

            scaleConfigRepository.save(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create scale config", e);
        }
    }

    private void updateScaleConfig(Scale scale, ScaleDto.Request request) {
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
                scale.getId(),
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
                throw new ResourceNotFoundException("Scale config", "scaleId", scale.getId());
            }

            // Clear entity manager cache to ensure fresh data
            entityManager.clear();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize config data", e);
        }
    }

    private Map<String, Object> createDefaultConnParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("ip", "192.168.1.10");
        params.put("port", 502);
        return params;
    }

    private Map<String, Object> createDefaultData1() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Weight");
        data.put("start_registers", 40001);
        data.put("num_registers", 2);
        data.put("is_used", true);
        return data;
    }
}