package org.facenet.controller.scale;

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
public class ScaleCurrentStateController {

    private final ScaleCurrentStateService scaleCurrentStateService;

    /**
     * Get current state of all scales with config info
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScaleCurrentStateDto>>> getAllScalesCurrentStates() {
        List<ScaleCurrentStateDto> states = scaleCurrentStateService.getAllScalesWithConfig();
        return ResponseEntity.ok(ApiResponse.success(states));
    }
}
