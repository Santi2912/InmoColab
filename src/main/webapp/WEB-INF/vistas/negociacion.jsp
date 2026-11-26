<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.negociacion.modelo.*,com.inmocolab.inmuebles.modelo.Inmueble,com.inmocolab.comentarios.modelo.Calificacion,com.inmocolab.compracolaborativa.modelo.GrupoCompra"%>
<%@include file="_comun.jspf"%>
<%
    Negociacion n = (Negociacion) request.getAttribute("negociacion");
    Inmueble inm = (Inmueble) request.getAttribute("inmueble");
    boolean esPropietario = Boolean.TRUE.equals(request.getAttribute("esPropietario"));
    boolean esAsesor = Boolean.TRUE.equals(request.getAttribute("esAsesor"));
    boolean puedeCalificar = Boolean.TRUE.equals(request.getAttribute("puedeCalificar"));
    boolean esAdmin = yo.getRol() == Rol.ADMINISTRADOR;
    @SuppressWarnings("unchecked")
    List<Usuario> asesores = (List<Usuario>) request.getAttribute("asesores");
    @SuppressWarnings("unchecked")
    List<Calificacion> calificaciones = (List<Calificacion>) request.getAttribute("calificaciones");
    EstadoNegociacion[] camino = {EstadoNegociacion.INICIADA, EstadoNegociacion.EN_OFERTA, EstadoNegociacion.ACEPTADA, EstadoNegociacion.EN_FORMALIZACION, EstadoNegociacion.CERRADA};
    boolean terminada = n.getEstado() == EstadoNegociacion.CERRADA || n.getEstado() == EstadoNegociacion.CANCELADA;
    // La negociación pasa por ACEPTADA de forma instantánea: se marca como hecha si llegó a formalización.
    boolean pasoAceptada = false;
    for (CambioEstado c : n.getHistorial()) { pasoAceptada |= c.getEstado() == EstadoNegociacion.ACEPTADA; }
%>
<h1>Negociación #<%= n.getId() %> <%= Util.insignia(n.getEstado()) %></h1>
<div class="card">
    <% if (n.getEstado() == EstadoNegociacion.CANCELADA) { %>
    <div class="linea"><span class="paso">Negociación cancelada</span></div>
    <% } else { %>
    <div class="linea">
        <% int actual = java.util.Arrays.asList(camino).indexOf(n.getEstado());
           for (int k = 0; k < camino.length; k++) {
               boolean hecho = k < actual || (camino[k] == EstadoNegociacion.ACEPTADA && pasoAceptada && actual > k);
               String clase = k == actual ? "actual" : (hecho ? "hecho" : ""); %>
        <span class="paso <%= clase %>"><%= h(Util.etiqueta(camino[k])) %></span>
        <% } %>
    </div>
    <% } %>
    <p><a href="<%= ctx %>/api/inmuebles/detalle?id=<%= inm.getId() %>"><strong><%= h(inm.getTipo()) %> · <%= h(inm.getCiudad()) %></strong></a> — <%= h(inm.getDireccion()) %>
        · Precio <strong><%= Util.dinero(inm.getPrecio()) %></strong></p>
    <p class="suave">Propietario: <%= h(cx.usuarios.nombre(inm.getIdPropietario())) %> · Asesor: <%= h(cx.usuarios.nombre(n.getIdAsesor())) %> · Inicio: <%= Util.fecha(n.getFechaInicio()) %>
        <%= n.getFechaFin() != null ? " · Fin: " + Util.fecha(n.getFechaFin()) : "" %></p>
    <div class="acciones">
        <% if ((esPropietario || esAdmin) && n.getEstado() == EstadoNegociacion.EN_FORMALIZACION) { %>
        <form class="en-linea" method="post" action="<%= ctx %>/api/negociaciones/cerrar" onsubmit="return confirm('¿Confirmas que la venta quedó formalizada? El inmueble pasará a Vendido.');">
            <input type="hidden" name="id" value="<%= n.getId() %>"><button class="btn primario" type="submit">Cerrar negociación (venta formalizada)</button>
        </form>
        <% } %>
        <% if ((esPropietario || esAdmin) && !terminada) { %>
        <form class="en-linea" method="post" action="<%= ctx %>/api/negociaciones/cancelar">
            <input type="hidden" name="id" value="<%= n.getId() %>">
            <input type="text" name="motivo" placeholder="Motivo de la cancelación" required>
            <button class="btn peligro" type="submit">Cancelar negociación</button>
        </form>
        <% } %>
        <% if (esAdmin && asesores != null && !terminada) { %>
        <form class="en-linea" method="post" action="<%= ctx %>/api/negociaciones/asignar">
            <input type="hidden" name="id" value="<%= n.getId() %>">
            <select name="idAsesor" aria-label="Asesor">
                <% for (Usuario a : asesores) { %><option value="<%= a.getId() %>"<%= Util.sel(n.getIdAsesor() != null && n.getIdAsesor() == a.getId()) %>><%= h(a.getNombre()) %></option><% } %>
            </select>
            <button class="btn" type="submit">Asignar asesor</button>
        </form>
        <% } %>
    </div>
</div>

