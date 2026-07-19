package com.pruebas.ia.service;

import com.pruebas.ia.config.GeminiConfig;
import com.pruebas.ia.model.HistoriaUsuario;
import com.pruebas.ia.prompt.PromptTemplates;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class HistoriaUsuarioService {

    private final GeminiConfig geminiConfig;
    private final HttpClient httpClient;

    public HistoriaUsuarioService(GeminiConfig geminiConfig) {
        this.geminiConfig = geminiConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String llamarGeminiAPI(HistoriaUsuario historia) {
        String prompt = String.format(PromptTemplates.SYSTEM_PROMPT, 
            "Como " + historia.getComo() + ", quiero " + historia.getQuiero() + ", para " + historia.getPara(),
            String.join(", ", historia.getCriteriosAceptacion())
        );

        String apiKey = geminiConfig.getApiKey();
        String urlString = geminiConfig.getApiUrl();
        if (apiKey != null && !apiKey.isBlank()) {
            urlString += "?key=" + apiKey;
        }

        // Build Gemini Request Payload
        String jsonPayload = """
            {
              "contents": [{
                "parts": [{
                  "text": "%s"
                }]
              }],
              "generationConfig": {
                "responseMimeType": "application/json",
                "temperature": 0.2
              }
            }
            """.formatted(escapeJson(prompt));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                return "{\"error\": \"Error en API Gemini: status " + response.statusCode() + "\"}";
            }
        } catch (Exception e) {
            return "{\"error\": \"Error de conexión: " + e.getMessage() + "\"}";
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
