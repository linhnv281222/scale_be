package org.facenet.repository.shift;

import org.facenet.entity.shift.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    boolean existsByCode(String code);
}
