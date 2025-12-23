package org.facenet.repository.scale;

import org.facenet.entity.scale.ScaleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for ScaleConfig entity
 */
@Repository
public interface ScaleConfigRepository extends JpaRepository<ScaleConfig, Long> {
}
