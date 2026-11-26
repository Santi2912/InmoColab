package com.inmocolab.reportes.servicio;

import com.inmocolab.comentarios.modelo.Comentario;
import com.inmocolab.comentarios.modelo.EstadoComentario;
import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Util;
import com.inmocolab.compracolaborativa.modelo.Aporte;
import com.inmocolab.compracolaborativa.modelo.EstadoAporte;
import com.inmocolab.compracolaborativa.modelo.EstadoParticipacion;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.compracolaborativa.modelo.Participacion;
import com.inmocolab.inmuebles.modelo.EstadoInmueble;
import com.inmocolab.inmuebles.modelo.EstadoPublicacion;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.inmuebles.modelo.Publicacion;
import com.inmocolab.negociacion.modelo.EstadoNegociacion;
import com.inmocolab.negociacion.modelo.Interes;
import com.inmocolab.negociacion.modelo.Negociacion;
import com.inmocolab.negociacion.modelo.Oferta;
import com.inmocolab.pqr.modelo.EstadoPqr;
import com.inmocolab.pqr.modelo.PQR;
import com.inmocolab.pqr.modelo.TipoPqr;
import com.inmocolab.reportes.modelo.Reporte;
import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Generación y filtrado de reportes según el rol (CU-07). */
public class ReporteServicio {

    public static final String MIS_INMUEBLES = "MIS_INMUEBLES";
    public static final String HISTORIAL = "HISTORIAL";
    public static final String NEGOCIACIONES_ASIGNADAS = "NEGOCIACIONES_ASIGNADAS";
    public static final String USUARIOS = "USUARIOS";
    public static final String INMUEBLES = "INMUEBLES";
    public static final String NEGOCIACIONES_APORTES = "NEGOCIACIONES_APORTES";
    public static final String PQR_COMENTARIOS = "PQR_COMENTARIOS";

    /** Reportes que cada rol puede consultar (código -> nombre). */
    public Map<String, String> tiposPara(Rol rol) {
        Map<String, String> t = new LinkedHashMap<>();
        switch (rol) {
            case PROPIETARIO:
                t.put(MIS_INMUEBLES, "Reporte de mis inmuebles");
                break;
            case COMPRADOR:
                t.put(HISTORIAL, "Historial de aportes y ofertas");
                break;
            case ASESOR:
                t.put(NEGOCIACIONES_ASIGNADAS, "Negociaciones asignadas");
                break;
            default:
                t.put(USUARIOS, "Reporte de usuarios");
                t.put(INMUEBLES, "Reporte de inmuebles");
                t.put(NEGOCIACIONES_APORTES, "Reporte de negociaciones y aportes");
                t.put(PQR_COMENTARIOS, "Reporte de PQR y comentarios");
        }
        return t;
    }

    public Reporte generar(Usuario u, String tipo, LocalDate desde, LocalDate hasta, String ciudad, String estado) {
        if (tipo == null || !tiposPara(u.getRol()).containsKey(tipo)) {
            throw new NegocioException("Tu rol no tiene acceso a ese reporte.");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new NegocioException("La fecha inicial no puede ser posterior a la final.");
        }
        switch (tipo) {
            case MIS_INMUEBLES: return misInmuebles(u, desde, hasta);
            case HISTORIAL: return historial(u, desde, hasta, estado);
            case NEGOCIACIONES_ASIGNADAS: return asignadas(u, desde, hasta, estado);
            case USUARIOS: return usuarios(desde, hasta);
            case INMUEBLES: return inmuebles(desde, hasta, ciudad);
            case NEGOCIACIONES_APORTES: return negociacionesYAportes(desde, hasta, ciudad);
            default: return pqrYComentarios(desde, hasta);
        }
    }

    // ------------------------------------------------------------ HU-36

