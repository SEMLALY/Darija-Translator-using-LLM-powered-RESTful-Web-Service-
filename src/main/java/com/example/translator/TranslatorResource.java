package com.example.translator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Path("/translate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.TEXT_PLAIN)
public class TranslatorResource {

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent";
    private static final String TRANSLATION_PROMPT =
            "Translate the following English text to Moroccan Darija. Return only the translation with no explanation.\n\n";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @POST
    public Response translate(TranslationRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("The request must contain non-empty English text.")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        try {
            String translation = invokeGemini(request.getText().trim());
            return Response.ok(translation)
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        } catch (GeminiClientException e) {
            return Response.status(e.getStatus())
                    .entity(e.getMessage())
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Translation request was interrupted.")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }
    }

    private String invokeGemini(String englishText) throws GeminiClientException, InterruptedException {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiClientException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Gemini API key is not configured in GEMINI_API_KEY.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(createGeminiUri(apiKey))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(createPayload(englishText), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new GeminiClientException(Response.Status.BAD_GATEWAY, "Gemini API request timed out.");
        } catch (IOException e) {
            throw new GeminiClientException(Response.Status.BAD_GATEWAY,
                    "Unable to reach Gemini API: " + e.getMessage());
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new GeminiClientException(Response.Status.BAD_GATEWAY,
                    "Gemini API returned HTTP " + response.statusCode() + ".");
        }

        String translation = extractTranslation(response.body());
        if (translation == null || translation.isBlank()) {
            throw new GeminiClientException(Response.Status.BAD_GATEWAY,
                    "Gemini returned a malformed or unexpected response.");
        }

        return translation.trim();
    }

    private URI createGeminiUri(String apiKey) throws GeminiClientException {
        try {
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            return URI.create(GEMINI_ENDPOINT + "?key=" + encodedKey);
        } catch (IllegalArgumentException e) {
            throw new GeminiClientException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Failed to build Gemini endpoint URI.");
        }
    }

    private static String createPayload(String text) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ObjectNode content = root.putArray("contents").addObject();
        ObjectNode part = content.putArray("parts").addObject();
        part.put("text", TRANSLATION_PROMPT + text);
        return root.toString();
    }

    private static String extractTranslation(String body) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode textNode = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");
            if (textNode.isTextual()) {
                return textNode.asText();
            }
        } catch (IOException ignored) {
            // Caller will convert this to a clean 502 response.
        }
        return null;
    }

    private static final class GeminiClientException extends Exception {
        private final Response.Status status;

        private GeminiClientException(Response.Status status, String message) {
            super(message);
            this.status = status;
        }

        private Response.Status getStatus() {
            return status;
        }
    }

    public static final class TranslationRequest {
        private String text;

        public TranslationRequest() {
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
