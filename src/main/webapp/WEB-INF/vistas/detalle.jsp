<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<%@page import="com.inmocolab.inmuebles.modelo.*,com.inmocolab.compracolaborativa.modelo.*,com.inmocolab.comentarios.modelo.*,com.inmocolab.negociacion.modelo.Negociacion"%>
<%@include file="_comun.jspf"%>
<%
    Inmueble i = (Inmueble) request.getAttribute("inmueble");
    Publicacion pub = (Publicacion) request.getAttribute("publicacion");
    @SuppressWarnings("unchecked")
    List<GrupoCompra> abiertos = (List<GrupoCompra>) request.getAttribute("gruposAbiertos");
    @SuppressWarnings("unchecked")
    List<Comentario> comentarios = (List<Comentario>) request.getAttribute("comentarios");
    @SuppressWarnings("unchecked")
    List<GrupoCompra> listos = (List<GrupoCompra>) request.getAttribute("misGruposListos");
    @SuppressWarnings("unchecked")
    List<Usuario> asesores = (List<Usuario>) request.getAttribute("asesores");
    Negociacion neg = (Negociacion) request.getAttribute("negociacion");
    boolean yaInteresado = Boolean.TRUE.equals(request.getAttribute("yaInteresado"));
    int interesados = (Integer) request.getAttribute("interesados");
    boolean esDueno = yo.getId() == i.getIdPropietario();
    boolean esAdmin = yo.getRol() == Rol.ADMINISTRADOR;
    boolean publicado = cx.inmuebles.estaPublicado(i);
    boolean comprador = yo.getRol() == Rol.COMPRADOR;
    boolean ofertaAceptada = cx.negociaciones.tieneOfertaAceptada(i.getId());
%>
<h1><%= h(i.getTipo()) %> en <%= h(i.getCiudad()) %></h1>
<div class="card">
    <div class="galeria">
        <% for (Fotografia f : i.getFotografias()) { %>
        <img src="<%= ctx + f.getUrl() %>" alt="Foto <%= f.getOrden() %> del inmueble">
        <% } %>
    </div>
