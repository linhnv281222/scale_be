package org.facenet.controller.protocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.protocol.ProtocolDto;
import org.facenet.service.protocol.ProtocolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Protocol operations
 * Contains exactly 5 APIs: Create, Update, Delete, GetById, GetList
 */
@RestController
@RequestMapping("/protocols")
@RequiredArgsConstructor
@Tag(name = "Protocol Management", description = "APIs for managing communication protocols")
public class ProtocolController {

    private final ProtocolService protocolService;

    /**
     * 1. Get all protocols with pagination and filters
     * Supports:
     * - search: Search by name, code, description
     * - code: Filter by exact code
     * - connectionType: Filter by connection type
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all protocols", 
               description = "Get list of protocols with pagination. Supports search (name/code/description), filter by code and connectionType")
    public ResponseEntity<ApiResponse<?>> getAllProtocols(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "connectionType", required = false) String connectionType) {
        
        Map<String, String> filters = new java.util.HashMap<>();
        if (code != null) {
            filters.put("code", code);
        }
        if (connectionType != null) {
            filters.put("connectionType", connectionType);
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .search(search)
            .build();
        
        PageResponseDto<ProtocolDto.Response> protocols = protocolService.getAllProtocols(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(protocols));
    }

    /**
     * 2. Get protocol by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get protocol by ID", description = "Retrieve a protocol by its ID")
    public ResponseEntity<ApiResponse<ProtocolDto.Response>> getProtocolById(
            @PathVariable("id") Long id) {
        ProtocolDto.Response protocol = protocolService.getProtocolById(id);
        return ResponseEntity.ok(ApiResponse.success(protocol));
    }

    /**
     * 3. Create a new protocol
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Create protocol", description = "Create a new communication protocol")
    public ResponseEntity<ApiResponse<ProtocolDto.Response>> createProtocol(
            @Valid @RequestBody ProtocolDto.Request request) {
        ProtocolDto.Response protocol = protocolService.createProtocol(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(protocol));
    }

    /**
     * 4. Update protocol
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Update protocol", description = "Update an existing communication protocol")
    public ResponseEntity<ApiResponse<ProtocolDto.Response>> updateProtocol(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProtocolDto.Request request) {
        ProtocolDto.Response protocol = protocolService.updateProtocol(id, request);
        return ResponseEntity.ok(ApiResponse.success(protocol));
    }

    /**
     * 5. Delete protocol (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete protocol (soft delete)", description = "Soft delete a communication protocol (sets is_active to false)")
    public ResponseEntity<Void> deleteProtocol(
            @PathVariable("id") Long id) {
        protocolService.deleteProtocol(id);
        return ResponseEntity.noContent().build();
    }
}
