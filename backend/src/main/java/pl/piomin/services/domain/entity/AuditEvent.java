package pl.piomin.services.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String sub;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "before_val")
    private String beforeVal;

    @Column(name = "after_val")
    private String afterVal;

    @Column(name = "occurred_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ", insertable = true, updatable = false)
    private OffsetDateTime occurredAt;

    public AuditEvent() {
        this.occurredAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getBeforeVal() {
        return beforeVal;
    }

    public void setBeforeVal(String beforeVal) {
        this.beforeVal = beforeVal;
    }

    public String getAfterVal() {
        return afterVal;
    }

    public void setAfterVal(String afterVal) {
        this.afterVal = afterVal;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
