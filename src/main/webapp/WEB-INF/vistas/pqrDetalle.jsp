<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.pqr.modelo.*"%>
<%@include file="_comun.jspf"%>
<%
    PQR p = (PQR) request.getAttribute("pqr");
    boolean esAdmin = Boolean.TRUE.equals(request.getAttribute("esAdmin"));
    boolean esDueno = Boolean.TRUE.equals(request.getAttribute("esDueno"));
    boolean puedeResponder = Boolean.TRUE.equals(request.getAttribute("puedeResponder"));
    @SuppressWarnings("unchecked")
    List<Usuario> responsables = (List<Usuario>) request.getAttribute("responsables");
    boolean abierta = p.getEstado() == EstadoPqr.RADICADA || p.getEstado() == EstadoPqr.EN_TRAMITE;
%>
<h1><%= h(p.getRadicado()) %> <%= Util.insignia(p.getEstado()) %></h1>
<div class="card">
    <p><strong><%= h(Util.etiqueta(p.getTipo())) %>:</strong> <%= h(p.getAsunto()) %></p>
    <p><%= h(p.getDescripcion()) %></p>
    <p class="suave">Radicada por <%= h(cx.usuarios.nombre(p.getIdUsuario())) %> el <%= Util.fecha(p.getFechaRadicacion()) %> ·
        Fecha límite de respuesta: <strong><%= Util.fecha(p.getFechaLimite()) %></strong><%= cx.pqr.estaVencida(p) ? " (VENCIDA)" : "" %> ·
        Responsable: <%= h(cx.usuarios.nombre(p.getIdResponsable())) %></p>
    <% if (p.getIdInmueble() != null) { %><p class="suave">Inmueble relacionado: <a href="<%= ctx %>/api/inmuebles/detalle?id=<%= p.getIdInmueble() %>">#<%= p.getIdInmueble() %></a></p><% } %>
    <% if (!p.getAdjuntos().isEmpty()) { %>
    <p><strong>Evidencias:</strong>
        <% for (Adjunto a : p.getAdjuntos()) { %><span class="badge"><%= h(a.getNombre()) %> (<%= a.getTamano() / 1024 %> KB)</span> <% } %></p>
    <% } %>
</div>

<div class="card">
    <h2>Respuestas</h2>
    <% if (p.getRespuestas().isEmpty()) { %><p class="suave">Aún no hay respuesta.</p><% } %>
    <% for (RespuestaPQR r : p.getRespuestas()) { %>
    <div class="comentario"><strong><%= h(cx.usuarios.nombre(r.getIdAutor())) %></strong> <span class="suave">· <%= Util.fecha(r.getFecha()) %></span><br><%= h(r.getTexto()) %></div>
    <% } %>
    <% if (p.getCalificacion() != null) { %><p>Calificación de la atención: <strong><%= p.getCalificacion() %> / 5</strong></p><% } %>
</div>

<div class="grid g2">
    <% if (esAdmin && p.getEstado() != EstadoPqr.CERRADA) { %>
    <div class="card">
        <h2>Asignar responsable</h2>
        <form class="en-linea" method="post" action="<%= ctx %>/api/pqr/asignar">
            <input type="hidden" name="id" value="<%= p.getId() %>">
            <select name="idResponsable" aria-label="Responsable">
                <% for (Usuario r : responsables) { %><option value="<%= r.getId() %>"<%= Util.sel(p.getIdResponsable() != null && p.getIdResponsable() == r.getId()) %>><%= h(r.getNombre()) %> (<%= h(Util.etiqueta(r.getRol())) %>)</option><% } %>
            </select>
            <button class="btn" type="submit">Asignar</button>
        </form>
    </div>
    <% } %>
    <% if (puedeResponder && abierta) { %>
    <div class="card">
        <h2>Responder</h2>
        <form method="post" action="<%= ctx %>/api/pqr/responder">
            <input type="hidden" name="id" value="<%= p.getId() %>">
            <textarea name="texto" required><%= Util.val(request, "texto") %></textarea>
            <div class="acciones"><button class="btn primario" type="submit">Enviar respuesta</button></div>
        </form>
    </div>
    <% } %>
    <% if (esDueno && p.getEstado() == EstadoPqr.RESPONDIDA) { %>
    <div class="card">
        <h2>¿Quedó resuelta tu solicitud?</h2>
        <form method="post" action="<%= ctx %>/api/pqr/cerrar">
            <input type="hidden" name="id" value="<%= p.getId() %>">
            <label for="calificacion">Califica la atención (opcional)</label>
            <select id="calificacion" name="calificacion">
                <option value="">Sin calificar</option>
                <option value="5">5 – Excelente</option><option value="4">4 – Muy buena</option><option value="3">3 – Buena</option><option value="2">2 – Regular</option><option value="1">1 – Mala</option>
            </select>
            <div class="acciones"><button class="btn primario" type="submit">Sí, cerrar PQR</button></div>
        </form>
    </div>
    <% } %>
    <% if (esDueno && (p.getEstado() == EstadoPqr.RESPONDIDA || p.getEstado() == EstadoPqr.CERRADA) && !p.isReabierta()) { %>
    <div class="card">
        <h2>¿No quedó resuelta?</h2>
        <p class="suave">Puedes reabrirla una sola vez dentro de los 5 días siguientes a la respuesta.</p>
        <form method="post" action="<%= ctx %>/api/pqr/reabrir"><input type="hidden" name="id" value="<%= p.getId() %>"><button class="btn peligro" type="submit">Reabrir PQR</button></form>
    </div>
    <% } %>
</div>
