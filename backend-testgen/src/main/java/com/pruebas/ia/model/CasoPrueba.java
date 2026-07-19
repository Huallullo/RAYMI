package com.pruebas.ia.model;

import java.util.List;

public class CasoPrueba {
    private String id;
    private String escenario;
    private String tipo; // POSITIVO, NEGATIVO, BORDE, SEGURIDAD
    private List<String> pasos;
    private String datosEntrada;
    private String resultadoEsperado;
    private String severidad; // ALTA, MEDIA, BAJA

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEscenario() { return escenario; }
    public void setEscenario(String escenario) { this.escenario = escenario; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public List<String> getPasos() { return pasos; }
    public void setPasos(List<String> pasos) { this.pasos = pasos; }

    public String getDatosEntrada() { return datosEntrada; }
    public void setDatosEntrada(String datosEntrada) { this.datosEntrada = datosEntrada; }

    public String getResultadoEsperado() { return resultadoEsperado; }
    public void setResultadoEsperado(String resultadoEsperado) { this.resultadoEsperado = resultadoEsperado; }

    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }
}
