<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<div class="centro ancho">
    <div class="card">
        <h1>Crear cuenta</h1>
        <form method="post" action="<%= ctx %>/api/usuarios/registro">
            <label>Quiero registrarme como</label>
            <div class="acciones">
                <label class="en-linea"><input type="radio" name="rol" value="COMPRADOR" <%= !"PROPIETARIO".equals(Util.val(request, "rol")) ? "checked" : "" %>> Comprador</label>
                <label class="en-linea"><input type="radio" name="rol" value="PROPIETARIO" <%= "PROPIETARIO".equals(Util.val(request, "rol")) ? "checked" : "" %>> Propietario</label>
            </div>
            <div class="fila">
                <div><label for="nombre">Nombre completo</label>
                    <input type="text" id="nombre" name="nombre" maxlength="100" value="<%= Util.val(request, "nombre") %>" required></div>
                <div><label for="documento">Documento de identidad</label>
                    <input type="text" id="documento" name="documento" maxlength="20" value="<%= Util.val(request, "documento") %>" required></div>
                <div><label for="correo">Correo electrónico</label>
                    <input type="email" id="correo" name="correo" maxlength="120" value="<%= Util.val(request, "correo") %>" required></div>
                <div><label for="telefono">Teléfono</label>
                    <input type="text" id="telefono" name="telefono" maxlength="20" value="<%= Util.val(request, "telefono") %>" required></div>
                <div><label for="clave">Contraseña</label>
                    <input type="password" id="clave" name="clave" required>
                    <span class="suave">Mínimo 8 caracteres, una mayúscula y un número.</span></div>
                <div><label for="confirmacion">Confirmar contraseña</label>
                    <input type="password" id="confirmacion" name="confirmacion" required></div>
            </div>
            <div class="acciones">
                <button class="btn primario" type="submit">Crear cuenta</button>
                <a href="<%= ctx %>/api/usuarios/login">Ya tengo cuenta</a>
            </div>
        </form>
    </div>
</div>
