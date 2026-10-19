package org.facenet.service.scale;

import lombok.RequiredArgsConstructor;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.entity.scale.ScaleConfigAuditLog;
import org.facenet.repository.scale.ScaleConfigAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScaleConfigAuditService {
    private final ScaleConfigAuditLogRepository repository;

    @Transactional
    public void record(Long scaleId, String action, Map<String, Object> before, Map<String, Object> after) {
        repository.save(ScaleConfigAuditLog.builder().scaleId(scaleId).action(action)
                .beforeConfig(before).afterConfig(after).changedBy(currentUsername())
                .changedAt(OffsetDateTime.now()).build());
    }

    @Transactional(readOnly = true)
    public Page<ScaleConfigAuditLog> history(Long scaleId, int page, int size) {
        return repository.findByScaleIdOrderByChangedAtDesc(scaleId,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 200)));
    }

    public Map<String, Object> snapshot(ScaleConfig config) {
        if (config == null) return null;
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("protocol", config.getProtocol());
        value.put("poll_interval", config.getPollInterval());
        value.put("conn_params", copy(config.getConnParams()));
        value.put("data_1", copy(config.getData1()));
        value.put("data_2", copy(config.getData2()));
        value.put("data_3", copy(config.getData3()));
        value.put("data_4", copy(config.getData4()));
        value.put("data_5", copy(config.getData5()));
        return value;
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        return source == null ? null : new LinkedHashMap<>(source);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated() ? "system" : authentication.getName();
    }
}
