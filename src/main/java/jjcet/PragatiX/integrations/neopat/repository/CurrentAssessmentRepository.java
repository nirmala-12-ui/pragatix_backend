package jjcet.PragatiX.integrations.neopat.repository;

import jjcet.PragatiX.integrations.neopat.entity.CurrentAssessment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrentAssessmentRepository extends JpaRepository<CurrentAssessment, Long> {

    List<CurrentAssessment> findByProcessingStatus(String processingStatus);

    List<CurrentAssessment> findByProcessingStatus(String processingStatus, Pageable pageable);

    boolean existsByEmailAndTestId(String email, String testId);
}
