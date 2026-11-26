package com.inmocolab.negociacion.modelo;

import com.inmocolab.comun.Entidad;
import java.time.LocalDateTime;

public class Observacion extends Entidad {

    private long idAsesor;
    private String texto;
    private LocalDateTime fecha;

    public long getIdAsesor() {
        return idAsesor;
    }

    public void setIdAsesor(long idAsesor) {
        this.idAsesor = idAsesor;
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
