package pl.piomin.services.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.piomin.services.domain.entity.AuditEvent;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    @Modifying
    @Query(value = "UPDATE audit_events SET sub = 'DELETED:' || md5(sub) WHERE sub = :sub", nativeQuery = true)
    void anonymiseBySub(@Param("sub") String sub);
}
