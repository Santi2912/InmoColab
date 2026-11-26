package com.inmocolab.negociacion.modelo;

import java.time.LocalDateTime;

public class CambioEstado {

    private EstadoNegociacion estado;
    private LocalDateTime fecha;
    private String detalle;

    public CambioEstado() {
    }

    public CambioEstado(EstadoNegociacion estado, LocalDateTime fecha, String detalle) {
        this.estado = estado;
        this.fecha = fecha;
        this.detalle = detalle;
    }

    public EstadoNegociacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoNegociacion estado) {
        this.estado = estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

}
