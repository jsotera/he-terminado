# ¡He terminado!

Prototipo para que el alumnado indique qué actividades ha terminado y el profesorado vea el progreso y la distribución del aula.

## Requisitos y arranque

- Java 17 o superior.
- Maven Wrapper incluido (descarga Maven automáticamente la primera vez), o abrir `pom.xml` como proyecto Maven en IntelliJ, Eclipse o NetBeans.

```powershell
.\mvnw.cmd clean package
java -jar target/he-terminado-1.0-SNAPSHOT.jar
```

También puedes ejecutar la clase `es.jsotera.heterminado.Main` desde el IDE.

El profesorado abre `http://localhost:7070/index` en el equipo que ejecuta el servidor. Todo el alumnado abre **`http://IP-DEL-PROFESOR:7070/index`**, desde otros equipos de la misma red. El profesor puede usar también esa URL. En Windows, `ipconfig` muestra la dirección IPv4 del adaptador de red. Si el cortafuegos pregunta, permite Java en la red privada del aula. La red debe permitir conexiones entre los equipos.

Para cambiar el puerto o el archivo de datos:

```powershell
java -Dport=8080 -Ddata=datos/mi-aula.xml -jar target/he-terminado-1.0-SNAPSHOT.jar
```

Las opciones `-D` van antes de `-jar`. El archivo se resuelve desde la carpeta donde se ejecuta el comando.

## Uso

1. El profesorado añade una o varias actividades.
2. Cada alumno introduce su nombre y marca o desmarca sus actividades. Una cookie recuerda su identidad durante un año; «Cambiar nombre» conserva su progreso y asiento.
3. El panel del profesor se actualiza al cambiar el aula. Con «Todas las actividades», cada tarjeta muestra todas las tareas y se pone verde cuando están todas terminadas. Al elegir una actividad, la tarjeta muestra solo esa tarea y queda verde o roja según su estado. El filtro se aplica también a «Sin asiento» y a la vista maximizada. Sin actividades, las tarjetas aparecen en gris; el alumnado desactivado conserva su aspecto gris.
4. El profesor configura filas y columnas (1–12) y arrastra cada alumno desde «Sin asiento». También puede seleccionar una tarjeta y pulsar el destino, con ratón o teclado. Al mover a un asiento ocupado se intercambian los sitios.
5. Al reducir el aula, los alumnos que quedan fuera pasan a «Sin asiento». Los demás conservan su fila y columna.
6. «Reiniciar progreso» pone todas las actividades como pendientes, conservando actividades, identidades y asientos. Eliminar una actividad elimina también su progreso, previa confirmación.
7. «Invertir vista del aula» coloca la pizarra abajo y el pupitre 1·1 abajo a la derecha. Solo cambia el dibujo: las asignaciones se mantienen. Al desmarcarlo vuelve la vista original.
8. Arrastra la ficha «Pasillo» a cualquier pupitre para convertirlo en un hueco sin número que no admite alumnado. También puedes pulsar «Pasillo» y luego el pupitre, con ratón o teclado. Si estaba ocupado, el alumno pasa a «Sin asiento». Pulsa la × del pasillo para recuperar un pupitre libre. La numeración visible se recalcula saltando los pasillos: `1·1 — pasillo — 1·2`. Las filas que solo contienen pasillos tampoco cuentan como filas numeradas. Cambian las etiquetas, pero cada alumno conserva su lugar y la vista invertida mantiene esa misma numeración.

La orientación y los pasillos se guardan automáticamente en el XML. Al redimensionar, los pasillos conservan su fila y columna y se eliminan los que queden fuera.

Para compartir el plano con la clase, pulsa «Maximizar» en «Mi aula». Se ocultan la cabecera, los contadores, los controles y el alumnado sin asiento para mostrar solo la pizarra y los pupitres con texto más grande. El progreso sigue actualizándose. «Minimizar» o la tecla Escape recuperan la vista completa; no cambian las asignaciones ni la configuración del aula.

## Acceso directo

