package org.facenet.service.location;

import lombok.RequiredArgsConstructor;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.exception.ValidationException;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.specification.GenericSpecification;
import org.facenet.dto.location.LocationDto;
import org.facenet.entity.location.Location;
import org.facenet.event.LocationChangedEvent;
import org.facenet.mapper.LocationMapper;
import org.facenet.repository.location.LocationRepository;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for Location operations
 * Implements 5 core operations: GetList, GetById, Create, Update, Delete
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final ScaleRepository scaleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResponseDto<LocationDto.Response> getAllLocations(PageRequestDto pageRequest, Map<String, String> filters) {
        GenericSpecification<Location> spec = new GenericSpecification<>();
        Specification<Location> specification = spec.buildSpecification(filters);
        
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            Specification<Location> searchSpec = spec.buildSearchSpecification(
                pageRequest.getSearch(), "name", "code", "description"
            );
            specification = specification.and(searchSpec);
        }
        
        PageRequest springPageRequest = pageRequest.toPageRequest();
        Page<Location> page = locationRepository.findAll(specification, springPageRequest);
        Page<LocationDto.Response> responsePage = page.map(LocationMapper::toFlatResponseDto);
        
        return PageResponseDto.from(responsePage);
    }

    @Override
    @Cacheable(value = "locationsTree")
    public List<LocationDto.Response> getLocationsTree() {
        List<Location> allLocations = locationRepository.findAll();
        return buildTree(allLocations);
    }

    @Override
    @Cacheable(value = "locations", key = "#id")
    public LocationDto.Response getLocationById(@Param("id") Long id) {
        Location location = locationRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));
        return LocationMapper.toResponseDto(location);
    }

    @Override
    @Transactional
    public LocationDto.Response createLocation(LocationDto.Request request) {
        // Validate code is provided for create
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new ValidationException("Location code is required");
        }

        // Validate code uniqueness
        if (locationRepository.existsByCode(request.getCode())) {
            throw new AlreadyExistsException("Location", "code", request.getCode());
        }

        // Validate parent exists if provided
        Location parent = null;
        if (request.getParentId() != null) {
            parent = locationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent location", "id", request.getParentId()));
        }

        Location location = Location.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .parent(parent)
                .build();

        location = locationRepository.save(location);
        
        // Invalidate cache and publish event
        eventPublisher.publishEvent(new LocationChangedEvent(this, location.getId(), "CREATE"));
        
        return LocationMapper.toResponseDto(location);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "locations", key = "#id"),
        @CacheEvict(value = "locations", allEntries = true),
        @CacheEvict(value = "locationsTree", allEntries = true)
    })
    public LocationDto.Response updateLocation(Long id, LocationDto.Request request) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        // Validate parent exists if provided and not self
        Location parent = location.getParent(); // Keep existing parent by default
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new ValidationException("Location cannot be its own parent");
            }
            parent = locationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent location", "id", request.getParentId()));
        }

        // Only update provided fields, never update code (it's immutable)
        if (request.getName() != null) {
            location.setName(request.getName());
        }
        if (request.getDescription() != null) {
            location.setDescription(request.getDescription());
        }
        location.setParent(parent);

        location = locationRepository.save(location);
        
        // Publish event
        eventPublisher.publishEvent(new LocationChangedEvent(this, location.getId(), "UPDATE"));
        
        return LocationMapper.toResponseDto(location);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "locations", key = "#id"),
        @CacheEvict(value = "locations", allEntries = true),
        @CacheEvict(value = "locationsTree", allEntries = true)
    })
    public void deleteLocation(Long id) {
        Location location = locationRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "id", id));

        // Check if has children
        if (location.getChildren() != null && !location.getChildren().isEmpty()) {
            throw new ValidationException("Cannot delete location with sub-locations. Found " + 
                location.getChildren().size() + " sub-location(s)");
        }

        // Check if has scales
        long scaleCount = scaleRepository.findByLocationId(id).size();
        if (scaleCount > 0) {
            throw new ValidationException("Cannot delete location with scales. Found " + scaleCount + " scale(s)");
        }

        // Soft delete: set is_active to false
        location.setIsActive(false);
        locationRepository.save(location);
        
        // Publish event
        eventPublisher.publishEvent(new LocationChangedEvent(this, id, "SOFT_DELETE"));
    }

    private List<LocationDto.Response> buildTree(List<Location> locations) {
        Map<Long, LocationDto.Response> locationMap = locations.stream()
                .collect(Collectors.toMap(Location::getId, LocationMapper::toResponseDto));

        List<LocationDto.Response> roots = locations.stream()
                .filter(loc -> loc.getParent() == null)
                .map(LocationMapper::toResponseDto)
                .collect(Collectors.toList());

        for (Location location : locations) {
            if (location.getParent() != null) {
                LocationDto.Response parentDto = locationMap.get(location.getParent().getId());
                if (parentDto != null && parentDto.getChildren() != null) {
                    parentDto.getChildren().add(locationMap.get(location.getId()));
                }
            }
        }

        return roots;
    }
}