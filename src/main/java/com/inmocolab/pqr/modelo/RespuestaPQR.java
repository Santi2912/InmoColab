package com.inmocolab.pqr.modelo;

import com.inmocolab.comun.Entidad;
import java.time.LocalDateTime;

public class RespuestaPQR extends Entidad {

    private long idAutor;
    private String texto;
    private LocalDateTime fecha;

    public long getIdAutor() {
        return idAutor;
    }

    public void setIdAutor(long idAutor) {
        this.idAutor = idAutor;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

}
