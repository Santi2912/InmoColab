<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<div class="centro">
    <div class="card">
        <h1>Nueva contraseña</h1>
        <form method="post" action="<%= ctx %>/api/usuarios/restablecer">
            <input type="hidden" name="token" value="<%= h(request.getAttribute("token")) %>">
            <label for="clave">Nueva contraseña</label>
            <input type="password" id="clave" name="clave" required autofocus>
            <span class="suave">Mínimo 8 caracteres, una mayúscula y un número.</span>
            <label for="confirmacion">Confirmar contraseña</label>
            <input type="password" id="confirmacion" name="confirmacion" required>
            <div class="acciones"><button class="btn primario" type="submit">Guardar contraseña</button></div>
        </form>
    </div>
</div>
