package pl.piomin.services.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.application.dto.ProfileResponse;
import pl.piomin.services.application.dto.UpdateProfileRequest;
import pl.piomin.services.domain.entity.User;
import pl.piomin.services.domain.exception.UserNotFoundException;
import pl.piomin.services.domain.port.IdentityProvider;
import pl.piomin.services.domain.repository.UserRepository;

import java.util.UUID;

@Service
@Transactional
public class ProfileService {

    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;

    public ProfileService(UserRepository userRepository, IdentityProvider identityProvider) {
        this.userRepository = userRepository;
        this.identityProvider = identityProvider;
    }

    public ProfileResponse getProfile(UUID sub) {
        User user = userRepository.findById(sub)
                .orElseThrow(() -> new UserNotFoundException(sub));
        return toProfileResponse(user);
    }

    public ProfileResponse updateProfile(UUID sub, UpdateProfileRequest request) {
        User user = userRepository.findById(sub)
                .orElseThrow(() -> new UserNotFoundException(sub));
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        userRepository.save(user);
        return toProfileResponse(user);
    }

    public void deleteProfile(UUID sub) {
        User user = userRepository.findById(sub)
                .orElseThrow(() -> new UserNotFoundException(sub));
        userRepository.delete(user);
        identityProvider.deleteUser(sub.toString());
    }

    private ProfileResponse toProfileResponse(User user) {
        return new ProfileResponse(
                user.getSub(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatarBytes() != null && user.getAvatarBytes().length > 0,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
