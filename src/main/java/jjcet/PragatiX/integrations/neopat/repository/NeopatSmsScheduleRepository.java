package jjcet.PragatiX.integrations.neopat.repository;

import jjcet.PragatiX.integrations.neopat.entity.NeopatSmsSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NeopatSmsScheduleRepository extends JpaRepository<NeopatSmsSchedule, Long> {

    Optional<NeopatSmsSchedule> findFirstByOrderByIdAsc();
}
