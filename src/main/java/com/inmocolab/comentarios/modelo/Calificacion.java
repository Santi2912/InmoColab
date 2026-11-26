package com.inmocolab.comentarios.modelo;

import com.inmocolab.comun.Entidad;
import java.time.LocalDateTime;

public class Calificacion extends Entidad {

    private long idNegociacion;
    private long idComprador;
    private int estrellas;
    private String texto;
    private LocalDateTime fecha;

    public long getIdNegociacion() {
        return idNegociacion;
    }

    public void setIdNegociacion(long idNegociacion) {
        this.idNegociacion = idNegociacion;
    }

    public long getIdComprador() {
        return idComprador;
    }

    public void setIdComprador(long idComprador) {
        this.idComprador = idComprador;
    }

    public int getEstrellas() {
        return estrellas;
    }

    public void setEstrellas(int estrellas) {
        this.estrellas = estrellas;
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
