<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.comentarios.modelo.*"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<Comentario> comentarios = (List<Comentario>) request.getAttribute("comentarios");
%>
<h1>Moderar comentarios reportados</h1>
<% if (comentarios.isEmpty()) { %>
<div class="card">No hay comentarios reportados pendientes de revisión.</div>
<% } else { %>
<% for (Comentario cm : comentarios) { %>
<div class="card">
    <p><strong><%= h(cx.usuarios.nombre(cm.getIdAutor())) %></strong> en el inmueble
        <a href="<%= ctx %>/api/inmuebles/detalle?id=<%= cm.getIdInmueble() %>#comentarios">#<%= cm.getIdInmueble() %></a>
        <span class="suave">· <%= Util.fecha(cm.getFecha()) %></span> <%= Util.insignia(cm.getEstado()) %></p>
    <p>“<%= h(cm.getTexto()) %>”</p>
    <p class="suave"><strong>Reportes (<%= cm.getReportes().size() %>):</strong></p>
    <ul>
        <% for (ReporteComentario r : cm.getReportes()) { %><li><%= h(cx.usuarios.nombre(r.getIdUsuario())) %>: <%= h(r.getMotivo()) %> <span class="suave">(<%= Util.fecha(r.getFecha()) %>)</span></li><% } %>
    </ul>
    <div class="acciones">
        <form class="en-linea" method="post" action="<%= ctx %>/api/comentarios/moderar">
            <input type="hidden" name="id" value="<%= cm.getId() %>">
            <button class="btn primario" type="submit" name="decision" value="aprobar">Mantener visible</button>
            <input type="text" name="motivo" placeholder="Motivo (solo si oculta)">
            <button class="btn peligro" type="submit" name="decision" value="ocultar">Ocultar</button>
        </form>
    </div>
</div>
<% } %>
<% } %>
