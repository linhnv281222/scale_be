package org.facenet.repository.report;

import org.facenet.entity.report.ReportColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportColumnRepository extends JpaRepository<ReportColumn, Long> {

    /**
     * Find columns by template id, ordered by column_order
     */
    List<ReportColumn> findByTemplateIdOrderByColumnOrderAsc(Long templateId);

    /**
     * Find visible columns by template
     */
    List<ReportColumn> findByTemplateIdAndIsVisibleTrueOrderByColumnOrderAsc(Long templateId);
}
