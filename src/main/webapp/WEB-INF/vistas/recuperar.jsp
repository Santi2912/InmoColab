<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<div class="centro">
    <div class="card">
        <h1>Recuperar contraseña</h1>
        <p class="suave">Escribe el correo de tu cuenta y te enviaremos un enlace válido por 24 horas.</p>
        <form method="post" action="<%= ctx %>/api/usuarios/recuperar">
            <label for="correo">Correo electrónico</label>
            <input type="email" id="correo" name="correo" value="<%= Util.val(request, "correo") %>" required autofocus>
            <div class="acciones">
                <button class="btn primario" type="submit">Enviar enlace</button>
                <a href="<%= ctx %>/api/usuarios/login">Cancelar</a>
            </div>
        </form>
    </div>
</div>
