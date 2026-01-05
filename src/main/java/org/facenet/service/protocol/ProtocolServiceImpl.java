package org.facenet.service.protocol;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.common.exception.AlreadyExistsException;
import org.facenet.common.exception.ResourceNotFoundException;
import org.facenet.dto.protocol.ProtocolDto;
import org.facenet.entity.protocol.Protocol;
import org.facenet.mapper.ProtocolMapper;
import org.facenet.repository.protocol.ProtocolRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Protocol operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProtocolServiceImpl implements ProtocolService {

    private final ProtocolRepository protocolRepository;

    @Override
    @Cacheable(value = "protocols")
    public List<ProtocolDto.Response> getAllProtocols() {
        log.debug("Getting all protocols");
        List<Protocol> protocols = protocolRepository.findAll();
        return ProtocolMapper.toResponseDtoList(protocols);
    }

    @Override
    @Cacheable(value = "activeProtocols")
    public List<ProtocolDto.Response> getAllActiveProtocols() {
        log.debug("Getting all active protocols");
        List<Protocol> protocols = protocolRepository.findAllActive();
        return ProtocolMapper.toResponseDtoList(protocols);
    }

    @Override
    @Cacheable(value = "protocols", key = "#id")
    public ProtocolDto.Response getProtocolById(Long id) {
        log.debug("Getting protocol by id: {}", id);
        Protocol protocol = protocolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Protocol", "id", id));
        return ProtocolMapper.toResponseDto(protocol);
    }

    @Override
    @Cacheable(value = "protocols", key = "#code")
    public ProtocolDto.Response getProtocolByCode(String code) {
        log.debug("Getting protocol by code: {}", code);
        Protocol protocol = protocolRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Protocol", "code", code));
        return ProtocolMapper.toResponseDto(protocol);
    }

    @Override
    public List<ProtocolDto.Response> searchProtocols(String search) {
        log.debug("Searching protocols with keyword: {}", search);
        if (search == null || search.isBlank()) {
            return getAllProtocols();
        }
        List<Protocol> protocols = protocolRepository.searchByNameOrCode(search);
        return ProtocolMapper.toResponseDtoList(protocols);
    }

    @Override
    public List<ProtocolDto.Response> getProtocolsByConnectionType(String connectionType) {
        log.debug("Getting protocols by connection type: {}", connectionType);
        List<Protocol> protocols = protocolRepository.findByConnectionType(connectionType);
        return ProtocolMapper.toResponseDtoList(protocols);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "protocols", allEntries = true),
        @CacheEvict(value = "activeProtocols", allEntries = true)
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
        @CacheEvict(value = "protocols", allEntries = true),
        @CacheEvict(value = "activeProtocols", allEntries = true)
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
        @CacheEvict(value = "protocols", allEntries = true),
        @CacheEvict(value = "activeProtocols", allEntries = true)
    })
    public void deleteProtocol(Long id) {
        log.debug("Deleting protocol with id: {}", id);
        
        Protocol protocol = protocolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Protocol", "id", id));

        // TODO: Check if protocol is used by any scales before deleting
        
        protocolRepository.delete(protocol);
        log.info("Deleted protocol with id: {}", id);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "protocols", key = "#id"),
        @CacheEvict(value = "protocols", allEntries = true),
        @CacheEvict(value = "activeProtocols", allEntries = true)
    })
    public ProtocolDto.Response toggleActiveStatus(Long id) {
        log.debug("Toggling active status for protocol with id: {}", id);
        
        Protocol protocol = protocolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Protocol", "id", id));

        protocol.setIsActive(!protocol.getIsActive());
        protocol = protocolRepository.save(protocol);
        
        log.info("Toggled active status for protocol with id: {} to {}", id, protocol.getIsActive());
        
        return ProtocolMapper.toResponseDto(protocol);
    }
}
