package com.inmocolab.negociacion.modelo;

import com.inmocolab.comun.Entidad;
import java.time.LocalDateTime;

public class Interes extends Entidad {

    private long idInmueble;
    private long idComprador;
    private LocalDateTime fecha;

    public long getIdInmueble() {
        return idInmueble;
    }

    public void setIdInmueble(long idInmueble) {
        this.idInmueble = idInmueble;
    }

    public long getIdComprador() {
        return idComprador;
    }

    public void setIdComprador(long idComprador) {
        this.idComprador = idComprador;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

}
