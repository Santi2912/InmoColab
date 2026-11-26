<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.compracolaborativa.modelo.*,com.inmocolab.inmuebles.modelo.Inmueble"%>
<%@include file="_comun.jspf"%>
<%
    GrupoCompra g = (GrupoCompra) request.getAttribute("grupo");
    Inmueble inm = (Inmueble) request.getAttribute("inmueble");
    Participacion mia = (Participacion) request.getAttribute("miParticipacion");
    BigDecimal saldo = (BigDecimal) request.getAttribute("saldo");
    boolean esAdmin = yo.getRol() == Rol.ADMINISTRADOR;
    boolean ve = mia != null || esAdmin;
    int pct = g.getMontoObjetivo().signum() == 0 ? 0
            : g.getMontoRecaudado().multiply(new BigDecimal("100")).divide(g.getMontoObjetivo(), 0, java.math.RoundingMode.DOWN).min(new BigDecimal("100")).intValue();
%>
<h1>Grupo de compra #<%= g.getId() %> <%= Util.insignia(g.getEstado()) %></h1>
<div class="grid g2">
    <div class="card">
        <h2>Inmueble</h2>
        <p><a href="<%= ctx %>/api/inmuebles/detalle?id=<%= inm.getId() %>"><strong><%= h(inm.getTipo()) %> · <%= h(inm.getCiudad()) %></strong></a><br>
            <span class="suave"><%= h(inm.getDireccion()) %></span></p>
        <p>Precio publicado: <strong><%= Util.dinero(inm.getPrecio()) %></strong></p>
        <p class="suave">Creado por <%= h(cx.usuarios.nombre(g.getIdCreador())) %> · Fecha límite: <strong><%= Util.fecha(g.getFechaLimite()) %></strong></p>
    </div>
    <div class="card">
        <h2>Recaudo</h2>
        <div class="progreso" role="progressbar" aria-valuenow="<%= pct %>" aria-valuemin="0" aria-valuemax="100"><div style="width:<%= pct %>%"></div></div>
        <p><strong><%= Util.dinero(g.getMontoRecaudado()) %></strong> confirmados de <strong><%= Util.dinero(g.getMontoObjetivo()) %></strong> (<%= pct %> %)</p>
        <p class="suave">Saldo por comprometer (incluye aportes pendientes de verificar): <%= Util.dinero(saldo) %></p>
        <% if (g.getEstado() == EstadoGrupo.COMPLETO && mia != null) { %>
        <a class="btn primario" href="<%= ctx %>/api/inmuebles/detalle?id=<%= inm.getId() %>">El grupo está completo: hacer la oferta</a>
        <% } %>
    </div>
</div>

<% if (ve) { %>
<div class="card">
    <h2>Integrantes y participación</h2>
    <div class="tabla" style="margin:0">
        <table>
            <tr><th>Integrante</th><th>Aportes registrados</th><th>Confirmado</th><th>Participación</th><th>Estado</th></tr>
            <% for (Participacion p : g.getParticipaciones()) { %>
            <tr>
                <td><%= h(cx.usuarios.nombre(p.getIdComprador())) %><%= p.getIdComprador() == g.getIdCreador() ? " (creador)" : "" %></td>
                <td>
                    <% if (p.getAportes().isEmpty()) { %><span class="suave">Sin aportes</span><% } %>
                    <% for (Aporte a : p.getAportes()) { %>
                    <%= Util.dinero(a.getMonto()) %> <%= Util.insignia(a.getEstado()) %><% if (a.getMotivoRechazo() != null) { %> <span class="suave">(<%= h(a.getMotivoRechazo()) %>)</span><% } %><br>
                    <% } %>
                </td>
                <td><%= Util.dinero(cx.grupos.confirmado(p)) %></td>
                <td><strong><%= p.getEstado() == EstadoParticipacion.ACTIVA ? Util.numero(p.getPorcentaje()) + " %" : "-" %></strong></td>
                <td><%= Util.insignia(p.getEstado()) %></td>
            </tr>
            <% } %>
        </table>
    </div>
    <p class="suave">Participación = aporte confirmado / total confirmado del grupo × 100. Se recalcula con cada aporte confirmado.</p>
</div>
<% } else { %>
<div class="card"><p>Eres comprador pero aún no perteneces a este grupo. Hay <strong><%= cx.grupos.activos(g) %></strong> integrante(s).</p></div>
<% } %>

<% if (yo.getRol() == Rol.COMPRADOR) { %>
<div class="grid g2">
    <% if (mia == null && g.getEstado() == EstadoGrupo.ABIERTO) { %>
    <div class="card">
        <h2>Unirme al grupo</h2>
        <form method="post" action="<%= ctx %>/api/grupos/unirse">
            <input type="hidden" name="id" value="<%= g.getId() %>">
            <button class="btn primario" type="submit">Unirme</button>
        </form>
    </div>
    <% } %>
    <% if (mia != null && g.getEstado() == EstadoGrupo.ABIERTO) { %>
    <div class="card">
        <h2>Registrar mi aporte</h2>
        <form method="post" action="<%= ctx %>/api/aportes/registrar">
            <input type="hidden" name="idGrupo" value="<%= g.getId() %>">
            <label for="monto">Monto (máximo <%= Util.dinero(saldo) %>)</label>
            <input type="text" id="monto" name="monto" value="<%= Util.val(request, "monto") %>" required>
            <label for="comprobante">Comprobante (nombre del archivo o referencia de pago)</label>
            <input type="text" id="comprobante" name="comprobante" maxlength="255" placeholder="Ej. transferencia-0123.pdf" value="<%= Util.val(request, "comprobante") %>" required>
            <div class="acciones"><button class="btn primario" type="submit">Registrar aporte</button></div>
        </form>
    </div>
    <div class="card">
        <h2>Invitar a otro comprador</h2>
        <form method="post" action="<%= ctx %>/api/grupos/invitar">
            <input type="hidden" name="id" value="<%= g.getId() %>">
            <label for="correo">Correo del comprador registrado</label>
            <input type="email" id="correo" name="correo" value="<%= Util.val(request, "correo") %>" required>
            <div class="acciones"><button class="btn" type="submit">Enviar invitación</button></div>
        </form>
        <% if (!g.getInvitaciones().isEmpty()) { %>
        <p class="suave">Invitaciones enviadas:</p>
        <% for (Invitacion inv : g.getInvitaciones()) { %>
        <div><%= h(cx.usuarios.nombre(inv.getIdInvitado())) %> · <%= Util.fecha(inv.getFechaEnvio()) %> <%= Util.insignia(inv.getEstado()) %></div>
        <% } } %>
    </div>
    <% } %>
    <% if (mia != null && (g.getEstado() == EstadoGrupo.ABIERTO || g.getEstado() == EstadoGrupo.COMPLETO)) { %>
    <div class="card">
        <h2>Abandonar el grupo</h2>
        <p class="suave">Tus aportes pasarán a <em>En devolución</em> y se recalculará la participación de los demás.</p>
        <form method="post" action="<%= ctx %>/api/grupos/abandonar" onsubmit="return confirm('¿Seguro que quieres abandonar el grupo?');">
            <input type="hidden" name="id" value="<%= g.getId() %>">
            <button class="btn peligro" type="submit">Abandonar grupo</button>
        </form>
    </div>
    <% } %>
</div>
<% } %>
