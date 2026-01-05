package org.facenet.controller.shift;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.pagination.PageRequestDto;
import org.facenet.common.pagination.PageResponseDto;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.shift.ShiftDto;
import org.facenet.service.shift.ShiftService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    /**
     * Get all shifts with pagination and filters
     * Supports filters: isActive, code, name
     * Examples:
     * - /shifts?isActive=true&page=0&size=10
     * - /shifts?name_like=morning&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<PageResponseDto<ShiftDto.Response>>> getAll(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(required = false) Map<String, String> allParams) {
        
        Map<String, String> filters = new java.util.HashMap<>(allParams);
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        filters.remove("search");
        
        if (isActive != null) {
            filters.put("isActive", isActive.toString());
        }
        
        PageRequestDto pageRequest = PageRequestDto.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .search(search)
            .build();
        
        PageResponseDto<ShiftDto.Response> shifts = shiftService.getAll(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(shifts));
    }

    /**
     * Get all shifts without pagination
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<ShiftDto.Response>>> getAllList(
            @RequestParam(value = "isActive", required = false) Boolean isActive) {
        
        if (isActive == null) {
            return ResponseEntity.ok(ApiResponse.success(shiftService.getAll()));
        }
        
        Map<String, String> filters = new java.util.HashMap<>();
        filters.put("isActive", isActive.toString());
        
        PageRequestDto pageRequest = PageRequestDto.builder().page(0).size(10000).build();
        PageResponseDto<ShiftDto.Response> result = shiftService.getAll(pageRequest, filters);
        return ResponseEntity.ok(ApiResponse.success(result.getContent()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<ShiftDto.Response>> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ShiftDto.Response>> create(@Valid @RequestBody ShiftDto.Request request) {
        ShiftDto.Response created = shiftService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ShiftDto.Response>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody ShiftDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        shiftService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
