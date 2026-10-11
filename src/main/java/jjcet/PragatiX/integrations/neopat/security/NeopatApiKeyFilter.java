package jjcet.PragatiX.integrations.neopat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;

@Component
public class NeopatApiKeyFilter extends OncePerRequestFilter {

    private static final String NEOPAT_ASSESSMENT_URI = "/api/v1/integrations/neopat/assessment";
    private static final String API_KEY_HEADER = "x-apikey";

    private final String configuredApiKey;
    private final ObjectMapper objectMapper;

    public NeopatApiKeyFilter(
            @Value("${neopat.api-key:}") String configuredApiKey,
            ObjectMapper objectMapper) {
        this.configuredApiKey = configuredApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equalsIgnoreCase(NEOPAT_ASSESSMENT_URI);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String incomingKey = request.getHeader(API_KEY_HEADER);

        if (!StringUtils.hasText(incomingKey) || !StringUtils.hasText(configuredApiKey) || !constantTimeEquals(incomingKey, configuredApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Boolean> errorBody = Map.of("success", false);
            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}
