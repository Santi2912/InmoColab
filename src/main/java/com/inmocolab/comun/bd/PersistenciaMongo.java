package com.inmocolab.comun.bd;

import com.inmocolab.comentarios.modelo.Calificacion;
import com.inmocolab.comentarios.modelo.Comentario;
import com.inmocolab.comentarios.modelo.EstadoComentario;
import com.inmocolab.comentarios.modelo.ReporteComentario;
import com.inmocolab.comun.Contexto;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.WriteModel;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

/**
 * Base de datos NoSQL (MongoDB) del microservicio MS-Comentarios, como indica el diseño.
 * Guarda dos colecciones de documentos: {@code comentarios} (con sus reportes incrustados como arreglo)
 * y {@code calificaciones}. Usa la misma estrategia de sincronización por diferencias que la parte SQL.
 *
 * Patrón: Data Mapper para documentos (objeto del dominio <-> Document BSON).
 */
final class PersistenciaMongo {

    private static final Logger LOG = Logger.getLogger(PersistenciaMongo.class.getName());
    private static final String COMENTARIOS = "comentarios";
    private static final String CALIFICACIONES = "calificaciones";

    private final Contexto c;
    private MongoClient cliente;
    private MongoDatabase bd;
    /** colección -> (id -> documento) tal como quedó en MongoDB tras la última sincronización. */
    private final Map<String, Map<Long, Document>> copia = new HashMap<>();

    PersistenciaMongo(Contexto c) {
        this.c = c;
        copia.put(COMENTARIOS, new HashMap<Long, Document>());
        copia.put(CALIFICACIONES, new HashMap<Long, Document>());
    }

