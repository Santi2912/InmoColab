package com.inmocolab.comun.bd;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/** Libera las conexiones y los hilos de los drivers al detener o redesplegar la aplicación. */
@WebListener
public class CicloVida implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        // Las bases de datos se inicializan de forma perezosa en la primera petición (ver Contexto).
    }

    @Override
    public void contextDestroyed(ServletContextEvent evento) {
        Persistencia.cerrar();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            if (d.getClass().getClassLoader() == getClass().getClassLoader()) {
                try {
                    DriverManager.deregisterDriver(d);
                } catch (java.sql.SQLException e) {
                    // no se puede hacer más al apagar
                }
            }
        }
    }
}
