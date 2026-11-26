package com.inmocolab.inmuebles.servicio;

import com.inmocolab.comun.Contexto;
import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Pagina;
import com.inmocolab.comun.Secuencia;
import com.inmocolab.comun.Util;
import com.inmocolab.inmuebles.modelo.EstadoInmueble;
import com.inmocolab.inmuebles.modelo.EstadoPublicacion;
import com.inmocolab.inmuebles.modelo.Fotografia;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.inmuebles.modelo.Publicacion;
import com.inmocolab.inmuebles.persistencia.InmuebleRepositorio;
import com.inmocolab.inmuebles.persistencia.PublicacionRepositorio;
import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Registro, edición, retiro y búsqueda de inmuebles (CU-02). */
public class InmuebleServicio {

    public static final int TAMANO_PAGINA = 12;
    public static final int MIN_FOTOS = 3;
    public static final long MAX_BYTES_FOTO = 5L * 1024 * 1024;

    private final InmuebleRepositorio inmuebles;
    private final PublicacionRepositorio publicaciones;

    public InmuebleServicio(InmuebleRepositorio inmuebles, PublicacionRepositorio publicaciones) {
        this.inmuebles = inmuebles;
        this.publicaciones = publicaciones;
    }

    // ------------------------------------------------------------ CU-02.1 Registrar

    public Inmueble registrar(Usuario propietario, String direccion, String ciudad, String tipo, BigDecimal precio,
            BigDecimal area, String descripcion, BigDecimal precioMinimo, List<Fotografia> fotos) {
        exigirPropietario(propietario);
        validarDatos(direccion, ciudad, tipo, precio, area, descripcion);
        if (fotos == null || fotos.size() < MIN_FOTOS) {
            throw new NegocioException("Debes cargar al menos " + MIN_FOTOS + " fotografías.");
        }
        for (Fotografia f : fotos) {
            validarFoto(f);
        }
        Inmueble i = new Inmueble();
        i.setIdPropietario(propietario.getId());
        aplicar(i, direccion, ciudad, tipo, precio, area, descripcion, precioMinimo);
        i.setEstado(EstadoInmueble.DISPONIBLE);
        inmuebles.guardar(i);
        int orden = 1;
        for (Fotografia f : fotos) {
            f.setId(Secuencia.siguiente());
            f.setOrden(orden++);
            f.setUrl("/api/inmuebles/foto?id=" + f.getId());
            i.getFotografias().add(f);
        }
        Publicacion p = new Publicacion();
        p.setIdInmueble(i.getId());
        p.setEstado(EstadoPublicacion.PENDIENTE);
        p.setFechaPublicacion(LocalDateTime.now());
        publicaciones.guardar(p);
        Contexto c = Contexto.get();
        c.usuarios.registrarBitacora(propietario.getId(), "REGISTRAR_INMUEBLE", "Inmueble #" + i.getId() + " en " + i.getCiudad());
        c.usuarios.notificarAdministradores("Nueva publicación por validar: " + i.getDireccion() + ", " + i.getCiudad(),
                "/api/publicaciones/pendientes");
        return i;
    }

    public static void validarFoto(Fotografia f) {
        String tipo = f.getContentType() == null ? "" : f.getContentType().toLowerCase(Locale.ROOT);
        if (!tipo.equals("image/jpeg") && !tipo.equals("image/png")) {
            throw new NegocioException("La imagen '" + f.getNombre() + "' debe ser JPG o PNG.");
        }
        if (f.getDatos() == null || f.getDatos().length == 0) {
            throw new NegocioException("La imagen '" + f.getNombre() + "' está vacía.");
        }
        if (f.getDatos().length > MAX_BYTES_FOTO) {
            throw new NegocioException("La imagen '" + f.getNombre() + "' supera 5 MB.");
        }
    }

    // ------------------------------------------------------------ CU-02.2 Editar / CU-02.3 Retirar

    public void editar(Usuario u, long idInmueble, String direccion, String ciudad, String tipo, BigDecimal precio,
            BigDecimal area, String descripcion, BigDecimal precioMinimo) {
        Inmueble i = propio(u, idInmueble);
        if (Contexto.get().negociaciones.tieneOfertaAceptada(i.getId())) {
            throw new NegocioException("No se puede editar un inmueble con una negociación aceptada.");
        }
        if (i.getEstado() == EstadoInmueble.VENDIDO || i.getEstado() == EstadoInmueble.RETIRADO) {
            throw new NegocioException("El inmueble está " + Util.etiqueta(i.getEstado()).toLowerCase(Locale.ROOT) + " y no se puede editar.");
        }
        validarDatos(direccion, ciudad, tipo, precio, area, descripcion);
        BigDecimal anterior = i.getPrecio();
        aplicar(i, direccion, ciudad, tipo, precio, area, descripcion, precioMinimo);
        Contexto c = Contexto.get();
        c.usuarios.registrarBitacora(u.getId(), "EDITAR_INMUEBLE", "Inmueble #" + i.getId());
        if (anterior.compareTo(i.getPrecio()) != 0) {
            for (Long idComprador : c.negociaciones.interesados(i.getId())) {
                c.usuarios.notificar(idComprador, "Cambió el precio de " + i.getDireccion() + ": " + Util.dinero(anterior)
                        + " → " + Util.dinero(i.getPrecio()), "/api/inmuebles/detalle?id=" + i.getId());
            }
        }
    }

