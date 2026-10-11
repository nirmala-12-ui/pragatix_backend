package jjcet.PragatiX.integrations.neopat.repository;

import jjcet.PragatiX.integrations.neopat.entity.Assessment2029;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Assessment2029Repository extends JpaRepository<Assessment2029, Long> {

    List<Assessment2029> findByProcessingStatus(String processingStatus);

    List<Assessment2029> findByProcessingStatus(String processingStatus, Pageable pageable);

    long countByProcessingStatus(String processingStatus);

    Optional<Assessment2029> findByEmailAndTestId(String email, String testId);
}
