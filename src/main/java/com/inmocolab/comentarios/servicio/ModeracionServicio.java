package com.inmocolab.comentarios.servicio;

import com.inmocolab.comentarios.modelo.Comentario;
import com.inmocolab.comentarios.modelo.EstadoComentario;
import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Util;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.util.ArrayList;
import java.util.List;

/** Moderación de comentarios reportados por el administrador (CU-06.6). */
public class ModeracionServicio {

    public List<Comentario> reportados() {
        List<Comentario> lista = new ArrayList<>();
        for (Comentario cm : Contexto.get().comentarios.todos()) {
            if (!cm.getReportes().isEmpty() && !cm.isModerado()) {
                lista.add(cm);
            }
        }
        return lista;
    }

    public void aprobar(Usuario admin, long idComentario) {
        Comentario cm = pendiente(admin, idComentario);
        cm.getReportes().clear();
        cm.setEstado(EstadoComentario.VISIBLE);
        cm.setMotivoOcultar(null);
        cm.setModerado(true);
        Contexto c = Contexto.get();
        c.usuarios.registrarBitacora(admin.getId(), "MODERAR_APROBAR", "Comentario #" + cm.getId());
        c.usuarios.notificar(cm.getIdAutor(), "Moderación revisó tu comentario y lo mantuvo visible.", null);
    }

    public void ocultar(Usuario admin, long idComentario, String motivo) {
        Comentario cm = pendiente(admin, idComentario);
        if (Util.vacio(motivo)) {
            throw new NegocioException("Debes indicar el motivo.");
        }
        cm.setEstado(EstadoComentario.OCULTO);
        cm.setMotivoOcultar(motivo.trim());
        cm.setModerado(true);
        Contexto c = Contexto.get();
        c.usuarios.registrarBitacora(admin.getId(), "MODERAR_OCULTAR", "Comentario #" + cm.getId() + ". " + motivo.trim());
        c.usuarios.notificar(cm.getIdAutor(), "Moderación ocultó tu comentario. Motivo: " + motivo.trim(), null);
    }

    private Comentario pendiente(Usuario admin, long id) {
        if (admin.getRol() != Rol.ADMINISTRADOR) {
            throw new NegocioException("Solo el administrador modera comentarios.");
        }
        Comentario cm = Contexto.get().comentarios.requerir(id);
        if (cm.isModerado()) {
            throw new NegocioException("Este comentario ya fue moderado.");
        }
        return cm;
    }
}
