package com.example.translator.auth;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String SCHEME = "Basic";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String expectedUser = System.getenv("TRANSLATOR_API_USER");
        String expectedPassword = System.getenv("TRANSLATOR_API_PASSWORD");

        if (expectedUser == null || expectedUser.isBlank() || expectedPassword == null || expectedPassword.isBlank()) {
            abortWithServerConfigurationError(requestContext);
            return;
        }

        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (!isValid(authHeader, expectedUser, expectedPassword)) {
            abortWithUnauthorized(requestContext);
        }
    }

    private boolean isValid(String header, String expectedUser, String expectedPassword) {
        if (header == null) {
            return false;
        }

        int separatorIndex = header.indexOf(' ');
        if (separatorIndex <= 0) {
            return false;
        }
        String scheme = header.substring(0, separatorIndex);
        if (!SCHEME.equalsIgnoreCase(scheme)) {
            return false;
        }

        String token = header.substring(separatorIndex + 1).trim();
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(token);
        } catch (IllegalArgumentException e) {
            return false;
        }

        String credentials = new String(decoded, StandardCharsets.UTF_8);
        String[] parts = credentials.split(":", 2);
        return parts.length == 2 && expectedUser.equals(parts[0]) && expectedPassword.equals(parts[1]);
    }

    private void abortWithServerConfigurationError(ContainerRequestContext context) {
        context.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Authentication is not configured on the server.")
                .type(MediaType.TEXT_PLAIN)
                .build());
    }

    private void abortWithUnauthorized(ContainerRequestContext context) {
        context.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Darija Translator\"")
                .entity("Basic authentication is required.")
                .type(MediaType.TEXT_PLAIN)
                .build());
    }
}