    public void retirar(Usuario u, long idInmueble) {
        Inmueble i = propio(u, idInmueble);
        Contexto c = Contexto.get();
        if (c.negociaciones.tieneOfertaAceptada(i.getId())) {
            throw new NegocioException("No se puede retirar un inmueble con una negociación aceptada.");
        }
        if (i.getEstado() == EstadoInmueble.RETIRADO || i.getEstado() == EstadoInmueble.VENDIDO) {
            throw new NegocioException("El inmueble ya no está en oferta.");
        }
        c.negociaciones.cancelarPorRetiro(i.getId());
        i.setEstado(EstadoInmueble.RETIRADO);
        Publicacion p = publicaciones.porInmueble(i.getId());
        if (p != null) {
            p.setEstado(EstadoPublicacion.RETIRADA);
        }
        c.usuarios.registrarBitacora(u.getId(), "RETIRAR_INMUEBLE", "Inmueble #" + i.getId());
    }

    // ------------------------------------------------------------ CU-02.4 Buscar / CU-02.5 Detalle

    public Pagina<Inmueble> buscar(FiltroInmueble f) {
        if (f.precioMin != null && f.precioMax != null && f.precioMin.compareTo(f.precioMax) >= 0) {
            throw new NegocioException("El precio mínimo debe ser menor que el máximo.");
        }
        if (f.areaMin != null && f.areaMax != null && f.areaMin.compareTo(f.areaMax) > 0) {
            throw new NegocioException("El área mínima no puede ser mayor que la máxima.");
        }
        String palabra = f.palabra == null ? "" : f.palabra.trim().toLowerCase(Locale.ROOT);
        List<Inmueble> resultado = new ArrayList<>();
        for (Inmueble i : inmuebles.todos()) {
            if (!estaPublicado(i)) {
                continue;
            }
            if (!palabra.isEmpty() && !(i.getDireccion() + " " + i.getCiudad() + " " + i.getTipo() + " " + i.getDescripcion())
                    .toLowerCase(Locale.ROOT).contains(palabra)) {
                continue;
            }
            if (!Util.vacio(f.ciudad) && !i.getCiudad().equalsIgnoreCase(f.ciudad)) {
                continue;
            }
            if (!Util.vacio(f.tipo) && !i.getTipo().equalsIgnoreCase(f.tipo)) {
                continue;
            }
            if (f.precioMin != null && i.getPrecio().compareTo(f.precioMin) < 0) {
                continue;
            }
            if (f.precioMax != null && i.getPrecio().compareTo(f.precioMax) > 0) {
                continue;
            }
            if (f.areaMin != null && i.getArea().compareTo(f.areaMin) < 0) {
                continue;
            }
            if (f.areaMax != null && i.getArea().compareTo(f.areaMax) > 0) {
                continue;
            }
            resultado.add(i);
        }
        return new Pagina<>(resultado, f.pagina, TAMANO_PAGINA);
    }

    /** Visible en búsquedas: publicación aprobada y el inmueble sigue a la venta. */
    public boolean estaPublicado(Inmueble i) {
        Publicacion p = publicaciones.porInmueble(i.getId());
        return p != null && p.getEstado() == EstadoPublicacion.APROBADA
                && (i.getEstado() == EstadoInmueble.DISPONIBLE || i.getEstado() == EstadoInmueble.EN_NEGOCIACION);
    }

    /** Consulta el detalle respetando la visibilidad por rol; cuenta la visita de quien no es el propietario. */
    public Inmueble detalle(long id, Usuario viewer) {
        Inmueble i = inmuebles.buscar(id);
        if (i == null) {
            throw new NegocioException("El inmueble no existe.");
        }
        boolean privilegiado = viewer.getRol() == Rol.ADMINISTRADOR || viewer.getId() == i.getIdPropietario()
                || (i.getIdAsesor() != null && i.getIdAsesor() == viewer.getId());
        if (!estaPublicado(i) && !privilegiado) {
            throw new NegocioException("El inmueble no está disponible.");
        }
        if (viewer.getId() != i.getIdPropietario()) {
            synchronized (i) {
                i.setVisitas(i.getVisitas() + 1);
            }
        }
        return i;
    }

    public Inmueble buscar(long id) {
        return inmuebles.buscar(id);
    }

    public Inmueble requerir(long id) {
        Inmueble i = inmuebles.buscar(id);
        if (i == null) {
            throw new NegocioException("El inmueble no existe.");
        }
        return i;
    }

