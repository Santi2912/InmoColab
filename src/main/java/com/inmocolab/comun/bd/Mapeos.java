package com.inmocolab.comun.bd;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.Secuencia;
import com.inmocolab.compracolaborativa.modelo.Aporte;
import com.inmocolab.compracolaborativa.modelo.EstadoAporte;
import com.inmocolab.compracolaborativa.modelo.EstadoGrupo;
import com.inmocolab.compracolaborativa.modelo.EstadoInvitacion;
import com.inmocolab.compracolaborativa.modelo.EstadoParticipacion;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.compracolaborativa.modelo.Invitacion;
import com.inmocolab.compracolaborativa.modelo.Participacion;
import com.inmocolab.inmuebles.modelo.EstadoInmueble;
import com.inmocolab.inmuebles.modelo.EstadoPublicacion;
import com.inmocolab.inmuebles.modelo.Fotografia;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.inmuebles.modelo.Publicacion;
import com.inmocolab.negociacion.modelo.CambioEstado;
import com.inmocolab.negociacion.modelo.EstadoNegociacion;
import com.inmocolab.negociacion.modelo.EstadoOferta;
import com.inmocolab.negociacion.modelo.Interes;
import com.inmocolab.negociacion.modelo.Negociacion;
import com.inmocolab.negociacion.modelo.Observacion;
import com.inmocolab.negociacion.modelo.Oferta;
import com.inmocolab.pqr.modelo.Adjunto;
import com.inmocolab.pqr.modelo.EstadoPqr;
import com.inmocolab.pqr.modelo.PQR;
import com.inmocolab.pqr.modelo.RespuestaPQR;
import com.inmocolab.pqr.modelo.TipoPqr;
import com.inmocolab.reportes.modelo.ReporteGenerado;
import com.inmocolab.usuarios.modelo.Administrador;
import com.inmocolab.usuarios.modelo.Asesor;
import com.inmocolab.usuarios.modelo.Bitacora;
import com.inmocolab.usuarios.modelo.Comprador;
import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Notificacion;
import com.inmocolab.usuarios.modelo.Parametro;
import com.inmocolab.usuarios.modelo.Propietario;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Define, tabla por tabla, cómo se guardan y se cargan los objetos del dominio.
 * El orden de la lista respeta las llaves foráneas (padres antes que hijos).
 */
final class Mapeos {

    private final Contexto c;
    /** idParticipacion -> idGrupo; lo usan los aportes al cargarse. */
    private final Map<Long, Long> grupoDeParticipacion = new HashMap<>();

    Mapeos(Contexto c) {
        this.c = c;
    }

    List<Mapeo> todos() {
        List<Mapeo> m = new ArrayList<>();
        m.add(usuario());
        m.add(parametro());
        m.add(notificacion());
        m.add(bitacora());
        m.add(inmueble());
        m.add(fotografia());
        m.add(publicacion());
        m.add(grupo());
        m.add(invitacion());
        m.add(participacion());
        m.add(aporte());
        m.add(interes());
        m.add(negociacion());
        m.add(oferta());
        m.add(observacion());
        m.add(cambioEstado());
        m.add(pqr());
        m.add(adjunto());
        m.add(respuesta());
        m.add(reporteGenerado());
        return m;
    }

    // ------------------------------------------------------------ MS-Usuarios

