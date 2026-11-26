package com.inmocolab.comun.bd;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

/** Relaciona una tabla con los objetos en memoria: cómo escribirla y cómo cargarla. */
final class Mapeo {

    /** Convierte una fila leída de la base de datos en objetos del dominio. */
    interface Lector {
        void leer(ResultSet rs) throws SQLException;
    }

    final String tabla;
    /** Nombres de columna; las primeras {@code nClaves} forman la llave primaria. */
    final String[] columnas;
    final int nClaves;
    final Supplier<List<Fila>> actuales;
    final Lector lector;

    Mapeo(String tabla, String[] columnas, int nClaves, Supplier<List<Fila>> actuales, Lector lector) {
        this.tabla = tabla;
        this.columnas = columnas;
        this.nClaves = nClaves;
        this.actuales = actuales;
        this.lector = lector;
    }

    String sqlUpsert() {
        StringBuilder cols = new StringBuilder();
        StringBuilder marcas = new StringBuilder();
        StringBuilder claves = new StringBuilder();
        StringBuilder sets = new StringBuilder();
        for (int i = 0; i < columnas.length; i++) {
            cols.append(i > 0 ? ", " : "").append(columnas[i]);
            marcas.append(i > 0 ? ", " : "").append('?');
            if (i < nClaves) {
                claves.append(i > 0 ? ", " : "").append(columnas[i]);
            } else {
                sets.append(sets.length() > 0 ? ", " : "").append(columnas[i]).append(" = EXCLUDED.").append(columnas[i]);
            }
        }
        String conflicto = sets.length() == 0 ? " DO NOTHING" : " DO UPDATE SET " + sets;
        return "INSERT INTO " + tabla + " (" + cols + ") VALUES (" + marcas + ") ON CONFLICT (" + claves + ")" + conflicto;
    }

    String sqlBorrar() {
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < nClaves; i++) {
            where.append(i > 0 ? " AND " : "").append(columnas[i]).append(" = ?");
        }
        return "DELETE FROM " + tabla + " WHERE " + where;
    }

    String sqlLeer() {
        StringBuilder orden = new StringBuilder();
        for (int i = 0; i < nClaves; i++) {
            orden.append(i > 0 ? ", " : "").append(columnas[i]);
        }
        return "SELECT " + String.join(", ", columnas) + " FROM " + tabla + " ORDER BY " + orden;
    }
}
