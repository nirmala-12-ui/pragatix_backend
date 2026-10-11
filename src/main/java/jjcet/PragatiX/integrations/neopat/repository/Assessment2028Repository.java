package jjcet.PragatiX.integrations.neopat.repository;

import jjcet.PragatiX.integrations.neopat.entity.Assessment2028;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Assessment2028Repository extends JpaRepository<Assessment2028, Long> {

    List<Assessment2028> findByProcessingStatus(String processingStatus);

    List<Assessment2028> findByProcessingStatus(String processingStatus, Pageable pageable);

    long countByProcessingStatus(String processingStatus);

    Optional<Assessment2028> findByEmailAndTestId(String email, String testId);
}
