package org.facenet.controller.scale;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.facenet.common.response.ApiResponse;
import org.facenet.dto.scale.ScaleCurrentStateDto;
import org.facenet.service.scale.ScaleCurrentStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for scale current state
 */
@RestController
@RequestMapping("/scales/current-states")
@RequiredArgsConstructor
@Tag(name = "Scale Current State", description = "Get current state of scales")
public class ScaleCurrentStateController {

    private final ScaleCurrentStateService scaleCurrentStateService;

    /**
     * Get current state of all scales with config info
     */
    @GetMapping
    @Operation(summary = "Get current states of all scales with config", description = "Returns current state including data values and scale config names")
    public ResponseEntity<ApiResponse<List<ScaleCurrentStateDto>>> getAllScalesCurrentStates() {
        List<ScaleCurrentStateDto> states = scaleCurrentStateService.getAllScalesWithConfig();
        return ResponseEntity.ok(ApiResponse.success(states));
    }
}