<h2>Ofertas</h2>
<% if (n.getOfertas().isEmpty()) { %>
<div class="card">Todavía no hay ofertas en esta negociación.</div>
<% } else { %>
<div class="tabla">
    <table>
        <tr><th>Oferente</th><th>Monto</th><th>Condiciones</th><th>Fecha</th><th>Estado</th><th></th></tr>
        <% for (Oferta o : n.getOfertas()) {
               String oferente = o.getIdGrupo() != null ? "Grupo #" + o.getIdGrupo() : cx.usuarios.nombre(o.getIdComprador());
               boolean mia = o.getIdComprador() != null && o.getIdComprador() == yo.getId();
               if (o.getIdGrupo() != null && yo.getRol() == Rol.COMPRADOR) {
                   mia = cx.grupos.participacionActiva(cx.grupos.requerir(o.getIdGrupo()), yo.getId()) != null;
               } %>
        <tr>
            <td><%= h(oferente) %><% if (o.getIdGrupo() != null) { %><br><a class="suave" href="<%= ctx %>/api/grupos/ver?id=<%= o.getIdGrupo() %>">ver grupo</a><% } %></td>
            <td><strong><%= Util.dinero(o.getMonto()) %></strong></td>
            <td><%= h(o.getCondiciones()) %></td>
            <td><%= Util.fecha(o.getFecha()) %></td>
            <td><%= Util.insignia(o.getEstado()) %><% if (o.getMotivoRechazo() != null) { %><br><span class="suave"><%= h(o.getMotivoRechazo()) %></span><% } %></td>
            <td>
                <% if (o.getEstado() == EstadoOferta.PENDIENTE && esPropietario) { %>
                <form class="en-linea" method="post" action="<%= ctx %>/api/ofertas/aceptar" onsubmit="return confirm('Al aceptar, las demás ofertas pendientes serán rechazadas. ¿Continuar?');">
                    <input type="hidden" name="id" value="<%= o.getId() %>"><button class="btn chico primario" type="submit">Aceptar</button>
                </form>
                <form class="en-linea" method="post" action="<%= ctx %>/api/ofertas/rechazar">
                    <input type="hidden" name="id" value="<%= o.getId() %>">
                    <input type="text" name="motivo" placeholder="Motivo" required>
                    <button class="btn chico peligro" type="submit">Rechazar</button>
                </form>
                <% } %>
                <% if (o.getEstado() == EstadoOferta.PENDIENTE && mia) { %>
                <form class="en-linea" method="post" action="<%= ctx %>/api/ofertas/retirar">
                    <input type="hidden" name="id" value="<%= o.getId() %>"><button class="btn chico" type="submit">Retirar mi oferta</button>
                </form>
                <% } %>
            </td>
        </tr>
        <% } %>
    </table>
</div>
<% } %>

<div class="grid g2">
    <div class="card">
        <h2>Historial de estados</h2>
        <% for (CambioEstado c : n.getHistorial()) { %>
        <div><span class="suave"><%= Util.fecha(c.getFecha()) %></span> · <%= Util.insignia(c.getEstado()) %> <%= h(c.getDetalle()) %></div>
        <% } %>
    </div>
    <div class="card">
        <h2>Observaciones del asesor</h2>
        <% if (n.getObservaciones().isEmpty()) { %><p class="suave">Sin observaciones.</p><% } %>
        <% for (Observacion o : n.getObservaciones()) { %>
        <div class="comentario"><strong><%= h(cx.usuarios.nombre(o.getIdAsesor())) %></strong> <span class="suave">· <%= Util.fecha(o.getFecha()) %></span><br><%= h(o.getTexto()) %></div>
        <% } %>
        <% if (esAsesor && !terminada) { %>
        <form method="post" action="<%= ctx %>/api/negociaciones/observacion">
            <input type="hidden" name="id" value="<%= n.getId() %>">
            <label for="texto">Nueva observación</label>
            <textarea id="texto" name="texto" required><%= Util.val(request, "texto") %></textarea>
            <div class="acciones"><button class="btn primario" type="submit">Registrar observación</button></div>
        </form>
        <% } %>
    </div>
</div>

<% if (n.getEstado() == EstadoNegociacion.CERRADA) { %>
<div class="card">
    <h2>Calificaciones de la experiencia</h2>
    <% if (calificaciones.isEmpty()) { %><p class="suave">Aún no hay calificaciones.</p><% } %>
    <% for (Calificacion c : calificaciones) { %>
    <div class="comentario"><strong><%= h(cx.usuarios.nombre(c.getIdComprador())) %></strong> · <%= "★★★★★".substring(0, c.getEstrellas()) %><span class="suave"><%= "★★★★★".substring(c.getEstrellas()).replace('★', '☆') %></span>
        <%= c.getTexto() == null ? "" : "— " + h(c.getTexto()) %></div>
    <% } %>
    <% if (puedeCalificar) { %>
    <form method="post" action="<%= ctx %>/api/negociaciones/calificar">
        <input type="hidden" name="id" value="<%= n.getId() %>">
        <div class="fila">
            <div><label for="estrellas">Calificación</label>
                <select id="estrellas" name="estrellas" required>
                    <option value="">Selecciona…</option>
                    <option value="5">5 – Excelente</option><option value="4">4 – Muy buena</option><option value="3">3 – Buena</option><option value="2">2 – Regular</option><option value="1">1 – Mala</option>
                </select></div>
            <div><label for="textoCal">Comentario (opcional)</label><input type="text" id="textoCal" name="texto" maxlength="500"></div>
        </div>
        <div class="acciones"><button class="btn primario" type="submit">Calificar</button></div>
    </form>
    <% } %>
</div>
<% } %>
