package com.inmocolab.comun;

import com.inmocolab.compracolaborativa.modelo.EstadoAporte;
import com.inmocolab.compracolaborativa.modelo.GrupoCompra;
import com.inmocolab.compracolaborativa.modelo.Participacion;
import com.inmocolab.inmuebles.modelo.EstadoInmueble;
import com.inmocolab.inmuebles.modelo.EstadoPublicacion;
import com.inmocolab.inmuebles.modelo.Fotografia;
import com.inmocolab.inmuebles.modelo.Inmueble;
import com.inmocolab.inmuebles.modelo.Publicacion;
import com.inmocolab.pqr.modelo.TipoPqr;
import com.inmocolab.usuarios.modelo.Rol;
import com.inmocolab.usuarios.modelo.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Datos de ejemplo para que el prototipo funcione sin instalar una base de datos. */
final class Datos {

    static final String CLAVE_DEMO = "Demo1234";
    static final String CLAVE_ADMIN = "Admin123";

    private static final String[] CIUDADES = {"Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena"};
    private static final String[] TIPOS = {"Casa", "Apartamento", "Local", "Lote", "Oficina"};
    private static final String[] BARRIOS = {"Chapinero", "El Poblado", "Granada", "Alto Prado", "Bocagrande", "Usaquén", "Laureles",
        "Ciudad Jardín", "Riomar", "Getsemaní", "Cedritos", "Envigado", "San Fernando", "Villa Country"};

    private Datos() {
    }

    static void cargar(Contexto c) {
        parametros(c);
        Usuario admin = c.usuarios.crearActivo(Rol.ADMINISTRADOR, "Laura Administradora", "10000001", "admin@inmocolab.com", "3000000001", CLAVE_ADMIN);
        Usuario prop1 = c.usuarios.crearActivo(Rol.PROPIETARIO, "Carlos Propietario", "10000002", "propietario@inmocolab.com", "3000000002", CLAVE_DEMO);
        Usuario prop2 = c.usuarios.crearActivo(Rol.PROPIETARIO, "Marta Rojas", "10000003", "propietario2@inmocolab.com", "3000000003", CLAVE_DEMO);
        Usuario comp1 = c.usuarios.crearActivo(Rol.COMPRADOR, "Ana Compradora", "10000004", "comprador@inmocolab.com", "3000000004", CLAVE_DEMO);
        Usuario comp2 = c.usuarios.crearActivo(Rol.COMPRADOR, "Luis Comprador", "10000005", "comprador2@inmocolab.com", "3000000005", CLAVE_DEMO);
        Usuario comp3 = c.usuarios.crearActivo(Rol.COMPRADOR, "Sofía Gómez", "10000006", "comprador3@inmocolab.com", "3000000006", CLAVE_DEMO);
        Usuario asesor = c.usuarios.crearActivo(Rol.ASESOR, "Pedro Asesor", "10000007", "asesor@inmocolab.com", "3000000007", CLAVE_DEMO);

        for (int k = 0; k < 14; k++) {
            Inmueble i = inmueble(c, k % 3 == 0 ? prop2 : prop1, k);
            publicar(c, i, admin, EstadoPublicacion.APROBADA);
        }
        Inmueble pendiente = inmueble(c, prop1, 14);
        publicar(c, pendiente, null, EstadoPublicacion.PENDIENTE);

        // Grupo de compra con aportes confirmados y uno pendiente
        Inmueble objetivo = c.inmuebles.buscar(2);
        GrupoCompra g = c.grupos.crear(comp1, objetivo.getId(), objetivo.getPrecio().multiply(new BigDecimal("0.80")).setScale(0), LocalDate.now().plusDays(45));
        c.grupos.unirse(comp2, g.getId());
        Participacion p1 = c.grupos.participacionActiva(g, comp1.getId());
        Participacion p2 = c.grupos.participacionActiva(g, comp2.getId());
        c.aportes.sembrar(g, p1, g.getMontoObjetivo().multiply(new BigDecimal("0.40")).setScale(0), EstadoAporte.CONFIRMADO);
        c.aportes.sembrar(g, p2, g.getMontoObjetivo().multiply(new BigDecimal("0.20")).setScale(0), EstadoAporte.CONFIRMADO);
        c.aportes.sembrar(g, p2, g.getMontoObjetivo().multiply(new BigDecimal("0.10")).setScale(0), EstadoAporte.PENDIENTE);
        c.grupos.recalcular(g);

        // Interés y oferta individual en otro inmueble
        Inmueble otro = c.inmuebles.buscar(3);
        c.negociaciones.manifestarInteres(comp3, otro.getId());
        c.ofertas.ofertar(comp3, otro.getId(), otro.getPrecio().multiply(new BigDecimal("0.90")).setScale(0), null, "Pago de contado");
        c.negociaciones.asignarAsesor(admin, c.negociaciones.vigente(otro.getId()).getId(), asesor.getId());

        // PQR y comentario
        c.pqr.radicar(comp1, TipoPqr.PETICION, "Consulta sobre verificación de aportes",
                "¿Cuánto tarda la verificación de un aporte con comprobante?", null, null, null);
        c.comentarios.publicar(comp2, 1, "¿El precio es negociable? Me interesa visitarlo.");

        // Limpia la bitácora y las notificaciones de arranque no es necesario: sirven como ejemplo.
    }

