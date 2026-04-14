# Darija Translator REST API

University mini project: a Java Maven WAR service that translates English text into Moroccan Darija (Latin letters only) using Google Gemini.

## Overview

This project exposes one secured REST endpoint:

- `POST /api/translate`

The backend is built with Jakarta REST (JAX-RS), Jersey, and Jetty.  
It calls Gemini `generateContent` using:

- `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent`

The project also includes a Chrome extension (Manifest V3 + `chrome.sidePanel`) that can translate selected text directly from a web page.

## Features

- English to Moroccan Darija translation via Gemini
- Darija output requested in Latin letters only
- Basic Authentication for API access
- Clean input validation and upstream error handling
- Plain text translation response (not JSON)
- Chrome right-click flow: context menu `Translate` captures selection, opens side panel, sends text to backend, and shows translated result

## Technologies Used

- Java 17
- Maven (WAR packaging)
- Jakarta RESTful Web Services (`jakarta.ws.rs`)
- Jersey (JAX-RS implementation)
- Jetty Maven Plugin (local run)
- Jackson (JSON parsing)
- Google Gemini API (`generateContent`)
- Chrome Extension APIs (Manifest V3, contextMenus, sidePanel)

## Project Structure

- `pom.xml` - Maven dependencies and build plugins
- `src/main/java/com/example/translator/TranslatorApplication.java` - JAX-RS app config (`/api`)
- `src/main/java/com/example/translator/TranslatorResource.java` - `POST /api/translate` + Gemini integration
- `src/main/java/com/example/translator/auth/AuthenticationFilter.java` - Basic Authentication filter
- `src/main/webapp/WEB-INF/web.xml` - Jersey servlet mapping
- Chrome extension folder (Manifest V3) - context menu + side panel UI/logic

## Architecture Flow

1. User sends text (API client or Chrome extension)
2. Request hits `POST /api/translate`
3. `AuthenticationFilter` validates Basic Auth
4. `TranslatorResource` validates input and calls Gemini `generateContent`
5. Service extracts `candidates[0].content.parts[0].text`
6. Backend returns plain text Darija translation

## Setup

### Prerequisites

- Java 17 installed
- Maven installed (`mvn -v` should work)
- Gemini API key

### Environment Variables (Windows PowerShell)

```powershell
$env:GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
$env:TRANSLATOR_API_USER="student"
$env:TRANSLATOR_API_PASSWORD="student123"
```

Local demo credentials:

- Username: `student`
- Password: `student123`

### Run Backend

```powershell
mvn clean jetty:run
```

Base URL:

- `http://localhost:8080`

API endpoint:

- `http://localhost:8080/api/translate`

## API Usage

### Request

`POST /api/translate`  
Content-Type: `application/json`  
Authorization: Basic Auth

```json
{
  "text": "Hello, how are you?"
}
```

### Response

Content-Type: `text/plain`

Example:

```text
labas 3lik?
```

## Test API with PowerShell

```powershell
$pair = "student:student123"
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/translate" `
  -Headers @{ Authorization = "Basic $basic" } `
  -ContentType "application/json" `
  -Body '{"text":"Hello, how are you?"}'
```
## Class Diagram
<img width="504" height="777" alt="image" src="https://github.com/user-attachments/assets/a01de5f5-63d0-4e80-811d-0fe50fb343ac" />

## Deployment Diagram
<img width="1658" height="812" alt="deployment diagram drawio" src="https://github.com/user-attachments/assets/08625282-2954-4d01-a73c-c1d017a5d671" />


## Chrome Extension: Load and Test

1. Start backend with `mvn clean jetty:run`
2. Open Chrome: `chrome://extensions`
3. Enable **Developer mode**
4. Click **Load unpacked**
5. Select the extension folder (the one containing `manifest.json`)
6. Open any webpage and select English text
7. Right-click selected text and choose **Translate**
8. The side panel opens and displays the Darija translation

## Common Errors

- `503` (high demand): Gemini is temporarily unavailable. Retry after a short delay.
- `429` (quota/rate limit): Too many requests or quota exhausted. Wait and try again.
- Response format confusion: `/api/translate` returns **plain text**, not JSON.
- `401`: Basic Auth credentials are missing/invalid.
- `400`: `text` is missing or blank.
