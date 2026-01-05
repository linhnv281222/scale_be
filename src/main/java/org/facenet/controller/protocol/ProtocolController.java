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

import java.util.List;
import java.util.Map;

/**
 * Controller for Protocol operations
 */
@RestController
@RequestMapping("/protocols")
@RequiredArgsConstructor
@Tag(name = "Protocol Management", description = "APIs for managing communication protocols")
public class ProtocolController {

    private final ProtocolService protocolService;

    /**
     * Get all protocols with pagination and filters
     * Supports filters: connectionType, isActive, code, name
     * Examples:
     * - /protocols?connectionType=TCP&isActive=true
     * - /protocols?code_like=MODBUS&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all protocols", description = "Retrieve all protocols with pagination and filters. Supports: connectionType, isActive, code, name, defaultPort")
    public ResponseEntity<ApiResponse<PageResponseDto<ProtocolDto.Response>>> getAllProtocols(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "connectionType", required = false) String connectionType,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(required = false) Map<String, String> allParams) {
        
        Map<String, String> filters = new java.util.HashMap<>(allParams);
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        filters.remove("search");
        
        if (connectionType != null) {
            filters.put("connectionType", connectionType);
        }
        if (isActive != null) {
            filters.put("isActive", isActive.toString());
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
     * Get all protocols without pagination
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all protocols", description = "Retrieve all protocols without pagination. Supports same filters")
    public ResponseEntity<ApiResponse<List<ProtocolDto.Response>>> getAllProtocolsList(
            @RequestParam(value = "connectionType", required = false) String connectionType,
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        
        if (connectionType == null && isActive == null) {
            return ResponseEntity.ok(ApiResponse.success(protocolService.getAllProtocols()));
        }
        
        Map<String, String> filters = new java.util.HashMap<>();
        if (connectionType != null) {
            filters.put("connectionType", connectionType);
        }
        if (isActive != null) {
            filters.put("isActive", isActive.toString());
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder().page(0).size(10000).build();
        PageResponseDto<ProtocolDto.Response> result = protocolService.getAllProtocols(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(result.getContent()));
    }

    /**
     * Get protocol by ID
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
     * Get protocol by code
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get protocol by code", description = "Retrieve a protocol by its code")
    public ResponseEntity<ApiResponse<ProtocolDto.Response>> getProtocolByCode(
            @PathVariable("code") String code) {
        ProtocolDto.Response protocol = protocolService.getProtocolByCode(code);
        return ResponseEntity.ok(ApiResponse.success(protocol));
    }

    /**
     * Create a new protocol
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
     * Update protocol
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
     * Delete protocol
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete protocol", description = "Delete a communication protocol")
    public ResponseEntity<Void> deleteProtocol(
            @PathVariable("id") Long id) {
        protocolService.deleteProtocol(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle protocol active status
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Toggle protocol status", description = "Toggle the active status of a protocol")
    public ResponseEntity<ApiResponse<ProtocolDto.Response>> toggleActiveStatus(
            @PathVariable("id") Long id) {
        ProtocolDto.Response protocol = protocolService.toggleActiveStatus(id);
        return ResponseEntity.ok(ApiResponse.success(protocol));
    }
}
