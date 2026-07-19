package com.pruebas.ia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pruebas.ia.model.HistoriaUsuario;
import com.pruebas.ia.model.PlanPruebas;
import org.springframework.stereotype.Service;

@Service
public class GeneradorPruebasService {

    private final HistoriaUsuarioService apiService;
    private final ObjectMapper objectMapper;

    public GeneradorPruebasService(HistoriaUsuarioService apiService) {
        this.apiService = apiService;
        this.objectMapper = new ObjectMapper();
    }

    public PlanPruebas generarPlan(HistoriaUsuario historia) {
        String rawResponse = apiService.llamarGeminiAPI(historia);
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            // Parsea la estructura típica de respuesta de Gemini
            JsonNode textNode = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");
            
            String jsonText = textNode.isMissingNode() ? rawResponse : textNode.asText();
            
            // Deserializa el JSON del plan de pruebas
            return objectMapper.readValue(jsonText, PlanPruebas.class);
        } catch (Exception e) {
            PlanPruebas planFallback = new PlanPruebas();
            planFallback.setResumen("Error al parsear el plan generado por la IA: " + e.getMessage());
            return planFallback;
        }
    }
}
