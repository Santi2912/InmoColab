package com.inmocolab.comun;

import com.inmocolab.comun.bd.Persistencia;
import com.inmocolab.comentarios.persistencia.CalificacionRepositorio;
import com.inmocolab.comentarios.persistencia.ComentarioRepositorio;
import com.inmocolab.comentarios.servicio.ComentarioServicio;
import com.inmocolab.comentarios.servicio.ModeracionServicio;
import com.inmocolab.compracolaborativa.persistencia.AporteRepositorio;
import com.inmocolab.compracolaborativa.persistencia.GrupoRepositorio;
import com.inmocolab.compracolaborativa.servicio.AporteServicio;
import com.inmocolab.compracolaborativa.servicio.GrupoServicio;
import com.inmocolab.inmuebles.persistencia.InmuebleRepositorio;
import com.inmocolab.inmuebles.persistencia.PublicacionRepositorio;
import com.inmocolab.inmuebles.servicio.InmuebleServicio;
import com.inmocolab.inmuebles.servicio.PublicacionServicio;
import com.inmocolab.negociacion.persistencia.InteresRepositorio;
import com.inmocolab.negociacion.persistencia.NegociacionRepositorio;
import com.inmocolab.negociacion.persistencia.OfertaRepositorio;
import com.inmocolab.negociacion.servicio.NegociacionServicio;
import com.inmocolab.negociacion.servicio.OfertaServicio;
import com.inmocolab.pqr.persistencia.PqrRepositorio;
import com.inmocolab.pqr.servicio.PqrServicio;
import com.inmocolab.reportes.persistencia.ReporteRepositorio;
import com.inmocolab.reportes.servicio.ExportacionServicio;
import com.inmocolab.reportes.servicio.ReporteServicio;
import com.inmocolab.usuarios.persistencia.BitacoraRepositorio;
import com.inmocolab.usuarios.persistencia.NotificacionRepositorio;
import com.inmocolab.usuarios.persistencia.ParametroRepositorio;
import com.inmocolab.usuarios.persistencia.UsuarioRepositorio;
import com.inmocolab.usuarios.servicio.AutenticacionServicio;
import com.inmocolab.usuarios.servicio.UsuarioServicio;

/**
 * Raíz de composición: crea los repositorios y servicios una sola vez y los conecta a mano
 * (inyección de dependencias sin framework).
 *
 * Patrones: Singleton (una sola instancia), Composition Root e inyección de dependencias manual.
 */
public final class Contexto {

    private static final Contexto INSTANCIA;

    static {
        INSTANCIA = new Contexto();
        // Con PostgreSQL y datos guardados se cargan de la base; si no, se siembran los de ejemplo.
        if (!Persistencia.iniciar(INSTANCIA)) {
            Datos.cargar(INSTANCIA);
            Persistencia.sincronizar();
        }
    }

    // persistencia
    public final UsuarioRepositorio usuarioRepo = new UsuarioRepositorio();
    public final NotificacionRepositorio notificacionRepo = new NotificacionRepositorio();
    public final BitacoraRepositorio bitacoraRepo = new BitacoraRepositorio();
    public final ParametroRepositorio parametroRepo = new ParametroRepositorio();
    public final InmuebleRepositorio inmuebleRepo = new InmuebleRepositorio();
    public final PublicacionRepositorio publicacionRepo = new PublicacionRepositorio();
    public final GrupoRepositorio grupoRepo = new GrupoRepositorio();
    public final AporteRepositorio aporteRepo = new AporteRepositorio();
    public final NegociacionRepositorio negociacionRepo = new NegociacionRepositorio();
    public final OfertaRepositorio ofertaRepo = new OfertaRepositorio();
    public final InteresRepositorio interesRepo = new InteresRepositorio();
    public final PqrRepositorio pqrRepo = new PqrRepositorio();
    public final ComentarioRepositorio comentarioRepo = new ComentarioRepositorio();
    public final CalificacionRepositorio calificacionRepo = new CalificacionRepositorio();
    public final ReporteRepositorio reporteRepo = new ReporteRepositorio();

    // servicios
    public final UsuarioServicio usuarios = new UsuarioServicio(usuarioRepo, notificacionRepo, bitacoraRepo, parametroRepo);
    public final AutenticacionServicio autenticacion = new AutenticacionServicio(usuarioRepo, bitacoraRepo);
    public final InmuebleServicio inmuebles = new InmuebleServicio(inmuebleRepo, publicacionRepo);
    public final PublicacionServicio publicaciones = new PublicacionServicio(publicacionRepo);
    public final GrupoServicio grupos = new GrupoServicio(grupoRepo);
    public final AporteServicio aportes = new AporteServicio(aporteRepo);
    public final NegociacionServicio negociaciones = new NegociacionServicio(negociacionRepo, interesRepo);
    public final OfertaServicio ofertas = new OfertaServicio(ofertaRepo);
    public final PqrServicio pqr = new PqrServicio(pqrRepo);
    public final ComentarioServicio comentarios = new ComentarioServicio(comentarioRepo, calificacionRepo);
    public final ModeracionServicio moderacion = new ModeracionServicio();
    public final ReporteServicio reportes = new ReporteServicio();
    public final ExportacionServicio exportacion = new ExportacionServicio();

    private Contexto() {
    }

    public static Contexto get() {
        return INSTANCIA;
    }
}
