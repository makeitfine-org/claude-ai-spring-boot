package pl.piomin.services.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.piomin.services.domain.AuditEventType;
import pl.piomin.services.domain.entity.AuditEvent;
import pl.piomin.services.domain.repository.AuditEventRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    // -------------------------------------------------------------------------
    // log(sub, eventType) — no before/after values
    // -------------------------------------------------------------------------

    @Test
    void log_WithoutBeforeAfter_SavesEntityWithCorrectFields() {
        String sub = "user-123";
        AuditEventType eventType = AuditEventType.ACCOUNT_DELETED;
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(sub, eventType);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getSub()).isEqualTo(sub);
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.ACCOUNT_DELETED.name());
        assertThat(saved.getBeforeVal()).isNull();
        assertThat(saved.getAfterVal()).isNull();
        assertThat(saved.getOccurredAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // log(sub, eventType, beforeVal, afterVal) — with before/after values
    // -------------------------------------------------------------------------

    @Test
    void log_WithBeforeAndAfter_SavesEntityWithAllFields() {
        String sub = "user-456";
        AuditEventType eventType = AuditEventType.DISPLAY_NAME_CHANGED;
        String before = "Old Name";
        String after = "New Name";
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(sub, eventType, before, after);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getSub()).isEqualTo(sub);
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.DISPLAY_NAME_CHANGED.name());
        assertThat(saved.getBeforeVal()).isEqualTo("Old Name");
        assertThat(saved.getAfterVal()).isEqualTo("New Name");
        assertThat(saved.getOccurredAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // log — AVATAR_UPLOADED event type stored correctly
    // -------------------------------------------------------------------------

    @Test
    void log_AvatarUploaded_StoresCorrectEventType() {
        String sub = "user-789";
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        auditService.log(sub, AuditEventType.AVATAR_UPLOADED);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        assertThat(captor.getValue().getEventType()).isEqualTo("AVATAR_UPLOADED");
    }

    // -------------------------------------------------------------------------
    // anonymiseForDeletedUser — delegates to repository
    // -------------------------------------------------------------------------

    @Test
    void anonymiseForDeletedUser_CallsAnonymiseBySub() {
        String sub = "user-to-delete";

        auditService.anonymiseForDeletedUser(sub);

        verify(auditEventRepository).anonymiseBySub(sub);
    }
}
