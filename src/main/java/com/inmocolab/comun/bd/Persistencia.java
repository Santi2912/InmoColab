package com.inmocolab.comun.bd;

import com.inmocolab.comun.Contexto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Guarda en PostgreSQL (JDBC puro) lo que los servicios hacen en memoria.
 *
 * Funcionamiento: al arrancar, si la base ya tiene datos, se cargan a los repositorios; si está vacía, se siembran
 * los datos de ejemplo. Al terminar cada petición, {@link #sincronizar()} compara las filas actuales con la última
 * copia guardada y ejecuta solo los INSERT/UPDATE/DELETE necesarios, todo en una transacción.
 * Comentarios y calificaciones (MS-Comentarios) van a MongoDB, una base NoSQL de documentos.
 * Si PostgreSQL no está disponible, la aplicación sigue funcionando solo en memoria;
 * si es MongoDB el que no está, solo los comentarios y calificaciones quedan en memoria.
 *
 * Patrones: Unit of Work (acumula los cambios de la petición y los escribe juntos en una transacción),
 * Data Mapper (Mapeos separa los objetos del dominio de las tablas) y Identity Map (los repositorios).
 */
public final class Persistencia {

    private static final Logger LOG = Logger.getLogger(Persistencia.class.getName());

    private static ConexionBD conexion;
    private static List<Mapeo> mapeos;
    private static boolean activa;
    private static PersistenciaMongo mongo;
    private static String descripcion = "Datos en memoria";
    /** tabla -> (llave -> valores) tal como quedaron en la base de datos tras la última sincronización. */
    private static final Map<String, Map<String, Object[]>> copia = new HashMap<>();

    private Persistencia() {
    }

    /** @return true si cargó datos existentes de la base (no hay que sembrar los de ejemplo). */
    public static synchronized boolean iniciar(Contexto c) {
        ConexionBD cx = new ConexionBD();
        if (!cx.usaPostgres()) {
            descripcion = "Datos en memoria (persistencia desactivada)";
            return false;
        }
        try {
            cx.inicializar();
            List<Mapeo> m = new Mapeos(c).todos();
            boolean hayDatos;
            try (Connection cn = cx.abrir(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuario")) {
                rs.next();
                hayDatos = rs.getInt(1) > 0;
            }
            if (hayDatos) {
                try (Connection cn = cx.abrir()) {
                    for (Mapeo t : m) {
                        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(t.sqlLeer())) {
                            while (rs.next()) {
                                t.lector.leer(rs);
                            }
                        }
                    }
                }
            }
            copia.clear();
            for (Mapeo t : m) {
                copia.put(t.tabla, new HashMap<String, Object[]>());
            }
            conexion = cx;
            mapeos = m;
            activa = true;
            descripcion = "PostgreSQL (" + cx.url().replace("jdbc:postgresql://", "") + ")";
            conectarMongo(cx, c, hayDatos);
            if (hayDatos) {
                guardarCopia(actuales());
                LOG.info("Datos cargados desde PostgreSQL.");
                return true;
            }
            return false;
        } catch (Exception e) {
            activa = false;
            descripcion = "Datos en memoria (no se pudo usar PostgreSQL: " + e.getMessage() + ")";
            LOG.log(Level.SEVERE, "No se pudo usar PostgreSQL; la aplicación seguirá solo en memoria", e);
            return false;
        }
    }

    private static void conectarMongo(ConexionBD cx, Contexto c, boolean hayDatosSql) {
        mongo = null;
        if (!"true".equalsIgnoreCase(cx.valor("mongo.activo", "true"))) {
            descripcion += " · comentarios en memoria (MongoDB desactivado)";
            return;
        }
        String uri = cx.valor("mongo.uri", "mongodb://localhost:27017/?serverSelectionTimeoutMS=3000&connectTimeoutMS=3000");
        String base = cx.valor("mongo.base", "inmocolab");
        PersistenciaMongo pm = new PersistenciaMongo(c);
        if (!pm.conectar(uri, base)) {
            descripcion += " · comentarios solo en memoria (MongoDB no disponible)";
            return;
        }
        if (hayDatosSql) {
            pm.cargar();
            pm.cebar();
        } else {
            // La base relacional está vacía: se siembran datos nuevos, así que se descartan documentos viejos.
            pm.vaciar();
        }
        mongo = pm;
        descripcion += " + MongoDB (" + base + ")";
    }

    /** Cierra el cliente de MongoDB (lo llama {@link CicloVida} al detener la aplicación). */
    static synchronized void cerrar() {
        if (mongo != null) {
            mongo.cerrar();
            mongo = null;
        }
        activa = false;
    }

    public static boolean activa() {
        return activa;
    }

    public static String descripcion() {
        return descripcion;
    }

    private static List<List<Fila>> actuales() {
        List<List<Fila>> todas = new ArrayList<>();
        for (Mapeo m : mapeos) {
            todas.add(m.actuales.get());
        }
        return todas;
    }

    private static void guardarCopia(List<List<Fila>> todas) {
        copia.clear();
        for (int i = 0; i < mapeos.size(); i++) {
            Map<String, Object[]> filas = new HashMap<>();
            for (Fila f : todas.get(i)) {
                filas.put(f.clave, f.valores);
            }
            copia.put(mapeos.get(i).tabla, filas);
        }
    }

    /** Escribe en PostgreSQL los cambios hechos desde la última sincronización. */
    public static void sincronizar() {
        if (!activa) {
            return;
        }
        synchronized (Persistencia.class) {
            try (Connection cn = conexion.abrir()) {
                cn.setAutoCommit(false);
                try {
                    List<List<Fila>> todas = actuales();
                    int n = mapeos.size();
                    // 1) borrados, de los hijos hacia los padres
                    for (int i = n - 1; i >= 0; i--) {
                        Mapeo m = mapeos.get(i);
                        Map<String, Object[]> viejas = copia.get(m.tabla);
                        Set<String> vivas = new HashSet<>();
                        for (Fila f : todas.get(i)) {
                            vivas.add(f.clave);
                        }
                        try (PreparedStatement ps = cn.prepareStatement(m.sqlBorrar())) {
                            int pendientes = 0;
                            for (Map.Entry<String, Object[]> e : viejas.entrySet()) {
                                if (!vivas.contains(e.getKey())) {
                                    for (int k = 0; k < m.nClaves; k++) {
                                        ps.setObject(k + 1, e.getValue()[k]);
                                    }
                                    ps.addBatch();
                                    pendientes++;
                                }
                            }
                            if (pendientes > 0) {
                                ps.executeBatch();
                            }
                        }
                    }
                    // 2) altas y modificaciones, de los padres hacia los hijos
                    for (int i = 0; i < n; i++) {
                        Mapeo m = mapeos.get(i);
                        Map<String, Object[]> viejas = copia.get(m.tabla);
                        try (PreparedStatement ps = cn.prepareStatement(m.sqlUpsert())) {
                            int pendientes = 0;
                            for (Fila f : todas.get(i)) {
                                Object[] anterior = viejas.get(f.clave);
                                if (anterior == null || !Arrays.deepEquals(anterior, f.valores)) {
                                    for (int k = 0; k < f.valores.length; k++) {
                                        ps.setObject(k + 1, f.valores[k]);
                                    }
                                    ps.addBatch();
                                    pendientes++;
                                }
                            }
                            if (pendientes > 0) {
                                ps.executeBatch();
                            }
                        }
                    }
                    cn.commit();
                    guardarCopia(todas);
                } catch (SQLException | RuntimeException e) {
                    cn.rollback();
                    throw e;
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "No se pudo sincronizar con PostgreSQL (se reintentará en la próxima petición)", e);
            }
            if (mongo != null) {
                try {
                    mongo.sincronizar();
                } catch (RuntimeException e) {
                    LOG.log(Level.SEVERE, "No se pudo sincronizar con MongoDB (se reintentará en la próxima petición)", e);
                }
            }
        }
    }
}
