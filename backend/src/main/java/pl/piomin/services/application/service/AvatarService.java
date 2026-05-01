package pl.piomin.services.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.piomin.services.domain.AuditEventType;
import pl.piomin.services.domain.entity.User;
import pl.piomin.services.domain.exception.AvatarNotFoundException;
import pl.piomin.services.domain.exception.AvatarTooLargeException;
import pl.piomin.services.domain.exception.InvalidAvatarException;
import pl.piomin.services.domain.exception.UnsupportedAvatarTypeException;
import pl.piomin.services.domain.exception.UserNotFoundException;
import pl.piomin.services.domain.repository.UserRepository;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@Transactional
public class AvatarService {

    private static final long MAX_SIZE_BYTES = 1_048_576L; // 1 MB
    private static final int MAX_DIMENSION = 512;

    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AvatarService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public record AvatarData(byte[] bytes, String contentType) {}

    public void uploadAvatar(UUID sub, MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null
                || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new UnsupportedAvatarTypeException(contentType);
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new AvatarTooLargeException(file.getSize(), MAX_SIZE_BYTES);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidAvatarException("Failed to read avatar file: " + e.getMessage());
        }

        validateMagicBytes(bytes, contentType);

        byte[] stored = resizeIfNeeded(bytes, contentType);

        User user = userRepository.findById(sub)
                .orElseThrow(() -> new UserNotFoundException(sub));
        user.setAvatarBytes(stored);
        user.setAvatarContentType(contentType);
        userRepository.save(user);
        auditService.log(sub.toString(), AuditEventType.AVATAR_UPLOADED);
    }

    public AvatarData getAvatarData(UUID sub) {
        User user = userRepository.findById(sub)
                .orElseThrow(() -> new UserNotFoundException(sub));

        if (user.getAvatarBytes() == null || user.getAvatarBytes().length == 0) {
            throw new AvatarNotFoundException(sub);
        }

        return new AvatarData(user.getAvatarBytes(), user.getAvatarContentType());
    }

    public void deleteAvatar(UUID sub) {
        User user = userRepository.findById(sub)
                .orElseThrow(() -> new UserNotFoundException(sub));
        user.setAvatarBytes(null);
        user.setAvatarContentType(null);
        userRepository.save(user);
        auditService.log(sub.toString(), AuditEventType.AVATAR_REMOVED);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateMagicBytes(byte[] bytes, String contentType) {
        if (contentType.equals("image/jpeg")) {
            if (!startsWith(bytes, JPEG_MAGIC)) {
                throw new InvalidAvatarException(
                        "File content does not match declared content type image/jpeg");
            }
        } else {
            if (!startsWith(bytes, PNG_MAGIC)) {
                throw new InvalidAvatarException(
                        "File content does not match declared content type image/png");
            }
        }
    }

    private boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private byte[] resizeIfNeeded(byte[] bytes, String contentType) {
        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new InvalidAvatarException("Failed to decode image: " + e.getMessage());
        }

        if (original == null) {
            throw new InvalidAvatarException("Could not decode image data");
        }

        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return bytes;
        }

        double scale = Math.min((double) MAX_DIMENSION / width, (double) MAX_DIMENSION / height);
        int targetWidth = (int) Math.round(width * scale);
        int targetHeight = (int) Math.round(height * scale);

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        String formatName = contentType.equals("image/jpeg") ? "jpeg" : "png";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(resized, formatName, out);
        } catch (IOException e) {
            throw new InvalidAvatarException("Failed to encode resized image: " + e.getMessage());
        }
        return out.toByteArray();
    }
}
