package pl.piomin.services.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pl.piomin.services.infrastructure.ratelimit.RegistrationRateLimitInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RegistrationRateLimitInterceptor registrationRateLimitInterceptor;

    public WebMvcConfig(RegistrationRateLimitInterceptor registrationRateLimitInterceptor) {
        this.registrationRateLimitInterceptor = registrationRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(registrationRateLimitInterceptor)
                .addPathPatterns("/api/register");
    }
}
