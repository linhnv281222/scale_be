package org.facenet.repository.scale;

import org.facenet.entity.scale.ScaleCurrentState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ScaleCurrentState entity
 */
@Repository
public interface ScaleCurrentStateRepository extends JpaRepository<ScaleCurrentState, Long> {

    List<ScaleCurrentState> findByStatus(String status);
}