    private Reporte misInmuebles(Usuario u, LocalDate desde, LocalDate hasta) {
        Contexto c = Contexto.get();
        Reporte r = new Reporte(MIS_INMUEBLES, "Reporte de mis inmuebles", "Inmueble", "Estado", "Visitas", "Interesados", "Ofertas");
        r.setTituloSerie("Ofertas por inmueble");
        int visitas = 0;
        int interesados = 0;
        int ofertas = 0;
        for (Inmueble i : c.inmuebles.misInmuebles(u.getId())) {
            int in = 0;
            for (Interes x : c.negociaciones.interesesDe(i.getId())) {
                if (enRango(x.getFecha(), desde, hasta)) {
                    in++;
                }
            }
            int of = 0;
            for (Negociacion n : c.negociaciones.negociacionesDe(i.getId())) {
                for (Oferta o : n.getOfertas()) {
                    if (enRango(o.getFecha(), desde, hasta)) {
                        of++;
                    }
                }
            }
            r.agregarFila(false, i.getDireccion() + ", " + i.getCiudad(), Util.etiqueta(i.getEstado()),
                    String.valueOf(i.getVisitas()), String.valueOf(in), String.valueOf(of));
            r.getSerie().put("#" + i.getId(), (double) of);
            visitas += i.getVisitas();
            interesados += in;
            ofertas += of;
        }
        r.getIndicadores().put("Inmuebles", String.valueOf(r.getFilas().size()));
        r.getIndicadores().put("Visitas", String.valueOf(visitas));
        r.getIndicadores().put("Interesados", String.valueOf(interesados));
        r.getIndicadores().put("Ofertas recibidas", String.valueOf(ofertas));
        return r;
    }

    // ------------------------------------------------------------ HU-37