    private Mapeo usuario() {
        String[] col = {"id_usuario", "nombre", "documento", "correo", "telefono", "contrasena_hash", "rol", "estado",
            "fecha_registro", "intentos_fallidos", "bloqueado_hasta", "token_verificacion"};
        return new Mapeo("usuario", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Usuario u : c.usuarioRepo.todos()) {
                f.add(new Fila(new Object[]{u.getId(), u.getNombre(), u.getDocumento(), u.getCorreo(), u.getTelefono(),
                    u.getContrasenaHash(), name(u.getRol()), name(u.getEstado()), ts(u.getFechaRegistro()),
                    u.getIntentosFallidos(), ts(u.getBloqueadoHasta()), u.getTokenVerificacion()}, 1));
            }
            return f;
        }, rs -> {
            Usuario u = nuevoUsuario(Rol.valueOf(rs.getString("rol")));
            u.setId(rs.getLong("id_usuario"));
            u.setNombre(rs.getString("nombre"));
            u.setDocumento(rs.getString("documento"));
            u.setCorreo(rs.getString("correo"));
            u.setTelefono(rs.getString("telefono"));
            u.setContrasenaHash(rs.getString("contrasena_hash"));
            u.setEstado(EstadoUsuario.valueOf(rs.getString("estado")));
            u.setFechaRegistro(ldt(rs, "fecha_registro"));
            u.setIntentosFallidos(rs.getInt("intentos_fallidos"));
            u.setBloqueadoHasta(ldt(rs, "bloqueado_hasta"));
            u.setTokenVerificacion(rs.getString("token_verificacion"));
            c.usuarioRepo.restaurar(u);
        });
    }

    private static Usuario nuevoUsuario(Rol rol) {
        switch (rol) {
            case ADMINISTRADOR: return new Administrador();
            case PROPIETARIO: return new Propietario();
            case ASESOR: return new Asesor();
            default: return new Comprador();
        }
    }

    private Mapeo parametro() {
        String[] col = {"clave", "valor", "descripcion"};
        return new Mapeo("parametro", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Parametro p : c.parametroRepo.todos()) {
                f.add(new Fila(new Object[]{p.getClave(), p.getValor(), p.getDescripcion()}, 1));
            }
            return f;
        }, rs -> c.parametroRepo.guardar(new Parametro(rs.getString("clave"), rs.getString("valor"), rs.getString("descripcion"))));
    }

    private Mapeo notificacion() {
        String[] col = {"id_notificacion", "id_usuario", "mensaje", "enlace", "fecha", "leida"};
        return new Mapeo("notificacion", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Notificacion n : c.notificacionRepo.todos()) {
                f.add(new Fila(new Object[]{n.getId(), n.getIdUsuario(), n.getMensaje(), n.getEnlace(), ts(n.getFecha()), n.isLeida()}, 1));
            }
            return f;
        }, rs -> {
            Notificacion n = new Notificacion();
            n.setId(rs.getLong("id_notificacion"));
            n.setIdUsuario(rs.getLong("id_usuario"));
            n.setMensaje(rs.getString("mensaje"));
            n.setEnlace(rs.getString("enlace"));
            n.setFecha(ldt(rs, "fecha"));
            n.setLeida(rs.getBoolean("leida"));
            c.notificacionRepo.restaurar(n);
        });
    }

    private Mapeo bitacora() {
        String[] col = {"id_bitacora", "id_usuario", "accion", "detalle", "fecha"};
        return new Mapeo("bitacora", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Bitacora b : c.bitacoraRepo.todas()) {
                f.add(new Fila(new Object[]{b.getId(), b.getIdUsuario(), b.getAccion(), b.getDetalle(), ts(b.getFecha())}, 1));
            }
            return f;
        }, rs -> c.bitacoraRepo.restaurar(new Bitacora(rs.getLong("id_bitacora"), rs.getLong("id_usuario"),
                rs.getString("accion"), rs.getString("detalle"), ldt(rs, "fecha"))));
    }

    // ------------------------------------------------------------ MS-Inmuebles

    private Mapeo inmueble() {
        String[] col = {"id_inmueble", "id_propietario", "id_asesor", "direccion", "ciudad", "tipo", "precio", "area",
            "descripcion", "estado", "precio_minimo", "visitas"};
        return new Mapeo("inmueble", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Inmueble i : c.inmuebleRepo.todos()) {
                f.add(new Fila(new Object[]{i.getId(), i.getIdPropietario(), i.getIdAsesor(), i.getDireccion(), i.getCiudad(),
                    i.getTipo(), i.getPrecio(), i.getArea(), i.getDescripcion(), name(i.getEstado()), i.getPrecioMinimo(),
                    i.getVisitas()}, 1));
            }
            return f;
        }, rs -> {
            Inmueble i = new Inmueble();
            i.setId(rs.getLong("id_inmueble"));
            i.setIdPropietario(rs.getLong("id_propietario"));
            i.setIdAsesor(lng(rs, "id_asesor"));
            i.setDireccion(rs.getString("direccion"));
            i.setCiudad(rs.getString("ciudad"));
            i.setTipo(rs.getString("tipo"));
            i.setPrecio(rs.getBigDecimal("precio"));
            i.setArea(rs.getBigDecimal("area"));
            i.setDescripcion(rs.getString("descripcion"));
            i.setEstado(EstadoInmueble.valueOf(rs.getString("estado")));
            i.setPrecioMinimo(rs.getBigDecimal("precio_minimo"));
            i.setVisitas(rs.getInt("visitas"));
            c.inmuebleRepo.restaurar(i);
        });
    }

    private Mapeo fotografia() {
        String[] col = {"id_foto", "id_inmueble", "url", "orden", "nombre", "content_type", "datos"};
        return new Mapeo("fotografia", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Inmueble i : c.inmuebleRepo.todos()) {
                for (Fotografia x : i.getFotografias()) {
                    f.add(new Fila(new Object[]{x.getId(), i.getId(), x.getUrl(), x.getOrden(), x.getNombre(), x.getContentType(),
                        x.getDatos()}, 1));
                }
            }
            return f;
        }, rs -> {
            Fotografia x = new Fotografia();
            x.setId(rs.getLong("id_foto"));
            x.setUrl(rs.getString("url"));
            x.setOrden(rs.getInt("orden"));
            x.setNombre(rs.getString("nombre"));
            x.setContentType(rs.getString("content_type"));
            x.setDatos(rs.getBytes("datos"));
            c.inmuebleRepo.buscar(rs.getLong("id_inmueble")).getFotografias().add(x);
            Secuencia.asegurarMinimo(x.getId());
        });
    }

    private Mapeo publicacion() {
        String[] col = {"id_publicacion", "id_inmueble", "id_administrador", "fecha_publicacion", "estado", "motivo_rechazo"};
        return new Mapeo("publicacion", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Publicacion p : c.publicacionRepo.todos()) {
                f.add(new Fila(new Object[]{p.getId(), p.getIdInmueble(), p.getIdAdministrador(), ts(p.getFechaPublicacion()),
                    name(p.getEstado()), p.getMotivoRechazo()}, 1));
            }
            return f;
        }, rs -> {
            Publicacion p = new Publicacion();
            p.setId(rs.getLong("id_publicacion"));
            p.setIdInmueble(rs.getLong("id_inmueble"));
            p.setIdAdministrador(lng(rs, "id_administrador"));
            p.setFechaPublicacion(ldt(rs, "fecha_publicacion"));
            p.setEstado(EstadoPublicacion.valueOf(rs.getString("estado")));
            p.setMotivoRechazo(rs.getString("motivo_rechazo"));
            c.publicacionRepo.restaurar(p);
        });
    }

    // ------------------------------------------------------------ MS-CompraColaborativa

    private Mapeo grupo() {
        String[] col = {"id_grupo", "id_inmueble", "id_creador", "monto_objetivo", "monto_recaudado", "fecha_creacion",
            "fecha_limite", "estado"};
        return new Mapeo("grupo_compra", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (GrupoCompra g : c.grupoRepo.todos()) {
                f.add(new Fila(new Object[]{g.getId(), g.getIdInmueble(), g.getIdCreador(), g.getMontoObjetivo(),
                    g.getMontoRecaudado(), ts(g.getFechaCreacion()), dt(g.getFechaLimite()), name(g.getEstado())}, 1));
            }
            return f;
        }, rs -> {
            GrupoCompra g = new GrupoCompra();
            g.setId(rs.getLong("id_grupo"));
            g.setIdInmueble(rs.getLong("id_inmueble"));
            g.setIdCreador(rs.getLong("id_creador"));
            g.setMontoObjetivo(rs.getBigDecimal("monto_objetivo"));
            g.setMontoRecaudado(rs.getBigDecimal("monto_recaudado"));
            g.setFechaCreacion(ldt(rs, "fecha_creacion"));
            g.setFechaLimite(ld(rs, "fecha_limite"));
            g.setEstado(EstadoGrupo.valueOf(rs.getString("estado")));
            c.grupoRepo.restaurar(g);
        });
    }

    private Mapeo invitacion() {
        String[] col = {"id_invitacion", "id_grupo", "id_invitado", "fecha_envio", "estado"};
        return new Mapeo("invitacion", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (GrupoCompra g : c.grupoRepo.todos()) {
                for (Invitacion x : g.getInvitaciones()) {
                    f.add(new Fila(new Object[]{x.getId(), g.getId(), x.getIdInvitado(), ts(x.getFechaEnvio()), name(x.getEstado())}, 1));
                }
            }
            return f;
        }, rs -> {
            Invitacion x = new Invitacion();
            x.setId(rs.getLong("id_invitacion"));
            x.setIdGrupo(rs.getLong("id_grupo"));
            x.setIdInvitado(rs.getLong("id_invitado"));
            x.setFechaEnvio(ldt(rs, "fecha_envio"));
            x.setEstado(EstadoInvitacion.valueOf(rs.getString("estado")));
            c.grupoRepo.buscar(x.getIdGrupo()).getInvitaciones().add(x);
            Secuencia.asegurarMinimo(x.getId());
        });
    }

    private Mapeo participacion() {
        String[] col = {"id_participacion", "id_grupo", "id_comprador", "porcentaje", "fecha_ingreso", "estado"};
        return new Mapeo("participacion", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (GrupoCompra g : c.grupoRepo.todos()) {
                for (Participacion p : g.getParticipaciones()) {
                    f.add(new Fila(new Object[]{p.getId(), g.getId(), p.getIdComprador(), p.getPorcentaje(), ts(p.getFechaIngreso()),
                        name(p.getEstado())}, 1));
                }
            }
            return f;
        }, rs -> {
            Participacion p = new Participacion();
            p.setId(rs.getLong("id_participacion"));
            p.setIdGrupo(rs.getLong("id_grupo"));
            p.setIdComprador(rs.getLong("id_comprador"));
            p.setPorcentaje(rs.getBigDecimal("porcentaje"));
            p.setFechaIngreso(ldt(rs, "fecha_ingreso"));
            p.setEstado(EstadoParticipacion.valueOf(rs.getString("estado")));
            c.grupoRepo.buscar(p.getIdGrupo()).getParticipaciones().add(p);
            grupoDeParticipacion.put(p.getId(), p.getIdGrupo());
            Secuencia.asegurarMinimo(p.getId());
        });
    }

    private Mapeo aporte() {
        String[] col = {"id_aporte", "id_participacion", "id_verificador", "monto", "fecha", "comprobante_url", "estado", "motivo_rechazo"};
        return new Mapeo("aporte", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Aporte a : c.aporteRepo.todos()) {
                f.add(new Fila(new Object[]{a.getId(), a.getIdParticipacion(), a.getIdVerificador(), a.getMonto(), ts(a.getFecha()),
                    a.getComprobante(), name(a.getEstado()), a.getMotivoRechazo()}, 1));
            }
            return f;
        }, rs -> {
            Aporte a = new Aporte();
            a.setId(rs.getLong("id_aporte"));
            a.setIdParticipacion(rs.getLong("id_participacion"));
            a.setIdVerificador(lng(rs, "id_verificador"));
            a.setMonto(rs.getBigDecimal("monto"));
            a.setFecha(ldt(rs, "fecha"));
            a.setComprobante(rs.getString("comprobante_url"));
            a.setEstado(EstadoAporte.valueOf(rs.getString("estado")));
            a.setMotivoRechazo(rs.getString("motivo_rechazo"));
            long idGrupo = grupoDeParticipacion.get(a.getIdParticipacion());
            a.setIdGrupo(idGrupo);
            for (Participacion p : c.grupoRepo.buscar(idGrupo).getParticipaciones()) {
                if (p.getId() == a.getIdParticipacion()) {
                    p.getAportes().add(a);
                }
            }
            c.aporteRepo.restaurar(a);
        });
    }

    // ------------------------------------------------------------ MS-Negociacion

    private Mapeo interes() {
        String[] col = {"id_interes", "id_inmueble", "id_comprador", "fecha"};
        return new Mapeo("interes", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Interes i : c.interesRepo.todos()) {
                f.add(new Fila(new Object[]{i.getId(), i.getIdInmueble(), i.getIdComprador(), ts(i.getFecha())}, 1));
            }
            return f;
        }, rs -> {
            Interes i = new Interes();
            i.setId(rs.getLong("id_interes"));
            i.setIdInmueble(rs.getLong("id_inmueble"));
            i.setIdComprador(rs.getLong("id_comprador"));
            i.setFecha(ldt(rs, "fecha"));
            c.interesRepo.restaurar(i);
        });
    }

    private Mapeo negociacion() {
        String[] col = {"id_negociacion", "id_inmueble", "id_asesor", "fecha_inicio", "fecha_fin", "estado", "ultima_actividad"};
        return new Mapeo("negociacion", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Negociacion n : c.negociacionRepo.todos()) {
                f.add(new Fila(new Object[]{n.getId(), n.getIdInmueble(), n.getIdAsesor(), ts(n.getFechaInicio()), ts(n.getFechaFin()),
                    name(n.getEstado()), ts(n.getUltimaActividad())}, 1));
            }
            return f;
        }, rs -> {
            Negociacion n = new Negociacion();
            n.setId(rs.getLong("id_negociacion"));
            n.setIdInmueble(rs.getLong("id_inmueble"));
            n.setIdAsesor(lng(rs, "id_asesor"));
            n.setFechaInicio(ldt(rs, "fecha_inicio"));
            n.setFechaFin(ldt(rs, "fecha_fin"));
            n.setEstado(EstadoNegociacion.valueOf(rs.getString("estado")));
            n.setUltimaActividad(ldt(rs, "ultima_actividad"));
            c.negociacionRepo.restaurar(n);
        });
    }

    private Mapeo oferta() {
        String[] col = {"id_oferta", "id_negociacion", "id_comprador", "id_grupo", "monto", "fecha", "estado", "motivo_rechazo", "condiciones"};
        return new Mapeo("oferta", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Oferta o : c.ofertaRepo.todos()) {
                f.add(new Fila(new Object[]{o.getId(), o.getIdNegociacion(), o.getIdComprador(), o.getIdGrupo(), o.getMonto(),
                    ts(o.getFecha()), name(o.getEstado()), o.getMotivoRechazo(), o.getCondiciones()}, 1));
            }
            return f;
        }, rs -> {
            Oferta o = new Oferta();
            o.setId(rs.getLong("id_oferta"));
            o.setIdNegociacion(rs.getLong("id_negociacion"));
            o.setIdComprador(lng(rs, "id_comprador"));
            o.setIdGrupo(lng(rs, "id_grupo"));
            o.setMonto(rs.getBigDecimal("monto"));
            o.setFecha(ldt(rs, "fecha"));
            o.setEstado(EstadoOferta.valueOf(rs.getString("estado")));
            o.setMotivoRechazo(rs.getString("motivo_rechazo"));
            o.setCondiciones(rs.getString("condiciones"));
            c.negociacionRepo.buscar(o.getIdNegociacion()).getOfertas().add(o);
            c.ofertaRepo.restaurar(o);
        });
    }

    private Mapeo observacion() {
        String[] col = {"id_observacion", "id_negociacion", "id_asesor", "texto", "fecha"};
        return new Mapeo("observacion", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (Negociacion n : c.negociacionRepo.todos()) {
                for (Observacion o : n.getObservaciones()) {
                    f.add(new Fila(new Object[]{o.getId(), n.getId(), o.getIdAsesor(), o.getTexto(), ts(o.getFecha())}, 1));
                }
            }
            return f;
        }, rs -> {
            Observacion o = new Observacion();
            o.setId(rs.getLong("id_observacion"));
            o.setIdAsesor(rs.getLong("id_asesor"));
            o.setTexto(rs.getString("texto"));
            o.setFecha(ldt(rs, "fecha"));
            c.negociacionRepo.buscar(rs.getLong("id_negociacion")).getObservaciones().add(o);
            Secuencia.asegurarMinimo(o.getId());
        });
    }

    private Mapeo cambioEstado() {
        String[] col = {"id_negociacion", "orden", "estado", "fecha", "detalle"};
        return new Mapeo("cambio_estado_negociacion", col, 2, () -> {
            List<Fila> f = new ArrayList<>();
            for (Negociacion n : c.negociacionRepo.todos()) {
                int orden = 0;
                for (CambioEstado x : n.getHistorial()) {
                    f.add(new Fila(new Object[]{n.getId(), orden++, name(x.getEstado()), ts(x.getFecha()), x.getDetalle()}, 2));
                }
            }
            return f;
        }, rs -> c.negociacionRepo.buscar(rs.getLong("id_negociacion")).getHistorial().add(
                new CambioEstado(EstadoNegociacion.valueOf(rs.getString("estado")), ldt(rs, "fecha"), rs.getString("detalle"))));
    }

    // ------------------------------------------------------------ MS-PQR

    private Mapeo pqr() {
        String[] col = {"id_pqr", "radicado", "id_usuario", "id_responsable", "tipo", "asunto", "descripcion", "fecha_radicacion",
            "fecha_limite", "estado", "calificacion", "id_inmueble", "id_negociacion", "fecha_respuesta", "fecha_cierre", "reabierta"};
        return new Mapeo("pqr", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (PQR p : c.pqrRepo.todos()) {
                f.add(new Fila(new Object[]{p.getId(), p.getRadicado(), p.getIdUsuario(), p.getIdResponsable(), name(p.getTipo()),
                    p.getAsunto(), p.getDescripcion(), ts(p.getFechaRadicacion()), dt(p.getFechaLimite()), name(p.getEstado()),
                    p.getCalificacion(), p.getIdInmueble(), p.getIdNegociacion(), ts(p.getFechaRespuesta()), ts(p.getFechaCierre()),
                    p.isReabierta()}, 1));
            }
            return f;
        }, rs -> {
            PQR p = new PQR();
            p.setId(rs.getLong("id_pqr"));
            p.setRadicado(rs.getString("radicado"));
            p.setIdUsuario(rs.getLong("id_usuario"));
            p.setIdResponsable(lng(rs, "id_responsable"));
            p.setTipo(TipoPqr.valueOf(rs.getString("tipo")));
            p.setAsunto(rs.getString("asunto"));
            p.setDescripcion(rs.getString("descripcion"));
            p.setFechaRadicacion(ldt(rs, "fecha_radicacion"));
            p.setFechaLimite(ld(rs, "fecha_limite"));
            p.setEstado(EstadoPqr.valueOf(rs.getString("estado")));
            int cal = rs.getInt("calificacion");
            p.setCalificacion(rs.wasNull() ? null : cal);
            p.setIdInmueble(lng(rs, "id_inmueble"));
            p.setIdNegociacion(lng(rs, "id_negociacion"));
            p.setFechaRespuesta(ldt(rs, "fecha_respuesta"));
            p.setFechaCierre(ldt(rs, "fecha_cierre"));
            p.setReabierta(rs.getBoolean("reabierta"));
            c.pqrRepo.restaurar(p);
        });
    }

    private Mapeo adjunto() {
        String[] col = {"id_pqr", "orden", "url", "tamano"};
        return new Mapeo("adjunto_pqr", col, 2, () -> {
            List<Fila> f = new ArrayList<>();
            for (PQR p : c.pqrRepo.todos()) {
                int orden = 0;
                for (Adjunto a : p.getAdjuntos()) {
                    f.add(new Fila(new Object[]{p.getId(), orden++, a.getNombre(), a.getTamano()}, 2));
                }
            }
            return f;
        }, rs -> c.pqrRepo.buscar(rs.getLong("id_pqr")).getAdjuntos().add(new Adjunto(rs.getString("url"), rs.getLong("tamano"))));
    }

    private Mapeo respuesta() {
        String[] col = {"id_respuesta", "id_pqr", "id_autor", "texto", "fecha"};
        return new Mapeo("respuesta_pqr", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (PQR p : c.pqrRepo.todos()) {
                for (RespuestaPQR r : p.getRespuestas()) {
                    f.add(new Fila(new Object[]{r.getId(), p.getId(), r.getIdAutor(), r.getTexto(), ts(r.getFecha())}, 1));
                }
            }
            return f;
        }, rs -> {
            RespuestaPQR r = new RespuestaPQR();
            r.setId(rs.getLong("id_respuesta"));
            r.setIdAutor(rs.getLong("id_autor"));
            r.setTexto(rs.getString("texto"));
            r.setFecha(ldt(rs, "fecha"));
            c.pqrRepo.buscar(rs.getLong("id_pqr")).getRespuestas().add(r);
            Secuencia.asegurarMinimo(r.getId());
        });
    }

    // ------------------------------------------------------------ MS-Reportes

    private Mapeo reporteGenerado() {
        String[] col = {"id_reporte", "id_usuario", "tipo", "filtros", "formato", "fecha"};
        return new Mapeo("reporte_generado", col, 1, () -> {
            List<Fila> f = new ArrayList<>();
            for (ReporteGenerado r : c.reporteRepo.todos()) {
                f.add(new Fila(new Object[]{r.getId(), r.getIdUsuario(), r.getTipo(), null, r.getFormato(), ts(r.getFecha())}, 1));
            }
            return f;
        }, rs -> {
            ReporteGenerado r = new ReporteGenerado();
            r.setId(rs.getLong("id_reporte"));
            r.setIdUsuario(rs.getLong("id_usuario"));
            r.setTipo(rs.getString("tipo"));
            r.setFormato(rs.getString("formato"));
            r.setFecha(ldt(rs, "fecha"));
            c.reporteRepo.restaurar(r);
        });
    }

    // ------------------------------------------------------------ conversiones

    private static String name(Enum<?> e) {
        return e == null ? null : e.name();
    }

    private static Timestamp ts(LocalDateTime t) {
        return t == null ? null : Timestamp.valueOf(t);
    }

    private static java.sql.Date dt(LocalDate d) {
        return d == null ? null : java.sql.Date.valueOf(d);
    }

    private static LocalDateTime ldt(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    private static LocalDate ld(ResultSet rs, String col) throws SQLException {
        java.sql.Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    private static Long lng(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }
}
