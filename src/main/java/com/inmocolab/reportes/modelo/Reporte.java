package com.inmocolab.reportes.modelo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resultado de un reporte: indicadores, serie para el gráfico y tabla de detalle. No se persiste. */
public class Reporte {

    private final String tipo;
    private final String titulo;
    private final List<String> columnas = new ArrayList<>();
    private final List<List<String>> filas = new ArrayList<>();
    private final Map<String, String> indicadores = new LinkedHashMap<>();
    private final Map<String, Double> serie = new LinkedHashMap<>();
    private final Set<Integer> resaltadas = new LinkedHashSet<>();
    private String tituloSerie = "";

    public Reporte(String tipo, String titulo, String... columnas) {
        this.tipo = tipo;
        this.titulo = titulo;
        for (String c : columnas) {
            this.columnas.add(c);
        }
    }

    public void agregarFila(boolean resaltar, String... celdas) {
        List<String> fila = new ArrayList<>();
        for (String c : celdas) {
            fila.add(c);
        }
        if (resaltar) {
            resaltadas.add(filas.size());
        }
        filas.add(fila);
    }

    public String getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public List<String> getColumnas() {
        return columnas;
    }

    public List<List<String>> getFilas() {
        return filas;
    }

    public Map<String, String> getIndicadores() {
        return indicadores;
    }

    public Map<String, Double> getSerie() {
        return serie;
    }

    public Set<Integer> getResaltadas() {
        return resaltadas;
    }

    public String getTituloSerie() {
        return tituloSerie;
    }

    public void setTituloSerie(String tituloSerie) {
        this.tituloSerie = tituloSerie;
    }
}
