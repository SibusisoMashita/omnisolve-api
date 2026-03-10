package com.omnisolve.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorsConfigTest {

    @Test
    void shouldAllowProductionFrontendOriginForDocumentApi() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(
                corsConfig,
                "allowedOrigins",
                "https://omnisolve.africa,https://d3s7bt9q3x42ay.cloudfront.net"
        );

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/documents/stats");
        request.addHeader("Origin", "https://omnisolve.africa");
        request.addHeader("Access-Control-Request-Method", "GET");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals("https://omnisolve.africa", configuration.checkOrigin("https://omnisolve.africa"));
    }
}
