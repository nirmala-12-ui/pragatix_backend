package jjcet.PragatiX.integrations.neopat.repository;

import jjcet.PragatiX.integrations.neopat.entity.FailedAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedAssessmentRepository extends JpaRepository<FailedAssessment, Long> {

    List<FailedAssessment> findByEmail(String email);

    List<FailedAssessment> findByFailureType(String failureType);

    List<FailedAssessment> findByStatus(String status);
}