Para abrir el aula desde un icono, crea un acceso directo en el equipo del alumno con la URL `http://IP-DEL-PROFESOR:7070/index` (o el puerto configurado). Basta con el navegador y la conexión al servidor del aula; no es necesario instalar una aplicación ni configurar certificados.

## Gestión manual del alumnado

Cada tarjeta incluye un ojo y una ×. El ojo desactiva o reactiva la participación: la tarjeta queda en gris, conserva nombre, asiento y progreso, y el alumno no puede marcar actividades hasta que el profesor vuelva a mostrarlo. El estado se guarda en XML. No se comprueba automáticamente si está conectado.

La × elimina al alumno y su progreso, previa confirmación, y libera su asiento. Si su navegador sigue abierto, volverá a la pantalla de nombre y podrá entrar de nuevo si el profesor se lo indica.

Las actualizaciones llegan mediante una conexión SSE de Javalin: el servidor envía un aviso cuando cambia el aula y el navegador consulta el estado actualizado. No hay consultas cada dos segundos, latidos ni tiempo de desconexión. La conexión se restablece automáticamente si se corta.

## Identificación por IP

El servidor compara la dirección remota de la conexión HTTP con su dirección local; también reconoce las direcciones de loopback (`localhost`). No pide nombre al profesorado ni confía en `X-Forwarded-For`. Las rutas de administración comprueban el rol en el servidor.

Está diseñado para **conexión directa en una red de aula de confianza**. No debe colocarse detrás de un proxy, túnel, NAT que oculte los clientes o alojamiento público: la identificación por IP no sustituye una contraseña. Un navegador abierto en el equipo servidor siempre tendrá el rol de profesor; para probar alumnado hay que entrar desde otro equipo o móvil en la misma red.

El nombre no es una contraseña. Dos alumnos pueden tener el mismo nombre y tendrán identidades distintas. Si se borran las cookies o se cambia de navegador, se crea otra identidad; este prototipo no incluye recuperación ni eliminación de alumnado. El alumno solo recibe sus datos y las actividades, no la lista del resto de la clase.

## Persistencia XML

`data/aula.xml` se crea al realizar el primer cambio y conserva actividades, alumnado, progreso, dimensiones y asientos. Cada cambio se guarda antes de confirmarse; las escrituras están sincronizadas y se usa un archivo temporal para reemplazar el anterior. Usa una sola instancia del servidor por archivo.

Para hacer una copia de seguridad, detén el servidor y copia el XML. Para modificarlo manualmente o restaurarlo, hazlo con el servidor detenido. Un archivo ilegible o incoherente impide el arranque en lugar de sobrescribirse. Los datos reales están excluidos de Git.

Ejemplo del formato (los identificadores se generan automáticamente):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<classroom rows="4" columns="5" inverted="false">
  <aisles><aisle seat="2"/></aisles>
  <activities><activity id="act-1">Ejercicio de arrays</activity></activities>
  <students>
    <student id="alumno-1" name="Ana" seat="0" hidden="false">
      <done activityId="act-1"/>
    </student>
  </students>
</classroom>
```

Los asientos empiezan por 0, de izquierda a derecha y de arriba abajo. Sin atributo `seat`, el alumno no tiene asiento.
`hidden="true"` desactiva temporalmente la participación; si el atributo no existe en un XML anterior, el alumno permanece activo.
`inverted` configura la vista y `aisles` contiene las posiciones reservadas como pasillo. Los XML anteriores, sin estos datos, siguen siendo válidos: se abren con orientación normal y sin pasillos.

## Organización para clase

```text
src/main/java/es/jsotera/heterminado/
  Main.java                   Arranque y configuración
  config/WebConfig            Clase que configura Javalin y FreeMarker
  model/                      Clases: Activity, Student, Classroom
  repository/XmlRepository    Lectura y escritura XML con DOM
  service/ClassroomService    Reglas del aula y cambios de estado
  web/ClassroomController    Rutas HTTP y validación de acceso
  web/ClassroomEvents        Avisos al navegador cuando cambia el aula
  web/TeacherAccess          Comparación de IP
  web/request/               Clases con los datos JSON de cada petición
