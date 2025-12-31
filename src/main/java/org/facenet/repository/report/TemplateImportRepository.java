package org.facenet.repository.report;

import org.facenet.entity.report.TemplateImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateImportRepository extends JpaRepository<TemplateImport, Long> {

    /**
     * Find by template ID
     */
    Optional<TemplateImport> findByTemplateId(Long templateId);

    /**
     * Find all imports by template code
     */
    List<TemplateImport> findByTemplateCode(String templateCode);

    /**
     * Find by resource path
     */
    Optional<TemplateImport> findByResourcePath(String resourcePath);

    /**
     * Find by file hash (for duplicate detection)
     */
    Optional<TemplateImport> findByFileHash(String fileHash);

    /**
     * Find all active imports
     */
    List<TemplateImport> findByIsActiveTrueOrderByImportDateDesc();

    /**
     * Find by import status
     */
    List<TemplateImport> findByImportStatusOrderByImportDateDesc(TemplateImport.ImportStatus status);

    /**
     * Check if file already imported by hash
     */
    boolean existsByFileHash(String fileHash);

    /**
     * Check if resource path already exists
     */
    boolean existsByResourcePath(String resourcePath);

    /**
     * Delete by template ID
     */
    @Query("DELETE FROM TemplateImport ti WHERE ti.template.id = :templateId")
    void deleteByTemplateId(@Param("templateId") Long templateId);
}
