package com.inmocolab.reportes.modelo;

import com.inmocolab.comun.Entidad;
import java.time.LocalDateTime;

public class ReporteGenerado extends Entidad {

    private long idUsuario;
    private String tipo;
    private String formato;
    private LocalDateTime fecha;

    public long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

}
