package jjcet.PragatiX.integrations.neopat.repository;

import jjcet.PragatiX.integrations.neopat.entity.AssessmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentHistoryRepository extends JpaRepository<AssessmentHistory, Long> {

    List<AssessmentHistory> findByEmail(String email);

    List<AssessmentHistory> findByTestId(String testId);

    List<AssessmentHistory> findBySmsStatus(String smsStatus);
}
