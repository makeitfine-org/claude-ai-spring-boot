package pl.piomin.services.presentation.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.piomin.services.application.dto.ProfileResponse;
import pl.piomin.services.application.dto.UpdateProfileRequest;
import pl.piomin.services.application.service.ProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getProfile(Authentication authentication) {
        return profileService.getProfile(extractSub(authentication));
    }

    @PatchMapping
    public ProfileResponse updateProfile(Authentication authentication,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(extractSub(authentication), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(Authentication authentication, HttpServletRequest request) {
        profileService.deleteProfile(extractSub(authentication));
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private UUID extractSub(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        if (principal instanceof OidcUser oidcUser) {
            return UUID.fromString(oidcUser.getSubject());
        }
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }
}
