package jjcet.PragatiX.integrations.neopat.repository;

import jjcet.PragatiX.integrations.neopat.entity.ParentContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParentContactRepository extends JpaRepository<ParentContact, Long> {

    Optional<ParentContact> findByStudentEmail(String studentEmail);

    Optional<ParentContact> findByStudentEmailIgnoreCase(String studentEmail);

    Optional<ParentContact> findByStudentEmailAndIsActiveTrue(String studentEmail);

    Optional<ParentContact> findByStudentEmailIgnoreCaseAndIsActiveTrue(String studentEmail);

    boolean existsByStudentEmail(String studentEmail);

    boolean existsByStudentEmailIgnoreCase(String studentEmail);

    boolean existsByStudentEmailIgnoreCaseAndIdNot(String studentEmail, Long id);

    Page<ParentContact> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
