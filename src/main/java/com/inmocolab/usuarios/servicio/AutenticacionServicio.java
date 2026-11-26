package com.inmocolab.usuarios.servicio;

import com.inmocolab.comun.NegocioException;
import com.inmocolab.comun.Util;
import com.inmocolab.usuarios.modelo.EstadoUsuario;
import com.inmocolab.usuarios.modelo.Usuario;
import com.inmocolab.usuarios.persistencia.BitacoraRepositorio;
import com.inmocolab.usuarios.persistencia.UsuarioRepositorio;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Autenticación: contraseñas cifradas con PBKDF2 y bloqueo tras 3 intentos fallidos. */
public class AutenticacionServicio {

    private static final int ITERACIONES = 60000;
    private static final int MAX_INTENTOS = 3;
    private static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepositorio usuarios;
    private final BitacoraRepositorio bitacora;

    public AutenticacionServicio(UsuarioRepositorio usuarios, BitacoraRepositorio bitacora) {
        this.usuarios = usuarios;
        this.bitacora = bitacora;
    }

    public Usuario iniciarSesion(String correo, String clave) {
        if (Util.vacio(correo) || Util.vacio(clave)) {
            throw new NegocioException("Ingresa tu correo y tu contraseña.");
        }
        Usuario u = usuarios.porCorreo(correo.trim());
        if (u == null) {
            throw new NegocioException("Correo o contraseña incorrectos.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (u.getBloqueadoHasta() != null && ahora.isBefore(u.getBloqueadoHasta())) {
            throw new NegocioException("Cuenta bloqueada temporalmente por intentos fallidos. Intenta de nuevo después de las "
                    + u.getBloqueadoHasta().format(DateTimeFormatter.ofPattern("HH:mm")) + ".");
        }
        if (!verificar(clave, u.getContrasenaHash())) {
            u.setIntentosFallidos(u.getIntentosFallidos() + 1);
            if (u.getIntentosFallidos() >= MAX_INTENTOS) {
                u.setIntentosFallidos(0);
                u.setBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO));
                bitacora.registrar(u.getId(), "BLOQUEO_TEMPORAL", "3 intentos fallidos de inicio de sesión");
                throw new NegocioException("Superaste 3 intentos. La cuenta se bloqueó " + MINUTOS_BLOQUEO + " minutos.");
            }
            throw new NegocioException("Correo o contraseña incorrectos.");
        }
        if (u.getEstado() == EstadoUsuario.SUSPENDIDO) {
            throw new NegocioException("Tu cuenta está suspendida. Contacta al administrador.");
        }
        if (u.getEstado() == EstadoUsuario.BLOQUEADO) {
            throw new NegocioException("Tu cuenta está bloqueada. Contacta al administrador.");
        }
        if (u.getEstado() == EstadoUsuario.PENDIENTE) {
            throw new NegocioException("Debes verificar tu correo antes de ingresar.");
        }
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        bitacora.registrar(u.getId(), "INICIO_SESION", "Inicio de sesión como " + u.getRol());
        return u;
    }

    public static void validarPolitica(String clave) {
        if (clave == null || clave.length() < 8 || !clave.matches(".*[A-Z].*") || !clave.matches(".*\\d.*")) {
            throw new NegocioException("La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número.");
        }
    }

    public static String cifrar(String clave) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return ITERACIONES + ":" + Base64.getEncoder().encodeToString(sal) + ":"
                + Base64.getEncoder().encodeToString(derivar(clave, sal, ITERACIONES));
    }

    public static boolean verificar(String clave, String almacenado) {
        String[] p = almacenado.split(":");
        byte[] esperado = Base64.getDecoder().decode(p[2]);
        byte[] calculado = derivar(clave, Base64.getDecoder().decode(p[1]), Integer.parseInt(p[0]));
        return java.security.MessageDigest.isEqual(esperado, calculado);
    }

    private static byte[] derivar(String clave, byte[] sal, int iteraciones) {
        try {
            KeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo cifrar la contraseña", e);
        }
    }
}
