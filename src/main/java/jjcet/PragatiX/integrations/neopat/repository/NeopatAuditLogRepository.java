package jjcet.PragatiX.integrations.neopat.repository;

import jjcet.PragatiX.integrations.neopat.entity.NeopatAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NeopatAuditLogRepository extends JpaRepository<NeopatAuditLog, Long> {

    List<NeopatAuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType);

    List<NeopatAuditLog> findTop50ByOrderByCreatedAtDesc();
}
