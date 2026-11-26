<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Usuario> usuarios = (List<Usuario>) request.getAttribute("usuarios");
%>
<h1>Cuentas de usuario</h1>
<p class="suave">Toda acción exige un motivo, queda en la bitácora y se notifica al usuario.</p>
<div class="tabla">
    <table>
        <tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Acción</th></tr>
        <% for (Usuario u : usuarios) { %>
        <tr>
            <td><%= h(u.getNombre()) %><br><span class="suave"><%= h(u.getDocumento()) %></span></td>
            <td><%= h(u.getCorreo()) %></td>
            <td><%= h(Util.etiqueta(u.getRol())) %></td>
            <td><%= Util.insignia(u.getEstado()) %></td>
            <td>
                <% if (u.getId() != yo.getId()) { %>
                <form class="en-linea" method="post" action="<%= ctx %>/api/usuarios/estado">
                    <input type="hidden" name="idUsuario" value="<%= u.getId() %>">
                    <select name="estado" aria-label="Nuevo estado">
                        <% for (EstadoUsuario e : EstadoUsuario.values()) { if (e == EstadoUsuario.PENDIENTE) continue; %>
                        <option value="<%= e %>"<%= Util.sel(e == u.getEstado()) %>><%= h(Util.etiqueta(e)) %></option>
                        <% } %>
                    </select>
                    <input type="text" name="motivo" placeholder="Motivo" required>
                    <button class="btn chico" type="submit">Aplicar</button>
                </form>
                <% } else { %><span class="suave">Tu cuenta</span><% } %>
            </td>
        </tr>
        <% } %>
    </table>
</div>
