package pl.piomin.services.infrastructure.identity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pl.piomin.services.domain.port.CreateUserCommand;
import pl.piomin.services.domain.port.IdentityProvider;

import java.util.UUID;

/**
 * No-op identity provider used when IAM is disabled ({@code app.iam.enabled=false}).
 * Registration still persists users to the local DB; a random UUID is generated as the sub.
 */
@Component
@ConditionalOnProperty(prefix = "app.iam", name = "enabled", havingValue = "false")
public class NoOpIdentityProvider implements IdentityProvider {

    @Override
    public String createUser(CreateUserCommand command) {
        return UUID.randomUUID().toString();
    }

    @Override
    public void deleteUser(String sub) {
        // no-op: no external identity provider in IAM-disabled mode
    }
}
