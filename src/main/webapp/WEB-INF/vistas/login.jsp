<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<div class="centro">
    <div class="card">
        <h1>Iniciar sesión</h1>
        <form method="post" action="<%= ctx %>/api/usuarios/login">
            <label for="correo">Correo electrónico</label>
            <input type="email" id="correo" name="correo" value="<%= Util.val(request, "correo") %>" required autofocus>
            <label for="clave">Contraseña</label>
            <input type="password" id="clave" name="clave" required>
            <div class="acciones">
                <button class="btn primario" type="submit">Ingresar</button>
                <a href="<%= ctx %>/api/usuarios/recuperar">¿Olvidaste tu contraseña?</a>
            </div>
        </form>
        <p class="suave">¿Aún no tienes cuenta? <a href="<%= ctx %>/api/usuarios/registro">Regístrate como propietario o comprador</a>.</p>
    </div>
    <div class="card demo">
        <h2>Cuentas de demostración</h2>
        <table>
            <tr><th>Rol</th><th>Correo</th><th>Contraseña</th></tr>
            <tr><td>Administrador</td><td>admin@inmocolab.com</td><td>Admin123</td></tr>
            <tr><td>Propietario</td><td>propietario@inmocolab.com</td><td>Demo1234</td></tr>
            <tr><td>Comprador</td><td>comprador@inmocolab.com</td><td>Demo1234</td></tr>
            <tr><td>Comprador 2 / 3</td><td>comprador2@… / comprador3@inmocolab.com</td><td>Demo1234</td></tr>
            <tr><td>Asesor</td><td>asesor@inmocolab.com</td><td>Demo1234</td></tr>
        </table>
    </div>
</div>
