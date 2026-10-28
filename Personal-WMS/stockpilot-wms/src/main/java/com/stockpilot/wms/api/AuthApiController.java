package com.stockpilot.wms.api;

import com.stockpilot.wms.api.ApiDtos.LoginRequest;
import com.stockpilot.wms.api.ApiDtos.LoginResponse;
import com.stockpilot.wms.config.ApiSessionService;
import com.stockpilot.wms.config.ApiSessionService.ApiSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Login")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final ApiSessionService sessionService;

    public AuthApiController(AuthenticationManager authenticationManager, ApiSessionService sessionService) {
        this.authenticationManager = authenticationManager;
        this.sessionService = sessionService;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in as an integration and get a session token")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        ApiSession session = sessionService.open(auth.getName(), auth.getAuthorities(),
                request.appName(), request.appId());
        return new LoginResponse(session.token(), session.username(), sessionService.timeoutMinutes());
    }

    @PostMapping("/logout")
    @Operation(summary = "End the session for the token in the Authorization header")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        if (authorization.startsWith("Bearer ")) {
            sessionService.close(authorization.substring(7).trim());
        }
        return ResponseEntity.noContent().build();
    }
}
