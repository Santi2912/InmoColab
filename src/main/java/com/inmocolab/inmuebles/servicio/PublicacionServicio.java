package com.inmocolab.inmuebles.servicio;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Util;
import com.inmocolab.inmuebles.modelo.EstadoPublicacion;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.inmuebles.modelo.Publicacion;
import com.inmocolab.inmuebles.persistencia.PublicacionRepositorio;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.time.LocalDateTime;
import java.util.List;

/** Validación de publicaciones por el administrador (CU-08.1). */
public class PublicacionServicio {

    private final PublicacionRepositorio publicaciones;

    public PublicacionServicio(PublicacionRepositorio publicaciones) {
        this.publicaciones = publicaciones;
    }

    public List<Publicacion> pendientes() {
        return publicaciones.porEstado(EstadoPublicacion.PENDIENTE);
    }

    public void aprobar(Usuario admin, long idPublicacion) {
        Publicacion p = pendiente(admin, idPublicacion);
        p.setEstado(EstadoPublicacion.APROBADA);
        p.setIdAdministrador(admin.getId());
        p.setFechaPublicacion(LocalDateTime.now());
        p.setMotivoRechazo(null);
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.requerir(p.getIdInmueble());
        c.usuarios.registrarBitacora(admin.getId(), "APROBAR_PUBLICACION", "Inmueble #" + i.getId());
        c.usuarios.notificar(i.getIdPropietario(), "Tu publicación en " + i.getDireccion() + " fue aprobada y ya es visible.",
                "/api/inmuebles/detalle?id=" + i.getId());
    }

    public void rechazar(Usuario admin, long idPublicacion, String motivo) {
        Publicacion p = pendiente(admin, idPublicacion);
        if (Util.vacio(motivo)) {
            throw new NegocioException("Debes indicar el motivo del rechazo.");
        }
        p.setEstado(EstadoPublicacion.RECHAZADA);
        p.setIdAdministrador(admin.getId());
        p.setMotivoRechazo(motivo.trim());
        Contexto c = Contexto.get();
        Inmueble i = c.inmuebles.requerir(p.getIdInmueble());
        c.usuarios.registrarBitacora(admin.getId(), "RECHAZAR_PUBLICACION", "Inmueble #" + i.getId() + ". Motivo: " + motivo.trim());
        c.usuarios.notificar(i.getIdPropietario(), "Tu publicación en " + i.getDireccion() + " fue rechazada. Motivo: " + motivo.trim(),
                "/api/inmuebles/mis");
    }

    private Publicacion pendiente(Usuario admin, long id) {
        if (admin.getRol() != Rol.ADMINISTRADOR) {
            throw new NegocioException("Solo el administrador valida publicaciones.");
        }
        Publicacion p = publicaciones.buscar(id);
        if (p == null || p.getEstado() != EstadoPublicacion.PENDIENTE) {
            throw new NegocioException("La publicación no está pendiente de validación.");
        }
        return p;
    }

    public Publicacion buscar(long id) {
        return publicaciones.buscar(id);
    }
}
