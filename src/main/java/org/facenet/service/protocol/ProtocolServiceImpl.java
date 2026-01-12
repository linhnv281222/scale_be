package org.facenet.service.protocol;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.specification.GenericSpecification;
import org.facenet.dto.protocol.ProtocolDto;
import org.facenet.entity.protocol.Protocol;
import org.facenet.mapper.ProtocolMapper;
import org.facenet.repository.protocol.ProtocolRepository;
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
 * Service implementation for Protocol operations
 * Implements 5 core operations: GetList, GetById, Create, Update, Delete
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProtocolServiceImpl implements ProtocolService {

    private final ProtocolRepository protocolRepository;

    @Override
    public PageResponseDto<ProtocolDto.Response> getAllProtocols(PageRequestDto pageRequest, Map<String, String> filters) {
        log.debug("Getting protocols with pagination: page={}, size={}, filters={}", 
                  pageRequest.getPage(), pageRequest.getSize(), filters);
        
        GenericSpecification<Protocol> spec = new GenericSpecification<>();
        Specification<Protocol> specification = spec.buildSpecification(filters);
        
        // Add search specification if search keyword is provided
        if (pageRequest.getSearch() != null && !pageRequest.getSearch().isBlank()) {
            Specification<Protocol> searchSpec = spec.buildSearchSpecification(
                pageRequest.getSearch(), "name", "code", "description"
            );
            specification = specification.and(searchSpec);
        }
        
        PageRequest springPageRequest = pageRequest.toPageRequest();
        Page<Protocol> page = protocolRepository.findAll(specification, springPageRequest);
        Page<ProtocolDto.Response> responsePage = page.map(ProtocolMapper::toResponseDto);
        
        return PageResponseDto.from(responsePage);
    }

    @Override
    @Cacheable(value = "protocols", key = "#id")
    public ProtocolDto.Response getProtocolById(@Param("id") Long id) {
        log.debug("Getting protocol by id: {}", id);
        Protocol protocol = protocolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Protocol", "id", id));
        return ProtocolMapper.toResponseDto(protocol);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "protocols", allEntries = true)
    })
    public ProtocolDto.Response createProtocol(ProtocolDto.Request request) {
        log.debug("Creating protocol with code: {}", request.getCode());
        
        // Validate code uniqueness
        if (protocolRepository.existsByCode(request.getCode())) {
            throw new AlreadyExistsException("Protocol", "code", request.getCode());
        }

        Protocol protocol = Protocol.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .connectionType(request.getConnectionType())
                .defaultPort(request.getDefaultPort())
                .defaultBaudRate(request.getDefaultBaudRate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .configTemplate(request.getConfigTemplate())
                .build();

        protocol = protocolRepository.save(protocol);
        log.info("Created protocol with id: {} and code: {}", protocol.getId(), protocol.getCode());
        
        return ProtocolMapper.toResponseDto(protocol);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "protocols", key = "#id"),
        @CacheEvict(value = "protocols", allEntries = true)
    })
    public ProtocolDto.Response updateProtocol(Long id, ProtocolDto.Request request) {
        log.debug("Updating protocol with id: {}", id);
        
        Protocol protocol = protocolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Protocol", "id", id));

        // Validate code uniqueness if changed
        if (!protocol.getCode().equals(request.getCode())) {
            if (protocolRepository.existsByCode(request.getCode())) {
                throw new AlreadyExistsException("Protocol", "code", request.getCode());
            }
            protocol.setCode(request.getCode());
        }

        // Update fields
        protocol.setName(request.getName());
        protocol.setDescription(request.getDescription());
        protocol.setConnectionType(request.getConnectionType());
        protocol.setDefaultPort(request.getDefaultPort());
        protocol.setDefaultBaudRate(request.getDefaultBaudRate());
        protocol.setConfigTemplate(request.getConfigTemplate());
        
        if (request.getIsActive() != null) {
            protocol.setIsActive(request.getIsActive());
        }

        protocol = protocolRepository.save(protocol);
        log.info("Updated protocol with id: {}", id);
        
        return ProtocolMapper.toResponseDto(protocol);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "protocols", key = "#id"),
        @CacheEvict(value = "protocols", allEntries = true)
    })
    public void deleteProtocol(Long id) {
        log.debug("Soft deleting protocol with id: {}", id);
        
        Protocol protocol = protocolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Protocol", "id", id));

        // Soft delete: set is_active to false
        protocol.setIsActive(false);
        protocolRepository.save(protocol);
        
        log.info("Soft deleted protocol with id: {}", id);
    }
}
