package org.facenet.controller.shift;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.shift.ShiftDto;
import org.facenet.service.shift.ShiftService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<List<ShiftDto.Response>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getAll()));
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
