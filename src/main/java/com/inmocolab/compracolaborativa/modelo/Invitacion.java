package com.inmocolab.compracolaborativa.modelo;

import com.inmocolab.comun.Entidad;
import java.time.LocalDateTime;

public class Invitacion extends Entidad {

    private long idGrupo;
    private long idInvitado;
    private LocalDateTime fechaEnvio;
    private EstadoInvitacion estado;

    public long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(long idGrupo) {
        this.idGrupo = idGrupo;
    }

    public long getIdInvitado() {
        return idInvitado;
    }

    public void setIdInvitado(long idInvitado) {
        this.idInvitado = idInvitado;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public EstadoInvitacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoInvitacion estado) {
        this.estado = estado;
    }

}
