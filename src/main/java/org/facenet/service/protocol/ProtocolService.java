package org.facenet.service.protocol;

import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.dto.protocol.ProtocolDto;

import java.util.Map;

/**
 * Service interface for Protocol operations
 * Supports 5 core operations: GetList, GetById, Create, Update, Delete
 */
public interface ProtocolService {

    /**
     * Get all protocols with pagination and filters
     * Supports:
     * - search: Search by name, code, description
     * - code: Filter by exact code
     * - connectionType: Filter by connection type
     */
    PageResponseDto<ProtocolDto.Response> getAllProtocols(PageRequestDto pageRequest, Map<String, String> filters);

    /**
     * Get protocol by ID
     */
    ProtocolDto.Response getProtocolById(Long id);

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
}
