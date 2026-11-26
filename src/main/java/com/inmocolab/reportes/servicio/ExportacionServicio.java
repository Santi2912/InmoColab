package com.inmocolab.reportes.servicio;

import com.inmocolab.comun.Contexto;
import com.inmocolab.reportes.modelo.Reporte;
import com.inmocolab.reportes.modelo.ReporteGenerado;
import com.inmocolab.usuarios.modelo.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Exportación de reportes (HU-43): CSV compatible con Excel; el PDF se obtiene imprimiendo la vista. */
public class ExportacionServicio {

    public String aCsv(Usuario u, Reporte r, String filtros) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(celda(r.getTitulo())).append("\r\n");
        sb.append("Generado el;").append(com.inmocolab.comun.Util.fecha(LocalDateTime.now())).append("\r\n");
        sb.append("Generado por;").append(celda(u.getNombre())).append("\r\n");
        sb.append("Filtros;").append(celda(filtros)).append("\r\n\r\n");
        for (Map.Entry<String, String> e : r.getIndicadores().entrySet()) {
            sb.append(celda(e.getKey())).append(';').append(celda(e.getValue())).append("\r\n");
        }
        sb.append("\r\n");
        sb.append(String.join(";", r.getColumnas().stream().map(ExportacionServicio::celda).toArray(String[]::new))).append("\r\n");
        for (List<String> fila : r.getFilas()) {
            sb.append(String.join(";", fila.stream().map(ExportacionServicio::celda).toArray(String[]::new))).append("\r\n");
        }
        ReporteGenerado g = new ReporteGenerado();
        g.setIdUsuario(u.getId());
        g.setTipo(r.getTipo());
        g.setFormato("CSV");
        g.setFecha(LocalDateTime.now());
        Contexto.get().reporteRepo.guardar(g);
        return sb.toString();
    }

    private static String celda(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\"", "\"\"");
        return t.contains(";") || t.contains("\"") || t.contains("\n") ? "\"" + t + "\"" : t;
    }
}
