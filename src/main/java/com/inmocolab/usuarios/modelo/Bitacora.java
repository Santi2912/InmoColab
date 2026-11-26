package com.inmocolab.usuarios.modelo;

import java.time.LocalDateTime;

/** Registro de auditoría. Es inmutable: no tiene setters ni se puede eliminar. */
public final class Bitacora {

    private final long id;
    private final long idUsuario;
    private final String accion;
    private final String detalle;
    private final LocalDateTime fecha;

    public Bitacora(long id, long idUsuario, String accion, String detalle, LocalDateTime fecha) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.accion = accion;
        this.detalle = detalle;
        this.fecha = fecha;
    }

    public long getId() {
        return id;
    }

    public long getIdUsuario() {
        return idUsuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