    private static void parametros(Contexto c) {
        c.usuarios.crearParametro("ciudades", "Bogotá,Medellín,Cali,Barranquilla,Cartagena", "Ciudades disponibles para publicar (separadas por coma)");
        c.usuarios.crearParametro("tipos_inmueble", "Casa,Apartamento,Local,Lote,Oficina", "Tipos de inmueble (separados por coma)");
        c.usuarios.crearParametro("plazo_pqr_dias", "15", "Días hábiles para responder una PQR");
        c.usuarios.crearParametro("vigencia_grupo_dias", "90", "Días máximos de vigencia de un grupo de compra");
        c.usuarios.crearParametro("palabras_prohibidas", "estafa,idiota,imbecil,estupido", "Palabras no permitidas en comentarios (separadas por coma)");
    }

    private static Inmueble inmueble(Contexto c, Usuario propietario, int k) {
        Inmueble i = new Inmueble();
        i.setIdPropietario(propietario.getId());
        i.setCiudad(CIUDADES[k % CIUDADES.length]);
        i.setTipo(TIPOS[(k + 1) % TIPOS.length]);
        i.setDireccion("Calle " + (10 + k * 3) + " # " + (20 + k) + "-" + (10 + k * 2) + ", " + BARRIOS[k % BARRIOS.length]);
        BigDecimal precio = new BigDecimal(180_000_000L + k * 45_000_000L);
        i.setPrecio(precio);
        i.setPrecioMinimo(precio.multiply(new BigDecimal("0.75")).setScale(0));
        i.setArea(new BigDecimal(45 + k * 12));
        i.setDescripcion(i.getTipo() + " en " + BARRIOS[k % BARRIOS.length] + " con excelente ubicación, cerca de transporte y comercio. "
                + "Documentos al día y listo para escriturar.");
        i.setEstado(EstadoInmueble.DISPONIBLE);
        i.setVisitas(k * 3);
        c.inmuebleRepo.guardar(i);
        for (int f = 1; f <= 3; f++) {
            Fotografia foto = new Fotografia();
            foto.setId(Secuencia.siguiente());
            foto.setNombre("foto" + f + ".png");
            foto.setContentType("image/png");
            foto.setOrden(f);
            foto.setUrl("/api/inmuebles/foto?id=" + foto.getId());
            i.getFotografias().add(foto);
        }
        return i;
    }

    private static void publicar(Contexto c, Inmueble i, Usuario admin, EstadoPublicacion estado) {
        Publicacion p = new Publicacion();
        p.setIdInmueble(i.getId());
        p.setEstado(estado);
        p.setFechaPublicacion(LocalDateTime.now().minusDays(5));
        if (admin != null) {
            p.setIdAdministrador(admin.getId());
        }
        c.publicacionRepo.guardar(p);
    }
}
