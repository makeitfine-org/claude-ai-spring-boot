package pl.piomin.services.presentation.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.piomin.services.application.service.AvatarService;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/avatar")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadAvatar(Authentication authentication,
                                             @RequestParam("file") MultipartFile file) {
        avatarService.uploadAvatar(extractSub(authentication), file);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<byte[]> getAvatar(Authentication authentication) {
        AvatarService.AvatarData avatarData = avatarService.getAvatarData(extractSub(authentication));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatarData.contentType()))
                .body(avatarData.bytes());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar(Authentication authentication) {
        avatarService.deleteAvatar(extractSub(authentication));
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