</div>
<div class="grid g2">
    <div class="card">
        <div class="precio"><%= Util.dinero(i.getPrecio()) %></div>
        <p><strong><%= h(i.getDireccion()) %></strong>, <%= h(i.getCiudad()) %></p>
        <p><%= Util.numero(i.getArea()) %> m² · <%= h(i.getTipo()) %> · <%= Util.insignia(i.getEstado()) %>
            <% if (pub != null && pub.getEstado() != EstadoPublicacion.APROBADA) { %> · Publicación: <%= Util.insignia(pub.getEstado()) %><% } %></p>
        <p><%= h(i.getDescripcion()) %></p>
        <p class="suave">Propietario: <%= h(cx.usuarios.nombre(i.getIdPropietario())) %> · Asesor: <%= h(cx.usuarios.nombre(i.getIdAsesor())) %> · <%= i.getVisitas() %> visita(s)
            <% if (esDueno || esAdmin) { %> · <%= interesados %> interesado(s)<% } %></p>
        <% if (pub != null && pub.getEstado() == EstadoPublicacion.RECHAZADA) { %>
        <div class="alert error">Publicación rechazada. Motivo: <%= h(pub.getMotivoRechazo()) %></div>
        <% } %>
        <% if (esDueno || esAdmin) { %>
        <p class="suave">Oferta mínima aceptada: <%= Util.dinero(i.getPrecioMinimo()) %></p>
        <div class="acciones">
            <% if (esDueno) { %>
            <a class="btn" href="<%= ctx %>/api/inmuebles/editar?id=<%= i.getId() %>">Editar información</a>
            <form class="en-linea" method="post" action="<%= ctx %>/api/inmuebles/retirar" onsubmit="return confirm('¿Retirar la publicación? Dejará de aparecer en las búsquedas.');">
                <input type="hidden" name="id" value="<%= i.getId() %>">
                <button class="btn peligro" type="submit">Retirar publicación</button>
            </form>
            <% } %>
            <% if (neg != null) { %><a class="btn primario" href="<%= ctx %>/api/negociaciones/ver?id=<%= neg.getId() %>">Ver negociación</a><% } %>
        </div>
        <% } %>
        <% if (esAdmin && asesores != null) { %>
        <form class="en-linea" style="margin-top:10px" method="post" action="<%= ctx %>/api/inmuebles/asignarAsesor">
            <input type="hidden" name="id" value="<%= i.getId() %>">
            <select name="idAsesor" aria-label="Asesor">
                <% for (Usuario a : asesores) { %><option value="<%= a.getId() %>"<%= Util.sel(i.getIdAsesor() != null && i.getIdAsesor() == a.getId()) %>><%= h(a.getNombre()) %></option><% } %>
            </select>
            <button class="btn chico" type="submit">Asignar asesor</button>
        </form>
        <% } %>
    </div>

    <% if (comprador && publicado) { %>
    <div class="card">
        <h2>Quiero este inmueble</h2>
        <% if (ofertaAceptada) { %>
        <p>Este inmueble ya tiene una oferta aceptada y está en formalización.</p>
        <% } else { %>
        <div class="acciones">
            <% if (!yaInteresado) { %>
            <form class="en-linea" method="post" action="<%= ctx %>/api/negociaciones/interes">
                <input type="hidden" name="idInmueble" value="<%= i.getId() %>">
                <button class="btn" type="submit">Manifestar interés</button>
            </form>
            <% } else { %><span class="badge e-ACTIVO">Ya manifestaste interés</span><% } %>
        </div>
        <form method="post" action="<%= ctx %>/api/ofertas/crear">
            <input type="hidden" name="idInmueble" value="<%= i.getId() %>">
            <label for="monto">Hacer una oferta (monto en pesos)</label>
            <input type="text" id="monto" name="monto" placeholder="Ej. 250000000" value="<%= Util.val(request, "monto") %>" required>
            <% if (listos != null && !listos.isEmpty()) { %>
            <label for="idGrupo">Ofertar como</label>
            <select id="idGrupo" name="idGrupo">
                <option value="">Oferta individual</option>
                <% for (GrupoCompra g : listos) { %><option value="<%= g.getId() %>">Grupo #<%= g.getId() %> (recaudado <%= Util.dinero(g.getMontoRecaudado()) %>)</option><% } %>
            </select>
            <% } %>
            <label for="condiciones">Condiciones (opcional)</label>
            <input type="text" id="condiciones" name="condiciones" maxlength="200" value="<%= Util.val(request, "condiciones") %>">
            <div class="acciones"><button class="btn primario" type="submit">Enviar oferta</button></div>
        </form>
        <% } %>
    </div>
    <% } %>
</div>

<% if (comprador && publicado && !ofertaAceptada) { %>
<div class="card">
    <h2>Compra colaborativa</h2>
    <% if (abiertos.isEmpty()) { %><p class="suave">Aún no hay grupos abiertos para este inmueble.</p>
    <% } else { %>
    <div class="tabla">
        <table>
            <tr><th>Grupo</th><th>Objetivo</th><th>Recaudado</th><th>Integrantes</th><th>Fecha límite</th><th></th></tr>
            <% for (GrupoCompra g : abiertos) { %>
            <tr>
                <td>#<%= g.getId() %></td>
                <td><%= Util.dinero(g.getMontoObjetivo()) %></td>
                <td><%= Util.dinero(g.getMontoRecaudado()) %></td>
                <td><%= cx.grupos.activos(g) %></td>
                <td><%= Util.fecha(g.getFechaLimite()) %></td>
                <td><a class="btn chico" href="<%= ctx %>/api/grupos/ver?id=<%= g.getId() %>">Ver grupo</a></td>
            </tr>
            <% } %>
        </table>
    </div>
    <% } %>
    <details>
        <summary>Crear un grupo de compra para este inmueble</summary>
        <form method="post" action="<%= ctx %>/api/grupos/crear">
            <input type="hidden" name="idInmueble" value="<%= i.getId() %>">
            <div class="fila">
                <div><label for="montoObjetivo">Monto objetivo (máx. <%= Util.dinero(i.getPrecio()) %>)</label>
                    <input type="text" id="montoObjetivo" name="montoObjetivo" value="<%= Util.val(request, "montoObjetivo") %>" required></div>
                <div><label for="fechaLimite">Fecha límite</label>
                    <input type="date" id="fechaLimite" name="fechaLimite" value="<%= Util.val(request, "fechaLimite") %>" required></div>
            </div>
            <div class="acciones"><button class="btn primario" type="submit">Crear grupo</button></div>
        </form>
    </details>
</div>
<% } %>

<div class="card" id="comentarios">
    <h2>Comentarios (<%= comentarios.size() %>)</h2>
    <% if (pub != null && pub.getEstado() == EstadoPublicacion.APROBADA) { %>
    <form method="post" action="<%= ctx %>/api/comentarios/publicar">
        <input type="hidden" name="idInmueble" value="<%= i.getId() %>">
        <label for="texto">Escribe un comentario (máximo 500 caracteres)</label>
        <textarea id="texto" name="texto" maxlength="500" required><%= Util.val(request, "texto") %></textarea>
        <div class="acciones"><button class="btn primario" type="submit">Publicar</button></div>
    </form>
    <% } %>
    <% for (Comentario cm : comentarios) {
           boolean propio = cm.getIdAutor() == yo.getId(); %>
    <div class="comentario">
        <strong><%= h(cx.usuarios.nombre(cm.getIdAutor())) %></strong>
        <span class="suave"> · <%= Util.fecha(cm.getFecha()) %><%= cm.isEditado() ? " · editado" : "" %></span>
        <% if (cm.getEstado() == EstadoComentario.OCULTO) { %> <%= Util.insignia(cm.getEstado()) %><% } %>
        <p style="margin:4px 0"><%= h(cm.getTexto()) %></p>
        <% if (cm.getRespuesta() != null) { %>
        <div class="resp"><strong>Propietario:</strong> <%= h(cm.getRespuesta()) %> <span class="suave">· <%= Util.fecha(cm.getFechaRespuesta()) %></span></div>
        <% } %>
        <div class="acciones">
            <% if (esDueno && cm.getRespuesta() == null) { %>
            <details><summary>Responder</summary>
                <form method="post" action="<%= ctx %>/api/comentarios/responder">
                    <input type="hidden" name="id" value="<%= cm.getId() %>">
                    <textarea name="texto" maxlength="500" required></textarea>
                    <button class="btn chico primario" type="submit">Enviar respuesta</button>
                </form></details>
            <% } %>
            <% if (propio && cx.comentarios.puedeEditar(yo, cm)) { %>
            <details><summary>Editar</summary>
                <form method="post" action="<%= ctx %>/api/comentarios/editar">
                    <input type="hidden" name="id" value="<%= cm.getId() %>">
                    <textarea name="texto" maxlength="500" required><%= h(cm.getTexto()) %></textarea>
                    <button class="btn chico primario" type="submit">Guardar</button>
                </form></details>
            <% } %>
            <% if (propio || esAdmin) { %>
            <form class="en-linea" method="post" action="<%= ctx %>/api/comentarios/eliminar" onsubmit="return confirm('¿Eliminar este comentario?');">
                <input type="hidden" name="id" value="<%= cm.getId() %>">
                <button class="btn chico peligro" type="submit">Eliminar</button>
            </form>
            <% } %>
            <% if (!propio) { %>
            <form class="en-linea" method="post" action="<%= ctx %>/api/comentarios/reportar">
                <input type="hidden" name="id" value="<%= cm.getId() %>">
                <select name="motivo" aria-label="Motivo del reporte">
                    <option value="">Reportar…</option>
                    <option>Lenguaje ofensivo</option><option>Spam o publicidad</option><option>Información falsa</option><option>Otro</option>
                </select>
                <button class="btn chico" type="submit">Reportar</button>
            </form>
            <% } %>
        </div>
    </div>
    <% } %>
</div>
