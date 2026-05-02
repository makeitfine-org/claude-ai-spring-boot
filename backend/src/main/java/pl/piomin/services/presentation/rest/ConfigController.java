package pl.piomin.services.presentation.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConfigController {

    private final boolean iamEnabled;

    public ConfigController(@Value("${app.iam.enabled:true}") boolean iamEnabled) {
        this.iamEnabled = iamEnabled;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of("iamEnabled", iamEnabled));
    }
}
