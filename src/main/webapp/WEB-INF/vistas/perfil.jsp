<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<h1>Mi perfil</h1>
<div class="card" style="max-width:640px">
    <form method="post" action="<%= ctx %>/api/usuarios/perfil">
        <div class="fila">
            <div><label>Nombre</label><input type="text" value="<%= h(yo.getNombre()) %>" disabled></div>
            <div><label>Documento de identidad (no editable)</label><input type="text" value="<%= h(yo.getDocumento()) %>" disabled></div>
            <div><label>Rol</label><input type="text" value="<%= h(Util.etiqueta(yo.getRol())) %>" disabled></div>
            <div><label for="telefono">Teléfono</label>
                <input type="text" id="telefono" name="telefono" maxlength="20" value="<%= Util.val(request, "telefono", yo.getTelefono()) %>" required></div>
        </div>
        <label for="correo">Correo electrónico</label>
        <input type="email" id="correo" name="correo" maxlength="120" value="<%= Util.val(request, "correo", yo.getCorreo()) %>" required>
        <p class="suave">Si cambias el correo, deberás verificarlo de nuevo antes de volver a ingresar.</p>
        <div class="acciones"><button class="btn primario" type="submit">Guardar cambios</button></div>
    </form>
</div>
