<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.pqr.modelo.*"%>
<%@include file="_comun.jspf"%>
<%
    @SuppressWarnings("unchecked")
    List<PQR> pqrs = (List<PQR>) request.getAttribute("pqrs");
    String fEstado = request.getParameter("estado") == null ? "" : request.getParameter("estado");
    String fTipo = request.getParameter("tipo") == null ? "" : request.getParameter("tipo");
%>
<h1>PQR vencidas o próximas a vencer</h1>
<form class="card" method="get" action="<%= ctx %>/api/pqr/vencidas">
    <div class="fila">
        <div><label for="estado">Estado</label>
            <select id="estado" name="estado"><option value="">Todos</option>
                <option value="RADICADA"<%= Util.sel("RADICADA".equals(fEstado)) %>>Radicada</option>
                <option value="EN_TRAMITE"<%= Util.sel("EN_TRAMITE".equals(fEstado)) %>>En trámite</option>
            </select></div>
        <div><label for="tipo">Tipo</label>
            <select id="tipo" name="tipo"><option value="">Todos</option>
                <% for (TipoPqr t : TipoPqr.values()) { %><option value="<%= t %>"<%= Util.sel(t.name().equals(fTipo)) %>><%= h(Util.etiqueta(t)) %></option><% } %>
            </select></div>
        <div><label for="desde">Radicadas desde</label><input type="date" id="desde" name="desde" value="<%= h(request.getParameter("desde")) %>"></div>
        <div><label for="hasta">Hasta</label><input type="date" id="hasta" name="hasta" value="<%= h(request.getParameter("hasta")) %>"></div>
    </div>
    <div class="acciones"><button class="btn primario" type="submit">Filtrar</button><a href="<%= ctx %>/api/pqr/vencidas">Limpiar</a></div>
</form>
<p class="suave">Se listan las PQR sin responder cuyo plazo venció o vence en los próximos 3 días. Las vencidas se resaltan en rojo.</p>
<% if (pqrs.isEmpty()) { %>
<div class="card">No hay PQR vencidas o por vencer con esos filtros.</div>
<% } else { %>
<div class="tabla">
    <table>
        <tr><th>Radicado</th><th>Tipo</th><th>Usuario</th><th>Estado</th><th>Fecha límite</th><th>Días transcurridos</th><th>Responsable</th><th></th></tr>
        <% for (PQR p : pqrs) { boolean vencida = cx.pqr.estaVencida(p); %>
        <tr class="<%= vencida ? "alerta" : "" %>">
            <td><strong><%= h(p.getRadicado()) %></strong></td>
            <td><%= h(Util.etiqueta(p.getTipo())) %></td>
            <td><%= h(cx.usuarios.nombre(p.getIdUsuario())) %></td>
            <td><%= Util.insignia(p.getEstado()) %></td>
            <td><%= Util.fecha(p.getFechaLimite()) %><%= vencida ? " · VENCIDA" : "" %></td>
            <td><%= cx.pqr.diasTranscurridos(p) %></td>
            <td><%= h(cx.usuarios.nombre(p.getIdResponsable())) %></td>
            <td><a class="btn chico" href="<%= ctx %>/api/pqr/ver?id=<%= p.getId() %>">Atender</a></td>
        </tr>
        <% } %>
    </table>
</div>
<% } %>