    /** @return true si hay conexión (hace un ping para fallar rápido si MongoDB no está encendido). */
    boolean conectar(String uri, String base) {
        try {
            cliente = MongoClients.create(uri);
            bd = cliente.getDatabase(base);
            bd.runCommand(new Document("ping", 1));
            return true;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "MongoDB no disponible: {0}", e.getMessage());
            cerrar();
            return false;
        }
    }

    void cerrar() {
        if (cliente != null) {
            cliente.close();
            cliente = null;
        }
    }

    /** Borra ambas colecciones (se usa cuando la base relacional está vacía y se siembran datos nuevos). */
    void vaciar() {
        bd.getCollection(COMENTARIOS).deleteMany(new Document());
        bd.getCollection(CALIFICACIONES).deleteMany(new Document());
        copia.get(COMENTARIOS).clear();
        copia.get(CALIFICACIONES).clear();
    }

    /** Carga las colecciones a los repositorios en memoria. */
    void cargar() {
        for (Document d : bd.getCollection(COMENTARIOS).find().sort(Sorts.ascending("_id"))) {
            Comentario cm = new Comentario();
            cm.setId(d.getLong("_id"));
            cm.setIdInmueble(d.getLong("idInmueble"));
            cm.setIdAutor(d.getLong("idAutor"));
            cm.setTexto(d.getString("texto"));
            cm.setFecha(aFecha(d.getDate("fecha")));
            cm.setEditado(Boolean.TRUE.equals(d.getBoolean("editado")));
            cm.setModerado(Boolean.TRUE.equals(d.getBoolean("moderado")));
            cm.setEstado(EstadoComentario.valueOf(d.getString("estado")));
            cm.setRespuesta(d.getString("respuesta"));
            cm.setFechaRespuesta(aFecha(d.getDate("fechaRespuesta")));
            cm.setMotivoOcultar(d.getString("motivoOcultar"));
            List<Document> reportes = d.getList("reportes", Document.class);
            if (reportes != null) {
                for (Document r : reportes) {
                    cm.getReportes().add(new ReporteComentario(r.getLong("idUsuario"), r.getString("motivo"), aFecha(r.getDate("fecha"))));
                }
            }
            c.comentarioRepo.restaurar(cm);
        }
        for (Document d : bd.getCollection(CALIFICACIONES).find().sort(Sorts.ascending("_id"))) {
            Calificacion x = new Calificacion();
            x.setId(d.getLong("_id"));
            x.setIdNegociacion(d.getLong("idNegociacion"));
            x.setIdComprador(d.getLong("idComprador"));
            x.setEstrellas(d.getInteger("estrellas"));
            x.setTexto(d.getString("texto"));
            x.setFecha(aFecha(d.getDate("fecha")));
            c.calificacionRepo.restaurar(x);
        }
    }

    /** Toma como "ya guardado" lo que hay hoy en memoria (justo después de cargar). */
    void cebar() {
        guardarCopia(documentosComentarios(), documentosCalificaciones());
    }

    /** Escribe en MongoDB los documentos nuevos, modificados o eliminados desde la última vez. */
    void sincronizar() {
        Map<Long, Document> comentarios = documentosComentarios();
        Map<Long, Document> calificaciones = documentosCalificaciones();
        escribir(bd.getCollection(COMENTARIOS), copia.get(COMENTARIOS), comentarios);
        escribir(bd.getCollection(CALIFICACIONES), copia.get(CALIFICACIONES), calificaciones);
        guardarCopia(comentarios, calificaciones);
    }

    private void escribir(MongoCollection<Document> coleccion, Map<Long, Document> viejos, Map<Long, Document> actuales) {
        List<WriteModel<Document>> operaciones = new ArrayList<>();
        for (Map.Entry<Long, Document> e : actuales.entrySet()) {
            Document anterior = viejos.get(e.getKey());
            if (anterior == null || !anterior.equals(e.getValue())) {
                operaciones.add(new ReplaceOneModel<>(Filters.eq("_id", e.getKey()), e.getValue(), new ReplaceOptions().upsert(true)));
            }
        }
        Set<Long> vivos = new HashSet<>(actuales.keySet());
        for (Long id : viejos.keySet()) {
            if (!vivos.contains(id)) {
                operaciones.add(new DeleteOneModel<Document>(Filters.eq("_id", id)));
            }
        }
        if (!operaciones.isEmpty()) {
            coleccion.bulkWrite(operaciones);
        }
    }

    private void guardarCopia(Map<Long, Document> comentarios, Map<Long, Document> calificaciones) {
        copia.put(COMENTARIOS, comentarios);
        copia.put(CALIFICACIONES, calificaciones);
    }

    private Map<Long, Document> documentosComentarios() {
        Map<Long, Document> docs = new HashMap<>();
        for (Comentario cm : c.comentarioRepo.todos()) {
            List<Document> reportes = new ArrayList<>();
            for (ReporteComentario r : cm.getReportes()) {
                reportes.add(new Document("idUsuario", r.getIdUsuario()).append("motivo", r.getMotivo()).append("fecha", aDate(r.getFecha())));
            }
            docs.put(cm.getId(), new Document("_id", cm.getId())
                    .append("idInmueble", cm.getIdInmueble())
                    .append("idAutor", cm.getIdAutor())
                    .append("texto", cm.getTexto())
                    .append("fecha", aDate(cm.getFecha()))
                    .append("editado", cm.isEditado())
                    .append("moderado", cm.isModerado())
                    .append("estado", cm.getEstado().name())
                    .append("respuesta", cm.getRespuesta())
                    .append("fechaRespuesta", aDate(cm.getFechaRespuesta()))
                    .append("motivoOcultar", cm.getMotivoOcultar())
                    .append("reportes", reportes));
        }
        return docs;
    }

    private Map<Long, Document> documentosCalificaciones() {
        Map<Long, Document> docs = new HashMap<>();
        for (Calificacion x : c.calificacionRepo.todos()) {
            docs.put(x.getId(), new Document("_id", x.getId())
                    .append("idNegociacion", x.getIdNegociacion())
                    .append("idComprador", x.getIdComprador())
                    .append("estrellas", x.getEstrellas())
                    .append("texto", x.getTexto())
                    .append("fecha", aDate(x.getFecha())));
        }
        return docs;
    }

    private static Date aDate(LocalDateTime f) {
        return f == null ? null : Date.from(f.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDateTime aFecha(Date d) {
        return d == null ? null : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
