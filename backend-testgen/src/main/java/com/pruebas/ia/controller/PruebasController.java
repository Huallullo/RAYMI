package com.pruebas.ia.controller;

import com.pruebas.ia.model.HistoriaUsuario;
import com.pruebas.ia.model.PlanPruebas;
import com.pruebas.ia.service.GeneradorPruebasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pruebas")
public class PruebasController {

    private final GeneradorPruebasService generadorService;

    public PruebasController(GeneradorPruebasService generadorService) {
        this.generadorService = generadorService;
    }

    @PostMapping("/generar")
    public ResponseEntity<PlanPruebas> generarPlan(@RequestBody HistoriaUsuario historia) {
        if (historia.getCriteriosAceptacion() == null || historia.getCriteriosAceptacion().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        PlanPruebas plan = generadorService.generarPlan(historia);
        return ResponseEntity.ok(plan);
    }
}
