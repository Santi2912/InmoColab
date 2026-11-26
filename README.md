# InmoColab – prototipo funcional

Plataforma para la compra y venta colaborativa de inmuebles (segundo entregable).
Java 8+ con **Servlets y JSP, sin ningún framework** (solo la API de Servlets que aporta Tomcat).
Proyecto web Maven que se abre directamente en NetBeans.

## Cómo ejecutarlo en NetBeans

1. Instala un JDK 8 o superior y **Apache Tomcat 9.0.x** (`.zip` desde https://tomcat.apache.org).
   Usa Tomcat 9 (paquete `javax.servlet`); Tomcat 10+ usa `jakarta.*` y no es compatible con este código.
2. En NetBeans: *Tools → Servers → Add Server → Apache Tomcat* y apunta a la carpeta de Tomcat.
3. *File → Open Project* y elige la carpeta `InmoColab`.
4. Clic derecho sobre el proyecto → *Properties → Run → Server*: Tomcat 9; *Relative URL*: `/` (o déjalo vacío).
5. **F6** (Run). Se abre `http://localhost:8080/InmoColab/`.

También funciona soltando el `.war` generado (`target/InmoColab.war`, o `dist/InmoColab.war` ya incluido) en la carpeta `webapps` de Tomcat.

## Bases de datos: PostgreSQL (SQL) y MongoDB (NoSQL)

- **PostgreSQL** con **JDBC puro** (solo el driver, sin ORM ni framework): usuarios, inmuebles, compra colaborativa,
  negociación, PQR y reportes (MS-Usuarios, MS-Inmuebles, MS-CompraColaborativa, MS-Negociacion, MS-PQR, MS-Reportes).
- **MongoDB** (NoSQL de documentos): **comentarios y calificaciones** (MS-Comentarios), como indica el diseño.
  Base `inmocolab`, colecciones `comentarios` (con los reportes incrustados como arreglo) y `calificaciones`.
  Instalación: `winget install MongoDB.Server` (queda como servicio en el puerto 27017). Se ve con **MongoDB Compass**
  (`winget install MongoDB.Compass.Full`): conectar a `mongodb://localhost:27017` → base `inmocolab`.
  Código: `comun/bd/PersistenciaMongo.java`.

1. Instala PostgreSQL 17 con usuario `postgres` y contraseña `postgres` en el puerto 5432
   (`winget install PostgreSQL.PostgreSQL.17`). Otra contraseña: cámbiala en `src/main/resources/inmocolab.properties`.
2. Ejecuta la app. Al arrancar **crea sola** la base `inmocolab`, las 23 tablas y los datos de ejemplo
   (script en `bd/inmocolab.sql`). No hay que correr ningún script a mano.
3. Mira los datos en **pgAdmin** (viene con el instalador): *Servers → PostgreSQL 17 → Databases → inmocolab → Schemas → public → Tables*.
   O en NetBeans: pestaña *Services → Databases → New Connection* con el driver PostgreSQL.
4. Al reiniciar Tomcat los datos se conservan. Para volver al estado inicial: elimina la base `inmocolab` y reinicia.

Si PostgreSQL no está disponible, la app arranca igual en **modo memoria**; si solo MongoDB no está, únicamente los
comentarios y calificaciones quedan en memoria. El pie de página indica qué bases está usando.
Para forzar memoria: `persistencia=memoria` en `inmocolab.properties`.

Cómo funciona: los servicios trabajan sobre los repositorios en memoria y, al terminar cada petición, `comun/bd/Persistencia`
compara con lo último guardado y ejecuta solo los `INSERT/UPDATE/DELETE` necesarios en una transacción
(`Mapeos` define el mapeo tabla por tabla). Al arrancar con datos existentes, los carga de la base.
El script añade algunas columnas al Anexo A (marcadas `-- prototipo`).

## Cuentas de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@inmocolab.com | Admin123 |
| Propietario | propietario@inmocolab.com (y propietario2@…) | Demo1234 |
| Comprador | comprador@inmocolab.com, comprador2@…, comprador3@… | Demo1234 |
| Asesor | asesor@inmocolab.com | Demo1234 |

Datos de ejemplo iniciales: 14 inmuebles aprobados y uno pendiente, un grupo de compra con aportes, una negociación con oferta, una PQR y un comentario.

## Estructura (igual a la sección 5.2 del documento)

```
src/main/java/com/inmocolab/
  gateway/            APIGatewayServlet (/api/*), FiltroSesion
  usuarios/           presentacion · servicio · modelo · persistencia
  inmuebles/          presentacion · servicio · modelo · persistencia
  compracolaborativa/ presentacion · servicio · modelo · persistencia
  negociacion/        presentacion · servicio · modelo · persistencia
  pqr/                presentacion · servicio · modelo · persistencia
  comentarios/        presentacion · servicio · modelo · persistencia
  reportes/           presentacion · servicio · modelo · persistencia
  comun/              Contexto (composición manual), ServletBase, Util, Flash, Sesion, Datos de ejemplo
src/main/webapp/      css/ · index.jsp · WEB-INF/vistas/ (una JSP por pantalla)
bd/inmocolab.sql      script del Anexo A (PostgreSQL)
```

- **Gateway:** `APIGatewayServlet` recibe todo `/api/*`, verifica sesión y rol y reenvía al servlet del microservicio (`/api/inmuebles/buscar` → `InmuebleServlet`). Los servlets internos (`/ms/*`) rechazan el acceso directo.
- **Capas:** presentación (servlets) → servicio (reglas de negocio) → persistencia (repositorios). El modelo son POJO.
- **Persistencia:** repositorios en memoria respaldados por PostgreSQL (ver la sección de base de datos); las capas de servicio y presentación no cambian.

## Patrones de diseño implementados

| Patrón | Dónde |
|---|---|
| API Gateway / Front Controller | `gateway/APIGatewayServlet` |
| Intercepting Filter | `gateway/FiltroSesion` |
| Arquitectura por capas (presentación → servicio → persistencia) | cada paquete de microservicio |
| MVC | servlets (controlador), servicios y modelo, JSP (vista) |
| Template Method | `comun/ServletBase` |
| Repository / DAO | clases `*Repositorio` y `comun/RepositorioMemoria` |
| Data Mapper + Unit of Work | `comun/bd/Mapeos`, `Persistencia`, `PersistenciaMongo` |
| Singleton + inyección de dependencias manual | `comun/Contexto` |

## Alcance y límites del prototipo

- Implementa los 8 casos de uso de nivel 0 y sus acciones de nivel 1 (49 historias de usuario).
- **Correo:** no hay servidor de correo; los enlaces de verificación y recuperación se muestran en pantalla.
- **Reportes:** se exportan a CSV (abre en Excel) y a PDF mediante *Imprimir → Guardar como PDF* del navegador.
- **Comprobantes de aportes y adjuntos de PQR:** los aportes guardan el nombre/referencia del comprobante; los adjuntos de PQR guardan nombre y tamaño (no el archivo). Las fotografías de inmuebles sí se guardan y se muestran.
- **bcrypt** del diseño se sustituye por PBKDF2 (incluido en el JDK) para no depender de más librerías.
- Todos los microservicios comparten una sola base de datos `inmocolab`; en el diseño cada uno tendría la suya.
