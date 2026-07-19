package com.pruebas.ia.model;

import java.util.List;

public class HistoriaUsuario {
    private String titulo;
    private String como;
    private String quiero;
    private String para;
    private List<String> criteriosAceptacion;

    // Getters and Setters
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getComo() { return como; }
    public void setComo(String como) { this.como = como; }

    public String getQuiero() { return quiero; }
    public void setQuiero(String quiero) { this.quiero = quiero; }

    public String getPara() { return para; }
    public void setPara(String para) { this.para = para; }

    public List<String> getCriteriosAceptacion() { return criteriosAceptacion; }
    public void setCriteriosAceptacion(List<String> criteriosAceptacion) { this.criteriosAceptacion = criteriosAceptacion; }
}
