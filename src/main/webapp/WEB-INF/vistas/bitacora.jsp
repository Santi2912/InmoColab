<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Bitacora> registros = (List<Bitacora>) request.getAttribute("registros");
    @SuppressWarnings("unchecked")
    List<Usuario> usuarios = (List<Usuario>) request.getAttribute("usuarios");
    String fUsuario = request.getParameter("idUsuario") == null ? "" : request.getParameter("idUsuario");
%>
<h1>Bitácora de operaciones</h1>
<form class="card" method="get" action="<%= ctx %>/api/usuarios/bitacora">
    <div class="fila">
        <div><label for="idUsuario">Usuario</label>
            <select id="idUsuario" name="idUsuario">
                <option value="">Todos</option>
                <% for (Usuario u : usuarios) { %><option value="<%= u.getId() %>"<%= Util.sel(fUsuario.equals(String.valueOf(u.getId()))) %>><%= h(u.getNombre()) %></option><% } %>
            </select></div>
        <div><label for="accionFiltro">Tipo de acción</label>
            <input type="text" id="accionFiltro" name="accionFiltro" placeholder="Ej. INICIO_SESION" value="<%= h(request.getParameter("accionFiltro")) %>"></div>
        <div><label for="desde">Desde</label><input type="date" id="desde" name="desde" value="<%= h(request.getParameter("desde")) %>"></div>
        <div><label for="hasta">Hasta</label><input type="date" id="hasta" name="hasta" value="<%= h(request.getParameter("hasta")) %>"></div>
    </div>
    <div class="acciones"><button class="btn primario" type="submit">Filtrar</button><a href="<%= ctx %>/api/usuarios/bitacora">Limpiar</a></div>
</form>
<p class="suave">Los registros no se pueden modificar ni eliminar. Se muestran los <%= Math.min(registros.size(), 300) %> más recientes de <%= registros.size() %>.</p>
<div class="tabla">
    <table>
        <tr><th>Fecha y hora</th><th>Usuario</th><th>Acción</th><th>Detalle</th></tr>
        <% int mostrados = 0; for (Bitacora b : registros) { if (mostrados++ >= 300) break; %>
        <tr>
            <td><%= Util.fecha(b.getFecha()) %></td>
            <td><%= h(cx.usuarios.nombre(b.getIdUsuario())) %></td>
            <td><code><%= h(b.getAccion()) %></code></td>
            <td><%= h(b.getDetalle()) %></td>
        </tr>
        <% } %>
    </table>
</div>
