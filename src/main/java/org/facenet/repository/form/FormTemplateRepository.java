package org.facenet.repository.form;

import org.facenet.entity.form.FormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for FormTemplate entity
 */
@Repository
public interface FormTemplateRepository extends JpaRepository<FormTemplate, Integer> {

    Optional<FormTemplate> findByIsDefaultTrue();
}