src/main/resources/
  templates/index.ftl         Vista FreeMarker según el rol
  public/app.js               Peticiones fetch, refresco y drag & drop
  public/style.css            Estilos adaptables
src/test/java/                Pruebas de reglas, persistencia y HTTP
```

Se emplean clases tradicionales con atributos privados, constructores y getters, listas y conjuntos, bucles `for`, métodos sincronizados y JavaScript sin frameworks. Por ejemplo, `student.getName()` devuelve el nombre. Los modelos no tienen setters: al modificar un alumno se construye otro objeto, conservando la protección de los datos que se están consultando o guardando. `equals` y `hashCode` comparan el contenido de los modelos, también al comprobar la persistencia.

Las clases de `web/request` sí incluyen constructor vacío, getters y setters: Jackson crea estos objetos al leer el JSON del navegador. El JSON y el formato XML son los mismos que antes de esta adaptación.

El código Java, incluidas las pruebas, no usa lambdas, referencias a métodos, Stream API ni `var`. Las rutas se registran con implementaciones explícitas de `Handler`; `WebConfig` implementa `Consumer<JavalinConfig>` y configura el servidor mediante su método `accept`. Estas interfaces pertenecen a Javalin y a Java y se implementan con clases, sin expresiones lambda. JavaScript utiliza funciones declaradas con `function`, sin funciones flecha.

No hay base de datos, contenedor de dependencias ni herramientas de compilación de frontend. FreeMarker escapa el HTML con la directiva `output_format="HTML" auto_esc=true` de la plantilla y JavaScript incorpora los nombres con `textContent`.

### Relación con HolaJavalin

La estructura de arranque y rutas sigue el ejemplo de [HolaJavalin](https://github.com/aetxabao/HolaJavalin/tree/e969e193678ca2eb3316a8acb8ed9a923ac4367e): `Main`, configuración mediante `Javalin.create(...)`, recursos en `/public`, plantillas `.ftl` en `/templates` y rutas `app.get(...)`, `app.post(...)`, etc. que apuntan a métodos con un parámetro `Context`.

El controlador registra las rutas en `routes(app)` mediante objetos `Handler` cuyo método `handle(Context ctx)` llama al método correspondiente del controlador. Cada método lee los datos de la petición, llama al servicio y prepara la respuesta. En `index`, un `HashMap` contiene el modelo que recibe FreeMarker, como en el ejemplo de saludo.

El POM conserva Java 17, la versión de proyecto `1.0-SNAPSHOT` y todas las dependencias del ejemplo con sus mismas versiones:

| Dependencia | Versión |
| --- | --- |
| Javalin | 6.4.0 |
| javalin-rendering | 6.1.3 |
| FreeMarker | 2.3.32 |
| slf4j-simple | 2.0.10 |
| log4j-core | 2.17.1 |
| junit-jupiter-engine | 5.8.1 |

Se mantiene además Jackson 2.20.1 para las peticiones y respuestas JSON. Los plugins de compilación, pruebas y empaquetado permiten generar el JAR ejecutable. El puerto sigue siendo 7070 y el archivo XML mantiene su formato anterior.

Para estudiarlo, empieza por los modelos, sigue por el servicio y el repositorio, y termina con las rutas y las llamadas `fetch`.

## Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas usan carpetas temporales y un servidor HTTP en un puerto libre. Comprueban persistencia tras reinicio, cambios de nombre, progreso, intercambio de asientos, reducción del aula, errores de escritura, XML corrupto, criterio de IP, plantillas, recursos y protección de peticiones.

En Linux o macOS usa `./mvnw` (puede requerir `chmod +x mvnw`). Con Maven ya instalado puedes usar `mvn`.

Tecnologías: Java 17, [Javalin 6](https://javalin.io/archive/docs/v6.4.0.html), [FreeMarker](https://freemarker.apache.org/), Maven, HTML, CSS y JavaScript.
