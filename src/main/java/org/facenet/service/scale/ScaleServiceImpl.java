package org.facenet.service.scale;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.dto.scale.ScaleConfigDto;
import org.facenet.dto.scale.ScaleDto;
import org.facenet.entity.location.Location;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.ConfigChangedEvent;
import org.facenet.mapper.ScaleMapper;
import org.facenet.repository.location.LocationRepository;
import org.facenet.repository.scale.ScaleConfigRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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

        Scale scale = Scale.builder()
                .name(request.getName())
                .location(location)
                .model(request.getModel())
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

        scale.setName(request.getName());
        scale.setLocation(location);
        scale.setModel(request.getModel());
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
        ScaleConfig config = scaleConfigRepository.findById(scaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Scale config", "scaleId", scaleId));

        config.setProtocol(request.getProtocol());
        config.setPollInterval(request.getPollInterval() != null ? request.getPollInterval() : 1000);
        config.setConnParams(request.getConnParams());
        config.setData1(request.getData1());
        config.setData2(request.getData2());
        config.setData3(request.getData3());
        config.setData4(request.getData4());
        config.setData5(request.getData5());

        config = scaleConfigRepository.save(config);

        // Publish event for hot-reload and cache invalidation
        eventPublisher.publishEvent(new ConfigChangedEvent(this, scaleId, "CONFIG_UPDATE"));

        return ScaleMapper.toConfigDto(config);
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