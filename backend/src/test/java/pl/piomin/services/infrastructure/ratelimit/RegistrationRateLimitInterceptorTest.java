package pl.piomin.services.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RegistrationRateLimitInterceptorTest {

    private RegistrationRateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RegistrationRateLimitInterceptor();
    }

    // -------------------------------------------------------------------------
    // First 5 requests from same IP — all pass
    // -------------------------------------------------------------------------

    @Test
    void preHandle_First5RequestsFromSameIp_AllPass() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            boolean result = interceptor.preHandle(request, response, new Object());
            assertThat(result).as("Request %d should pass", i + 1).isTrue();
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    // -------------------------------------------------------------------------
    // 6th request from same IP — returns 429 with Retry-After header
    // -------------------------------------------------------------------------

    @Test
    void preHandle_SixthRequestFromSameIp_Returns429WithRetryAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");

        // Exhaust the 5-request allowance
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("900");
        assertThat(response.getContentAsString())
                .contains("Too many registration attempts");
    }

    // -------------------------------------------------------------------------
    // Different IPs are tracked independently
    // -------------------------------------------------------------------------

    @Test
    void preHandle_DifferentIps_TrackedIndependently() throws Exception {
        // Exhaust limit for IP A
        MockHttpServletRequest requestA = new MockHttpServletRequest();
        requestA.setRemoteAddr("192.168.1.1");
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(requestA, new MockHttpServletResponse(), new Object());
        }

        // IP B should still be allowed
        MockHttpServletRequest requestB = new MockHttpServletRequest();
        requestB.setRemoteAddr("192.168.1.2");
        MockHttpServletResponse responseB = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(requestB, responseB, new Object());

        assertThat(result).isTrue();
        assertThat(responseB.getStatus()).isEqualTo(200);
    }

    // -------------------------------------------------------------------------
    // X-Forwarded-For header takes precedence over RemoteAddr
    // -------------------------------------------------------------------------

    @Test
    void preHandle_XForwardedForHeader_UsedAsClientIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5");

        // Exhaust limit for the forwarded IP
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
    }

    // -------------------------------------------------------------------------
    // X-Forwarded-For with comma-separated list — first IP is used
    // -------------------------------------------------------------------------

    @Test
    void preHandle_XForwardedForWithMultipleIps_UsesFirstIp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1, 10.0.0.2");

        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
    }
}
