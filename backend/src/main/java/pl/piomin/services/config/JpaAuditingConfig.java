package pl.piomin.services.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Enables JPA auditing with an {@link OffsetDateTime}-aware {@link DateTimeProvider}.
 *
 * <p>The built-in {@code CurrentDateTimeProvider} returns {@code LocalDateTime}, which
 * cannot be assigned to an {@code OffsetDateTime} auditing field. Registering a custom
 * provider that returns {@code OffsetDateTime.now()} fixes both:
 * <ul>
 *   <li>{@code User.createdAt} / {@code User.updatedAt} — typed as {@code OffsetDateTime}</li>
 *   <li>{@code Person.createdAt} / {@code Person.updatedAt} — typed as {@code LocalDateTime}
 *       (Spring Data extracts {@code LocalDateTime} via {@code OffsetDateTime#toLocalDateTime})</li>
 * </ul>
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
