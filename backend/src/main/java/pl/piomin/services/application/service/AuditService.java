package pl.piomin.services.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.domain.AuditEventType;
import pl.piomin.services.domain.entity.AuditEvent;
import pl.piomin.services.domain.repository.AuditEventRepository;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void log(String sub, AuditEventType eventType) {
        log(sub, eventType, null, null);
    }

    public void log(String sub, AuditEventType eventType, String beforeVal, String afterVal) {
        AuditEvent event = new AuditEvent();
        event.setSub(sub);
        event.setEventType(eventType.name());
        event.setBeforeVal(beforeVal);
        event.setAfterVal(afterVal);
        auditEventRepository.save(event);
    }

    public void anonymiseForDeletedUser(String sub) {
        auditEventRepository.anonymiseBySub(sub);
    }
}
