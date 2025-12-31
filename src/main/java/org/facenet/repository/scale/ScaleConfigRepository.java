package org.facenet.repository.scale;

import org.facenet.entity.scale.ScaleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Repository for ScaleConfig entity
 */
@Repository
public interface ScaleConfigRepository extends JpaRepository<ScaleConfig, Long> {

    /**
     * Update scale configuration using native query to ensure JSONB fields are properly updated
     */
    @Modifying
    @Query(value = """
        UPDATE scale_configs SET
            protocol = :protocol,
            poll_interval = :pollInterval,
            conn_params = CAST(:connParams AS jsonb),
            data_1 = CAST(:data1 AS jsonb),
            data_2 = CAST(:data2 AS jsonb),
            data_3 = CAST(:data3 AS jsonb),
            data_4 = CAST(:data4 AS jsonb),
            data_5 = CAST(:data5 AS jsonb),
            updated_at = CURRENT_TIMESTAMP,
            updated_by = :updatedBy
        WHERE scale_id = :scaleId
        """, nativeQuery = true)
    int updateScaleConfig(
        @Param("scaleId") Long scaleId,
        @Param("protocol") String protocol,
        @Param("pollInterval") Integer pollInterval,
        @Param("connParams") String connParams,
        @Param("data1") String data1,
        @Param("data2") String data2,
        @Param("data3") String data3,
        @Param("data4") String data4,
        @Param("data5") String data5,
        @Param("updatedBy") String updatedBy
    );
}
