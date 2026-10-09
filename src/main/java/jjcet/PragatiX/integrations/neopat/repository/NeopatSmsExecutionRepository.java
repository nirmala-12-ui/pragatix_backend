package jjcet.PragatiX.integrations.neopat.repository;

import jjcet.PragatiX.integrations.neopat.entity.NeopatSmsExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface NeopatSmsExecutionRepository extends JpaRepository<NeopatSmsExecution, Long> {

    Optional<NeopatSmsExecution> findByScheduledDateAndScheduledTime(LocalDate scheduledDate, LocalTime scheduledTime);

    boolean existsByScheduledDateAndScheduledTime(LocalDate scheduledDate, LocalTime scheduledTime);

    boolean existsByScheduledDateAndStatusIn(LocalDate scheduledDate, java.util.Collection<String> statuses);
}
