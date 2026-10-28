package com.stockpilot.wms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session tokens for integrations. An app logs in once with its app name/ID and a user's
 * credentials, gets a token, and sends it on every request: Authorization: Bearer <token>.
 *
 * Sessions live in memory, so restarting the server logs every integration out.
 */
@Service
public class ApiSessionService {

    private static final Logger log = LoggerFactory.getLogger(ApiSessionService.class);

    public record ApiSession(String token, String username, List<String> authorities,
                             String appName, String appId, Instant lastUsed) {
        ApiSession touch() {
            return new ApiSession(token, username, authorities, appName, appId, Instant.now());
        }
    }

    private final Map<String, ApiSession> sessions = new ConcurrentHashMap<>();
    private final Duration timeout;

    public ApiSessionService(@Value("${wms.api.session-timeout-minutes:60}") long timeoutMinutes) {
        this.timeout = Duration.ofMinutes(timeoutMinutes);
    }

    public ApiSession open(String username, Collection<? extends GrantedAuthority> authorities,
                           String appName, String appId) {
        String token = UUID.randomUUID().toString();
        List<String> roles = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        ApiSession session = new ApiSession(token, username, roles, appName, appId, Instant.now());
        sessions.put(token, session);
        log.info("API login: user={} app={} ({})", username, appName, appId);
        return session;
    }

    /** Returns the session if the token is valid, and refreshes its idle timer. */
    public Optional<ApiSession> validate(String token) {
        ApiSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.lastUsed().plus(timeout).isBefore(Instant.now())) {
            sessions.remove(token);
            log.info("API session expired for app {}", session.appName());
            return Optional.empty();
        }
        ApiSession touched = session.touch();
        sessions.put(token, touched);
        return Optional.of(touched);
    }

    public void close(String token) {
        ApiSession removed = sessions.remove(token);
        if (removed != null) {
            log.info("API logout: user={} app={}", removed.username(), removed.appName());
        }
    }

    public long timeoutMinutes() {
        return timeout.toMinutes();
    }
}
