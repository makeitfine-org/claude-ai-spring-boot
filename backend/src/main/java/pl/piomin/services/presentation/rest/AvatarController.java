package pl.piomin.services.presentation.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<Void> uploadAvatar(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam("file") MultipartFile file) {
        UUID sub = UUID.fromString(jwt.getSubject());
        avatarService.uploadAvatar(sub, file);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<byte[]> getAvatar(@AuthenticationPrincipal Jwt jwt) {
        UUID sub = UUID.fromString(jwt.getSubject());
        AvatarService.AvatarData avatarData = avatarService.getAvatarData(sub);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatarData.contentType()))
                .body(avatarData.bytes());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar(@AuthenticationPrincipal Jwt jwt) {
        UUID sub = UUID.fromString(jwt.getSubject());
        avatarService.deleteAvatar(sub);
    }
}