    private Reporte historial(Usuario u, LocalDate desde, LocalDate hasta, String estado) {
        Contexto c = Contexto.get();
        Reporte r = new Reporte(HISTORIAL, "Historial de aportes y ofertas", "Fecha", "Movimiento", "Grupo / Inmueble", "Monto", "Estado");
        r.setTituloSerie("Aportes por estado (monto)");
        List<Object[]> movimientos = new ArrayList<>();
        BigDecimal confirmado = BigDecimal.ZERO;
        BigDecimal pendiente = BigDecimal.ZERO;
        for (Aporte a : c.aportes.deComprador(u.getId())) {
            GrupoCompra g = c.grupos.requerir(a.getIdGrupo());
            Inmueble i = c.inmuebles.requerir(g.getIdInmueble());
            if (coincide(a.getEstado().name(), estado) && enRango(a.getFecha(), desde, hasta)) {
                movimientos.add(new Object[]{a.getFecha(), "Aporte", "Grupo #" + g.getId() + " · " + i.getDireccion(), a.getMonto(), a.getEstado().name()});
                if (a.getEstado() == EstadoAporte.CONFIRMADO) {
                    confirmado = confirmado.add(a.getMonto());
                } else if (a.getEstado() == EstadoAporte.PENDIENTE) {
                    pendiente = pendiente.add(a.getMonto());
                }
            }
        }
        for (Oferta o : c.ofertas.todas()) {
            boolean propia = o.getIdComprador() != null && o.getIdComprador() == u.getId();
            boolean delGrupo = false;
            if (o.getIdGrupo() != null) {
                delGrupo = c.grupos.requerir(o.getIdGrupo()).getParticipaciones().stream()
                        .anyMatch(p -> p.getIdComprador() == u.getId() && p.getEstado() == EstadoParticipacion.ACTIVA);
            }
            if ((propia || delGrupo) && coincide(o.getEstado().name(), estado) && enRango(o.getFecha(), desde, hasta)) {
                Inmueble i = c.inmuebles.requerir(c.negociaciones.requerir(o.getIdNegociacion()).getIdInmueble());
                movimientos.add(new Object[]{o.getFecha(), delGrupo ? "Oferta de grupo" : "Oferta", i.getDireccion(), o.getMonto(), o.getEstado().name()});
            }
        }
        Collections.sort(movimientos, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] x, Object[] y) {
                return ((LocalDateTime) y[0]).compareTo((LocalDateTime) x[0]);
            }
        });
        for (Object[] m : movimientos) {
            r.agregarFila(false, Util.fecha((LocalDateTime) m[0]), (String) m[1], (String) m[2], Util.dinero((BigDecimal) m[3]),
                    Util.etiqueta(m[4]));
        }
        r.getIndicadores().put("Movimientos", String.valueOf(movimientos.size()));
        r.getIndicadores().put("Total aportado confirmado", Util.dinero(confirmado));
        r.getIndicadores().put("Aportes pendientes", Util.dinero(pendiente));
        r.getSerie().put("Confirmado", confirmado.doubleValue());
        r.getSerie().put("Pendiente", pendiente.doubleValue());
        return r;
    }

    // ------------------------------------------------------------ HU-38

    private Reporte asignadas(Usuario u, LocalDate desde, LocalDate hasta, String estado) {
        Contexto c = Contexto.get();
        Reporte r = new Reporte(NEGOCIACIONES_ASIGNADAS, "Negociaciones asignadas", "N.°", "Inmueble", "Estado", "Inicio", "Última actividad", "Días sin cambios");
        r.setTituloSerie("Negociaciones por estado");
        int estancadas = 0;
        Map<String, Double> porEstado = new TreeMap<>();
        for (Negociacion n : c.negociaciones.misNegociaciones(u)) {
            if (!coincide(n.getEstado().name(), estado) || !enRango(n.getFechaInicio(), desde, hasta)) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(n.getUltimaActividad().toLocalDate(), LocalDate.now());
            boolean activa = n.getEstado() != EstadoNegociacion.CERRADA && n.getEstado() != EstadoNegociacion.CANCELADA;
            boolean alerta = activa && dias > 30;
            if (alerta) {
                estancadas++;
            }
            Inmueble i = c.inmuebles.requerir(n.getIdInmueble());
            r.agregarFila(alerta, "#" + n.getId(), i.getDireccion(), Util.etiqueta(n.getEstado()), Util.fecha(n.getFechaInicio()),
                    Util.fecha(n.getUltimaActividad()), String.valueOf(dias));
            porEstado.merge(Util.etiqueta(n.getEstado()), 1.0, Double::sum);
        }
        r.getSerie().putAll(porEstado);
        r.getIndicadores().put("Negociaciones", String.valueOf(r.getFilas().size()));
        r.getIndicadores().put("Con más de 30 días sin cambios", String.valueOf(estancadas));
        return r;
    }

    // ------------------------------------------------------------ HU-39

    private Reporte usuarios(LocalDate desde, LocalDate hasta) {
        Reporte r = new Reporte(USUARIOS, "Reporte de usuarios", "Rol", "Registrados", "Activos", "Suspendidos", "Bloqueados", "Pendientes");
        r.setTituloSerie("Registros por mes");
        Map<String, Double> porMes = new TreeMap<>();
        int total = 0;
        for (Rol rol : Rol.values()) {
            int reg = 0;
            int act = 0;
            int sus = 0;
            int blo = 0;
            int pen = 0;
            for (Usuario x : Contexto.get().usuarios.porRol(rol)) {
                if (!enRango(x.getFechaRegistro(), desde, hasta)) {
                    continue;
                }
                reg++;
                if (x.getEstado() == EstadoUsuario.ACTIVO) {
                    act++;
                } else if (x.getEstado() == EstadoUsuario.SUSPENDIDO) {
                    sus++;
                } else if (x.getEstado() == EstadoUsuario.BLOQUEADO) {
                    blo++;
                } else {
                    pen++;
                }
                porMes.merge(x.getFechaRegistro().getYear() + "-" + String.format("%02d", x.getFechaRegistro().getMonthValue()), 1.0, Double::sum);
            }
            total += reg;
            r.agregarFila(false, Util.etiqueta(rol), String.valueOf(reg), String.valueOf(act), String.valueOf(sus), String.valueOf(blo), String.valueOf(pen));
        }
        r.getSerie().putAll(porMes);
        r.getIndicadores().put("Total de usuarios", String.valueOf(total));
        return r;
    }

    // ------------------------------------------------------------ HU-40

    private Reporte inmuebles(LocalDate desde, LocalDate hasta, String ciudad) {
        Contexto c = Contexto.get();
        Reporte r = new Reporte(INMUEBLES, "Reporte de inmuebles", "Ciudad", "Tipo", "Publicados", "En validación", "Rechazados", "Retirados", "Vendidos");
        r.setTituloSerie("Inmuebles por ciudad");
        Map<String, int[]> grupos = new TreeMap<>();
        Map<String, Double> porCiudad = new TreeMap<>();
        int[] totales = new int[5];
        for (Inmueble i : c.inmuebles.todos()) {
            Publicacion p = c.inmuebles.publicacionDe(i.getId());
            LocalDateTime fecha = p == null ? null : p.getFechaPublicacion();
            if (!Util.vacio(ciudad) && !i.getCiudad().equalsIgnoreCase(ciudad)) {
                continue;
            }
            if (fecha != null && !enRango(fecha, desde, hasta)) {
                continue;
            }
            int[] fila = grupos.computeIfAbsent(i.getCiudad() + "|" + i.getTipo(), k -> new int[5]);
            int idx;
            if (i.getEstado() == EstadoInmueble.VENDIDO) {
                idx = 4;
            } else if (i.getEstado() == EstadoInmueble.RETIRADO) {
                idx = 3;
            } else if (p != null && p.getEstado() == EstadoPublicacion.PENDIENTE) {
                idx = 1;
            } else if (p != null && p.getEstado() == EstadoPublicacion.RECHAZADA) {
                idx = 2;
            } else {
                idx = 0;
            }
            fila[idx]++;
            totales[idx]++;
            porCiudad.merge(i.getCiudad(), 1.0, Double::sum);
        }
        for (Map.Entry<String, int[]> e : grupos.entrySet()) {
            String[] k = e.getKey().split("\\|");
            int[] f = e.getValue();
            r.agregarFila(false, k[0], k[1], String.valueOf(f[0]), String.valueOf(f[1]), String.valueOf(f[2]), String.valueOf(f[3]), String.valueOf(f[4]));
        }
        r.getSerie().putAll(porCiudad);
        r.getIndicadores().put("Publicados", String.valueOf(totales[0]));
        r.getIndicadores().put("En validación", String.valueOf(totales[1]));
        r.getIndicadores().put("Rechazados", String.valueOf(totales[2]));
        r.getIndicadores().put("Retirados", String.valueOf(totales[3]));
        r.getIndicadores().put("Vendidos", String.valueOf(totales[4]));
        return r;
    }

    // ------------------------------------------------------------ HU-41

    private Reporte negociacionesYAportes(LocalDate desde, LocalDate hasta, String ciudad) {
        Contexto c = Contexto.get();
        Reporte r = new Reporte(NEGOCIACIONES_APORTES, "Reporte de negociaciones y aportes", "Estado de la negociación", "Cantidad", "Monto de ofertas aceptadas");
        r.setTituloSerie("Negociaciones por estado");
        Map<EstadoNegociacion, int[]> cuenta = new LinkedHashMap<>();
        Map<EstadoNegociacion, BigDecimal> montos = new LinkedHashMap<>();
        for (EstadoNegociacion e : EstadoNegociacion.values()) {
            cuenta.put(e, new int[1]);
            montos.put(e, BigDecimal.ZERO);
        }
        int total = 0;
        for (Negociacion n : c.negociaciones.todas()) {
            Inmueble i = c.inmuebles.requerir(n.getIdInmueble());
            if (!enRango(n.getFechaInicio(), desde, hasta) || (!Util.vacio(ciudad) && !i.getCiudad().equalsIgnoreCase(ciudad))) {
                continue;
            }
            total++;
            cuenta.get(n.getEstado())[0]++;
            for (Oferta o : n.getOfertas()) {
                if (o.getEstado() == com.inmocolab.negociacion.modelo.EstadoOferta.ACEPTADA) {
                    montos.put(n.getEstado(), montos.get(n.getEstado()).add(o.getMonto()));
                }
            }
        }
        for (EstadoNegociacion e : EstadoNegociacion.values()) {
            r.agregarFila(false, Util.etiqueta(e), String.valueOf(cuenta.get(e)[0]), Util.dinero(montos.get(e)));
            r.getSerie().put(Util.etiqueta(e), (double) cuenta.get(e)[0]);
        }
        BigDecimal recaudado = BigDecimal.ZERO;
        for (Aporte a : c.aportes.todos()) {
            GrupoCompra g = c.grupos.requerir(a.getIdGrupo());
            Inmueble i = c.inmuebles.requerir(g.getIdInmueble());
            if (a.getEstado() == EstadoAporte.CONFIRMADO && enRango(a.getFecha(), desde, hasta)
                    && (Util.vacio(ciudad) || i.getCiudad().equalsIgnoreCase(ciudad))) {
                recaudado = recaudado.add(a.getMonto());
            }
        }
        int cerradas = cuenta.get(EstadoNegociacion.CERRADA)[0];
        r.getIndicadores().put("Negociaciones", String.valueOf(total));
        r.getIndicadores().put("Monto total recaudado (aportes confirmados)", Util.dinero(recaudado));
        r.getIndicadores().put("Tasa de negociaciones cerradas", total == 0 ? "0 %" : Math.round(cerradas * 100.0 / total) + " %");
        return r;
    }

    // ------------------------------------------------------------ HU-42

    private Reporte pqrYComentarios(LocalDate desde, LocalDate hasta) {
        Contexto c = Contexto.get();
        Reporte r = new Reporte(PQR_COMENTARIOS, "Reporte de PQR y comentarios", "Tipo", "Radicadas", "En trámite", "Respondidas", "Cerradas");
        r.setTituloSerie("PQR por tipo");
        long sumaDias = 0;
        int respondidas = 0;
        for (TipoPqr t : TipoPqr.values()) {
            int[] f = new int[4];
            for (PQR p : c.pqr.todas()) {
                if (p.getTipo() != t || !enRango(p.getFechaRadicacion(), desde, hasta)) {
                    continue;
                }
                f[p.getEstado().ordinal()]++;
                if (p.getFechaRespuesta() != null) {
                    sumaDias += ChronoUnit.DAYS.between(p.getFechaRadicacion().toLocalDate(), p.getFechaRespuesta().toLocalDate());
                    respondidas++;
                }
            }
            r.agregarFila(false, Util.etiqueta(t), String.valueOf(f[EstadoPqr.RADICADA.ordinal()]), String.valueOf(f[EstadoPqr.EN_TRAMITE.ordinal()]),
                    String.valueOf(f[EstadoPqr.RESPONDIDA.ordinal()]), String.valueOf(f[EstadoPqr.CERRADA.ordinal()]));
            r.getSerie().put(Util.etiqueta(t), (double) (f[0] + f[1] + f[2] + f[3]));
        }
        int reportados = 0;
        int ocultos = 0;
        for (Comentario cm : c.comentarios.todos()) {
            if (!enRango(cm.getFecha(), desde, hasta)) {
                continue;
            }
            if (!cm.getReportes().isEmpty() || cm.getEstado() == EstadoComentario.OCULTO) {
                reportados++;
            }
            if (cm.getEstado() == EstadoComentario.OCULTO) {
                ocultos++;
            }
        }
        r.getIndicadores().put("Tiempo promedio de respuesta", respondidas == 0 ? "Sin datos" : String.format("%.1f días", sumaDias / (double) respondidas));
        r.getIndicadores().put("Comentarios reportados", String.valueOf(reportados));
        r.getIndicadores().put("Comentarios ocultados", String.valueOf(ocultos));
        return r;
    }

    // ------------------------------------------------------------ utilidades

    private boolean enRango(LocalDateTime f, LocalDate desde, LocalDate hasta) {
        if (f == null) {
            return true;
        }
        LocalDate d = f.toLocalDate();
        return (desde == null || !d.isBefore(desde)) && (hasta == null || !d.isAfter(hasta));
    }

    private boolean coincide(String valor, String filtro) {
        return Util.vacio(filtro) || valor.equalsIgnoreCase(filtro.trim());
    }
}
