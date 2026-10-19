package org.facenet.repository.scale;

import org.facenet.entity.scale.ScaleConfigAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScaleConfigAuditLogRepository extends JpaRepository<ScaleConfigAuditLog, Long> {
    Page<ScaleConfigAuditLog> findByScaleIdOrderByChangedAtDesc(Long scaleId, Pageable pageable);
}
