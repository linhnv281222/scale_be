package org.facenet.service.protocol;

import org.facenet.dto.protocol.ProtocolDto;

import java.util.List;

/**
 * Service interface for Protocol operations
 */
public interface ProtocolService {

    /**
     * Get all protocols
     */
    List<ProtocolDto.Response> getAllProtocols();

    /**
     * Get all active protocols
     */
    List<ProtocolDto.Response> getAllActiveProtocols();

    /**
     * Get protocol by ID
     */
    ProtocolDto.Response getProtocolById(Long id);

    /**
     * Get protocol by code
     */
    ProtocolDto.Response getProtocolByCode(String code);

    /**
     * Search protocols by name or code
     */
    List<ProtocolDto.Response> searchProtocols(String search);

    /**
     * Get protocols by connection type
     */
    List<ProtocolDto.Response> getProtocolsByConnectionType(String connectionType);

    /**
     * Create a new protocol
     */
    ProtocolDto.Response createProtocol(ProtocolDto.Request request);

    /**
     * Update protocol
     */
    ProtocolDto.Response updateProtocol(Long id, ProtocolDto.Request request);

    /**
     * Delete protocol
     */
    void deleteProtocol(Long id);

    /**
     * Toggle protocol active status
     */
    ProtocolDto.Response toggleActiveStatus(Long id);
}