    public Publicacion publicacionDe(long idInmueble) {
        return publicaciones.porInmueble(idInmueble);
    }

    public List<Inmueble> misInmuebles(long idPropietario) {
        return inmuebles.porPropietario(idPropietario);
    }

    public List<Inmueble> todos() {
        return inmuebles.todos();
    }

    public Fotografia fotografia(long idFoto) {
        for (Inmueble i : inmuebles.todos()) {
            for (Fotografia f : i.getFotografias()) {
                if (f.getId() == idFoto) {
                    return f;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------ CU-08.4 Asignar asesor

    public void asignarAsesor(Usuario admin, long idInmueble, long idAsesor) {
        Inmueble i = requerir(idInmueble);
        Usuario asesor = Contexto.get().usuarios.buscar(idAsesor);
        if (asesor == null || asesor.getRol() != Rol.ASESOR || asesor.getEstado() != EstadoUsuario.ACTIVO) {
            throw new NegocioException("Solo se pueden asignar asesores activos.");
        }
        i.setIdAsesor(asesor.getId());
        Contexto c = Contexto.get();
        c.usuarios.registrarBitacora(admin.getId(), "ASIGNAR_ASESOR", "Inmueble #" + i.getId() + " -> " + asesor.getNombre());
        c.usuarios.notificar(asesor.getId(), "Se te asignó el inmueble " + i.getDireccion() + ", " + i.getCiudad(),
                "/api/inmuebles/detalle?id=" + i.getId());
    }

    // ------------------------------------------------------------ internos

    private Inmueble propio(Usuario u, long idInmueble) {
        exigirPropietario(u);
        Inmueble i = requerir(idInmueble);
        if (i.getIdPropietario() != u.getId()) {
            throw new NegocioException("Solo puedes modificar tus propios inmuebles.");
        }
        return i;
    }

    private void exigirPropietario(Usuario u) {
        if (u.getRol() != Rol.PROPIETARIO) {
            throw new NegocioException("Solo un propietario puede gestionar inmuebles.");
        }
    }

    private void validarDatos(String direccion, String ciudad, String tipo, BigDecimal precio, BigDecimal area, String descripcion) {
        if (Util.vacio(direccion) || Util.vacio(ciudad) || Util.vacio(tipo) || Util.vacio(descripcion) || precio == null || area == null) {
            throw new NegocioException("Dirección, ciudad, tipo, precio, área y descripción son obligatorios.");
        }
        if (precio.signum() <= 0 || area.signum() <= 0) {
            throw new NegocioException("El precio y el área deben ser mayores que cero.");
        }
        if (direccion.trim().length() > 150) {
            throw new NegocioException("La dirección admite máximo 150 caracteres.");
        }
        if (area.compareTo(new BigDecimal("99999999")) > 0) {
            throw new NegocioException("El área es demasiado grande.");
        }
        List<String> ciudades = Contexto.get().usuarios.listaParametro("ciudades");
        List<String> tipos = Contexto.get().usuarios.listaParametro("tipos_inmueble");
        if (!ciudades.isEmpty() && !contiene(ciudades, ciudad)) {
            throw new NegocioException("La ciudad no está en el catálogo (" + String.join(", ", ciudades) + ").");
        }
        if (!tipos.isEmpty() && !contiene(tipos, tipo)) {
            throw new NegocioException("El tipo de inmueble no está en el catálogo (" + String.join(", ", tipos) + ").");
        }
    }

    private boolean contiene(List<String> lista, String valor) {
        for (String s : lista) {
            if (s.equalsIgnoreCase(valor.trim())) {
                return true;
            }
        }
        return false;
    }

    private String canonico(List<String> lista, String valor) {
        for (String s : lista) {
            if (s.equalsIgnoreCase(valor.trim())) {
                return s;
            }
        }
        return valor.trim();
    }

    private void aplicar(Inmueble i, String direccion, String ciudad, String tipo, BigDecimal precio, BigDecimal area,
            String descripcion, BigDecimal precioMinimo) {
        i.setDireccion(direccion.trim());
        i.setCiudad(canonico(Contexto.get().usuarios.listaParametro("ciudades"), ciudad));
        i.setTipo(canonico(Contexto.get().usuarios.listaParametro("tipos_inmueble"), tipo));
        i.setPrecio(precio);
        i.setArea(area);
        i.setDescripcion(descripcion.trim());
        BigDecimal minimo = precioMinimo;
        if (minimo == null) {
            minimo = precio.multiply(new BigDecimal("0.70")).setScale(0, java.math.RoundingMode.HALF_UP);
        }
        if (minimo.signum() <= 0 || minimo.compareTo(precio) > 0) {
            throw new NegocioException("La oferta mínima debe ser mayor que cero y no superar el precio.");
        }
        i.setPrecioMinimo(minimo);
    }
}
