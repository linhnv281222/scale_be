package org.facenet.controller.protocol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.protocol.ProtocolDto;
import org.facenet.service.protocol.ProtocolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Get all protocols
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get all protocols", description = "Retrieve all communication protocols")
    public ResponseEntity<ApiResponse<List<ProtocolDto.Response>>> getAllProtocols() {
        List<ProtocolDto.Response> protocols = protocolService.getAllProtocols();
        return ResponseEntity.ok(ApiResponse.success(protocols));
    }

    /**
     * Get all active protocols
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get active protocols", description = "Retrieve all active communication protocols")
    public ResponseEntity<ApiResponse<List<ProtocolDto.Response>>> getAllActiveProtocols() {
        List<ProtocolDto.Response> protocols = protocolService.getAllActiveProtocols();
        return ResponseEntity.ok(ApiResponse.success(protocols));
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
     * Search protocols by name or code
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Search protocols", description = "Search protocols by name or code")
    public ResponseEntity<ApiResponse<List<ProtocolDto.Response>>> searchProtocols(
            @RequestParam("q") String search) {
        List<ProtocolDto.Response> protocols = protocolService.searchProtocols(search);
        return ResponseEntity.ok(ApiResponse.success(protocols));
    }

    /**
     * Get protocols by connection type
     */
    @GetMapping("/connection-type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @Operation(summary = "Get protocols by connection type", 
               description = "Retrieve protocols filtered by connection type (TCP, SERIAL, etc.)")
    public ResponseEntity<ApiResponse<List<ProtocolDto.Response>>> getProtocolsByConnectionType(
            @PathVariable("type") String connectionType) {
        List<ProtocolDto.Response> protocols = protocolService.getProtocolsByConnectionType(connectionType);
        return ResponseEntity.ok(ApiResponse.success(protocols));
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
