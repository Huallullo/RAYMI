package com.pruebas.ia.model;

import java.util.List;

public class PlanPruebas {
    private String resumen;
    private List<CasoPrueba> casos;
    private int totalCasos;
    private double coberturaEstimada;

    // Getters and Setters
    public String getResumen() { return resumen; }
    public void setResumen(String resumen) { this.resumen = resumen; }

    public List<CasoPrueba> getCasos() { return casos; }
    public void setCasos(List<CasoPrueba> casos) { this.casos = casos; }

    public int getTotalCasos() { return totalCasos; }
    public void setTotalCasos(int totalCasos) { this.totalCasos = totalCasos; }

    public double getCoberturaEstimada() { return coberturaEstimada; }
    public void setCoberturaEstimada(double coberturaEstimada) { this.coberturaEstimada = coberturaEstimada; }
}
