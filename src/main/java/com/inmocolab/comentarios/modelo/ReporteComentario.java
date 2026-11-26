package com.inmocolab.comentarios.modelo;

import java.time.LocalDateTime;

public class ReporteComentario {

    private long idUsuario;
    private String motivo;
    private LocalDateTime fecha;

    public ReporteComentario() {
    }

    public ReporteComentario(long idUsuario, String motivo, LocalDateTime fecha) {
        this.idUsuario = idUsuario;
        this.motivo = motivo;
        this.fecha = fecha;
    }

    public long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

}
