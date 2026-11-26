package com.inmocolab.comun.bd;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuración y conexión JDBC pura (sin pool ni framework).
 * Lee {@code inmocolab.properties}; cualquier valor se puede sobrescribir con {@code -Dinmocolab.<clave>=...}.
 */
final class ConexionBD {

    private static final Logger LOG = Logger.getLogger(ConexionBD.class.getName());

    private final Properties config = new Properties();

    ConexionBD() {
        try (InputStream in = ConexionBD.class.getResourceAsStream("/inmocolab.properties")) {
            if (in != null) {
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    config.load(r);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo leer inmocolab.properties; se usan valores por defecto", e);
        }
    }

    String valor(String clave, String defecto) {
        String v = System.getProperty("inmocolab." + clave);
        return v != null ? v : config.getProperty(clave, defecto);
    }

    boolean usaPostgres() {
        return "postgresql".equalsIgnoreCase(valor("persistencia", "postgresql"));
    }

    String url() {
        return valor("db.url", "jdbc:postgresql://localhost:5432/inmocolab");
    }

    Connection abrir() throws SQLException {
        return abrir(url());
    }

    private Connection abrir(String url) throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Falta el driver JDBC de PostgreSQL (org.postgresql:postgresql).", e);
        }
        return DriverManager.getConnection(url, valor("db.usuario", "postgres"), valor("db.clave", "postgres"));
    }

    /** Crea la base de datos si no existe y ejecuta el script de tablas (idempotente). */
    void inicializar() throws SQLException, IOException {
        String url = url();
        int barra = url.lastIndexOf('/');
        String nombreBd = url.substring(barra + 1);
        if (!nombreBd.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("Nombre de base de datos no válido: " + nombreBd);
        }
        try (Connection admin = abrir(url.substring(0, barra + 1) + "postgres");
                PreparedStatement ps = admin.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            ps.setString(1, nombreBd);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    try (Statement st = admin.createStatement()) {
                        st.execute("CREATE DATABASE " + nombreBd + " ENCODING 'UTF8' TEMPLATE template0");
                        LOG.info("Base de datos creada: " + nombreBd);
                    }
                }
            }
        }
        try (Connection cn = abrir(); Statement st = cn.createStatement()) {
            for (String sentencia : leerScript().split(";\\s*(\\r?\\n|$)")) {
                String s = sentencia.trim();
                if (!s.isEmpty()) {
                    st.execute(s);
                }
            }
        }
    }

    private String leerScript() throws IOException {
        try (InputStream in = ConexionBD.class.getResourceAsStream("/bd/inmocolab.sql")) {
            if (in == null) {
                throw new IOException("No se encontró /bd/inmocolab.sql en el classpath.");
            }
            StringBuilder sb = new StringBuilder();
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) != -1) {
                    sb.append(buf, 0, n);
                }
            }
            // quita comentarios de línea para que no estorben al separar sentencias
            return sb.toString().replaceAll("(?m)--.*$", "");
        }
    }
}
