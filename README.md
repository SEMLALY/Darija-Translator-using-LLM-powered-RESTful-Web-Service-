# Darija Translator using Gemini Flash Lite

Secure Jakarta REST service that translates English text into Moroccan Darija using Google Gemini Flash Lite (`gemini-flash-lite-latest`). The project exposes a single `/api/translate` endpoint, protects it with Basic Authentication, and relies on environment-driven configuration for secrets.

## Project structure

- `pom.xml` – Maven configuration, dependencies, and Jetty run plugin for local testing.
- `src/main/java/com/example/translator/TranslatorApplication.java` – Rest application activation at `/api`.
- `src/main/java/com/example/translator/TranslatorResource.java` – `/translate` endpoint, Gemini integration, JSON parsing, and error handling.
- `src/main/java/com/example/translator/auth/AuthenticationFilter.java` – Basic Authentication enforcement using environment variables.

## Setup

1. Install Java 17+ and Maven.
2. Populate required environment variables before running or packaging:
   - `GEMINI_API_KEY` – Google Gemini API key for `gemini-flash-lite-latest`.
   - `TRANSLATOR_API_USER` – Basic Auth username.
   - `TRANSLATOR_API_PASSWORD` – Basic Auth password.

   Example for PowerShell:
   ```powershell
   $env:GEMINI_API_KEY='your_key'
   $env:TRANSLATOR_API_USER='translator'
   $env:TRANSLATOR_API_PASSWORD='secret'
   ```

3. Build and run locally via Jetty:
   ```bash
   mvn clean package
   mvn jetty:run
   ```
   Jetty hosts the application at `http://localhost:8080` with the resource exposed at `/api/translate`.

4. For production you can deploy `target/darija-translator.war` to any Jakarta EE / Jakarta REST-compatible application server.

## Translation flow

- The resource expects `{"text":"English sentence"}` (JSON) via POST.
- It validates input, calls Gemini (`generateText`) with the strong prompt:
  `"Translate the following English text to Moroccan Darija. Return only the translation with no explanation."`
- The JSON response is parsed to extract the translated text; only the translation is returned as `text/plain`.
- The endpoint responds with appropriate HTTP codes for invalid input, authentication failure, missing API key, or Gemini errors.

## Testing the endpoint

Curl example:
```bash
curl -u translator:secret \
  -X POST http://localhost:8080/api/translate \
  -H "Content-Type: application/json" \
  -d '{"text":"How do I get to the kasbah?"}'
```

Expected response (plain text):
```
كيفاش نوصل للقصبة؟
```

Postman/Post scripts should mirror the Basic Auth header and JSON payload.

## Notes

- Make sure `GEMINI_API_KEY` remains secret; it is only read from the environment at runtime.
- Keep the Basic Auth credentials safe; the filter refuses requests when either user or password is missing.
- The service uses Java’s modern `HttpClient`, Jackson for JSON handling, and Jakarta REST annotations for structure and validation.
