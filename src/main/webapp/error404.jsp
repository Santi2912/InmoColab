<%@page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>No encontrado · InmoColab</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/estilos.css">
</head>
<body>
<main class="centro card">
    <h1>Página no encontrada</h1>
    <p>La dirección que buscas no existe en InmoColab.</p>
    <a class="btn primario" href="<%= request.getContextPath() %>/api/usuarios/inicio">Volver al inicio</a>
</main>
</body>
</html>
