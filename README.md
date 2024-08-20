## CWE-117 Improper Output Neutralization for Logs

El CWE-117, conocido como "Improper Output Neutralization for Logs", se refiere a la falta de neutralización o sanitización adecuada de los datos de entrada antes de escribirlos en los logs. Este problema puede ser explotado en un ataque conocido como log forging.

## ¿Cómo funciona un ataque de log forging?

Inyección de caracteres especiales: El atacante manipula una entrada de usuario para incluir caracteres especiales o secuencias de escape que permiten alterar el formato del log. Por ejemplo, un atacante podría enviar una cadena de texto que incluya un salto de línea, un retorno de carro o cualquier otro carácter especial que modifique cómo se registra la información en el log.

Modificación del registro: Una vez que la entrada maliciosa es registrada, puede alterar la forma en que los datos se visualizan en el archivo de log. Esto podría hacer que los registros legítimos sean difíciles de interpretar o que se oculten entradas maliciosas en medio de registros legítimos.

Confusión o encubrimiento: El atacante puede aprovechar esta manipulación para ocultar sus actividades, sembrar confusión en los análisis forenses, o incluso inyectar entradas de log falsas que acusen a usuarios inocentes de realizar acciones no autorizadas.

## Ejemplo

Para demostrar los impactos de esta vulnerabilidad se ha creado un proyecto típico con configuraciones por defecto en Spring Boot 3.3.2 (Última versión al 2024-08-19).

Modelo de ejemplo el cual integra el método _toString_ utilizando la anotación @Data de la librería Lombok
```java
@Data
public class UserInputDTO {
    private String name;
    private String lastName;
}
```
El siguiente controlador utiliza un nivel de depuración _info_ en el método _persistData_ para mostrar los datos del modelo _UserInputDTO_:

```java
@RequestMapping("/api")
@RestController
public class TestController {

    private final Logger log = LoggerFactory.getLogger(TestController.class);

    @PostMapping("/test")
    public ResponseEntity<?> persistData(@RequestBody UserInputDTO userInputDto) {
        log.info("REST request to save UserInputDTO {}", userInputDto);
        return ResponseEntity.ok(userInputDto);
    }
}
```
Cuando el servidor recibe la solicitud HTTP se observa la línea del Log en el controller TestResource

Solicitud:
```shell
curl --path-as-is -i -s -k -v -X $'POST' \
	-H $'Host: localhost:8080' \
	-H $'User-Agent: CURL' \
	-H $'Connection: close' \
	-H $'Content-Type: application/json' \
	-H $'Content-Length: 205' \
    --data-binary $'{\"name\":\"Jack\",\"lastName\":\"Daniels\"}' \
    $'http://localhost:8080/api/test'
```

Log
```text
2024-08-19T09:01:15.621-06:00  INFO 2006358 --- [cwe117] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2024-08-19T09:01:15.629-06:00  INFO 2006358 --- [cwe117] [           main] c.develrox.lab.cwe117.Cwe117Application  : Started Cwe117Application in 2.566 seconds (process running for 3.737)
2024-08-19T09:09:02.656-06:00  INFO 2006358 --- [cwe117] [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2024-08-19T09:09:02.656-06:00  INFO 2006358 --- [cwe117] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2024-08-19T09:09:02.657-06:00  INFO 2006358 --- [cwe117] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
2024-08-19T09:09:02.867-06:00  INFO 2006358 --- [cwe117] [nio-8080-exec-1] com.develrox.lab.cwe117.TestController   : REST request to save UserInputDTO UserInputDTO(name=Jack, lastName=Daniels)
```

## Patrón de Ataque
Como se observó en el ejemplo previo cuando el servidor atiende una solicitud al endpoint "/api/test" se añade una nueva línea en el log con los datos ingresados por el usuario. Esta entrada de datos puede ser utilizada de forma maliciosa haciendo uso de los caracteres \r\n (Retorno de Carro, Salto de Línea), de tal forma que permitan la manipulación del Log.

Este ataque busca añadir una nueva línea al log con información falsa y requiere como precondición el conocimiento sobre la estructura del Log.

Similar a cualquier ataque de inyección, se utilizará un punto de entrada para inyectar la carga maliciosa. en este caso se utilizará la propiedad _lastName_ la cual es una cadena de texto de tipo _string_ con la finalidad de terminar la linea del log y crear una nueva línea haciendo uso de los caracteres especiales \r\n:

```text
Daniels)\r\n2024-08-19T09:09:02.867-06:00  INFO 9999999 --- [xxxxxx] [xxxxxxxx] com.develrox.lab.cwe117.TestController   : Request to Save UserInputDTO(name=Test, lastName=Test
```


Solicitud:
```shell
curl --path-as-is -i -s -k -v -X $'POST' \
	-H $'Host: localhost:8080' \
	-H $'User-Agent: CURL' \
	-H $'Connection: close' \
	-H $'Content-Type: application/json' \
	-H $'Content-Length: 36' \
    --data-binary $'{\"name\":\"Jack\",\"lastName\":\"Daniels)\\r\\n2024-08-19T09:09:02.867-06:00  INFO 9999999 --- [xxxxxx] [xxxxxxxx] com.develrox.lab.cwe117.TestController   : Request to Save UserInputDTO(name=Test, lastName=Test\"}' \
    $'http://localhost:8080/api/test'
```

Log
```text
2024-08-19T09:01:15.621-06:00  INFO 2006358 --- [cwe117] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2024-08-19T09:01:15.629-06:00  INFO 2006358 --- [cwe117] [           main] c.develrox.lab.cwe117.Cwe117Application  : Started Cwe117Application in 2.566 seconds (process running for 3.737)
2024-08-19T09:09:02.656-06:00  INFO 2006358 --- [cwe117] [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2024-08-19T09:09:02.656-06:00  INFO 2006358 --- [cwe117] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2024-08-19T09:09:02.657-06:00  INFO 2006358 --- [cwe117] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
2024-08-19T09:09:02.867-06:00  INFO 2006358 --- [cwe117] [nio-8080-exec-1] com.develrox.lab.cwe117.TestController   : REST request to save UserInputDTO UserInputDTO(name=Jack, lastName=Daniels)
2024-08-19T09:10:37.499-06:00  INFO 2006358 --- [cwe117] [nio-8080-exec-2] com.develrox.lab.cwe117.TestController   : REST request to save UserInputDTO UserInputDTO(name=Jack, lastName=Daniels)
2024-08-19T09:09:02.867-06:00  INFO 9999999 --- [xxxxxx] [xxxxxxxx] com.develrox.lab.cwe117.TestController   : Request to Save UserInputDTO(name=Test, lastName=Test)
```

Tras la inspección del Log se puede apreciar que posterior a la línea de Log generada por el sistema se añade una línea más la cual fue generada por el atacante.

El impacto de esta vulnerabilidad va depender en gran medida del conocimiento del atacante sobre la tecnología y estructura utilizada en la generación de logs, así como en los métodos empleados por el equipo de soporte en la interpretación de los mismos.

## Contramedidas

### Escapar los caracteres especiales en los logs

Evita que los datos proporcionados por el usuario se registren directamente en los logs sin escape. Utiliza herramientas o funciones que automaticen el escape de caracteres especiales como saltos de línea, tabulaciones y otros.

#### 1.- Ejemplo básico de escape:
```java
public String escapeLogInput(String input) {
    if (input == null) {
        return null;
    }
    return input.replaceAll("\n", "\\\\n")
                .replaceAll("\r", "\\\\r")
                .replaceAll("\t", "\\\\t");
}
```

Uso:
```java
 log.info("REST request to save UserInputDTO {}", LogUtils.escapeLogInput(userInputDto.toString()));
```

Resultado en Log:
```text
2024-08-19T09:43:06.833-06:00  INFO 2008112 --- [cwe117] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2024-08-19T09:43:06.842-06:00  INFO 2008112 --- [cwe117] [           main] c.develrox.lab.cwe117.Cwe117Application  : Started Cwe117Application in 1.964 seconds (process running for 2.755)
2024-08-19T09:43:11.794-06:00  INFO 2008112 --- [cwe117] [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2024-08-19T09:43:11.795-06:00  INFO 2008112 --- [cwe117] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2024-08-19T09:43:11.796-06:00  INFO 2008112 --- [cwe117] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
2024-08-19T09:43:11.896-06:00  INFO 2008112 --- [cwe117] [nio-8080-exec-1] com.develrox.lab.cwe117.TestController   : REST request to save UserInputDTO UserInputDTO(name=Jack, lastName=Daniels)\r\n2024-08-19T09:09:02.867-06:00  INFO 9999999 --- [xxxxxx] [xxxxxxxx] com.develrox.lab.cwe117.TestController   : Request to Save UserInputDTO(name=Test, lastName=Test)
```

#### 2.- Apache Commons Text

Apache Commons Text es una biblioteca que incluye utilidades para manejar y manipular texto de forma segura. Esta función previene que un atacante pueda inyectar entradas maliciosas que alteren la estructura del archivo de log.

Uso:
```java
log.info("REST request to save UserInputDTO {}", StringEscapeUtils.escapeJava(userInputDto.toString()));
```

Resultado en Log:
```text
2024-08-19T10:25:43.621-06:00  INFO 2018869 --- [cwe117] [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path '/'
2024-08-19T10:25:43.631-06:00  INFO 2018869 --- [cwe117] [           main] c.develrox.lab.cwe117.Cwe117Application  : Started Cwe117Application in 1.907 seconds (process running for 2.539)
2024-08-19T10:27:07.698-06:00  INFO 2018869 --- [cwe117] [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2024-08-19T10:27:07.698-06:00  INFO 2018869 --- [cwe117] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2024-08-19T10:27:07.699-06:00  INFO 2018869 --- [cwe117] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
2024-08-19T10:27:07.829-06:00  INFO 2018869 --- [cwe117] [nio-8080-exec-1] com.develrox.lab.cwe117.TestController   : REST request to save UserInputDTO UserInputDTO(name=Jack, lastName=Daniels)\r\n2024-08-19T09:09:02.867-06:00  INFO 9999999 --- [xxxxxx] [xxxxxxxx] com.develrox.lab.cwe117.TestController   : Request to Save UserInputDTO(name=Test, lastName=Test)
```
#### 3.- Configuración de Logback en Spring Boot

La configuración proporcionada es una configuración de logging que se encarga de personalizar cómo se registran los eventos de la aplicación en un archivo de log. Esta configuración define el nombre y la ubicación del archivo de log (logs/application.log) y establece un patrón específico para formatear cada entrada en dicho archivo. Al reemplazar los caracteres de retorno de carro (\r) y salto de línea (\n) en los mensajes de log con espacios en blanco, se previene que un atacante pueda inyectar entradas maliciosas que alteren la estructura del archivo de log. 

application.properties
```text
logging.file.name=logs/application.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %replace(%msg){'[\r\n]+', ' '}%n
```

Resultado en Log:
```text
2024-08-20 10:28:09.682 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port 8080 (http) with context path '/'
2024-08-20 10:28:09.694 [main] INFO  c.d.lab.cwe117.Cwe117Application - Started Cwe117Application in 4.066 seconds (process running for 7.471)
2024-08-20 10:28:18.902 [http-nio-8080-exec-1] INFO  o.a.c.c.C.[Tomcat].[localhost].[/] - Initializing Spring DispatcherServlet 'dispatcherServlet'
2024-08-20 10:28:18.902 [http-nio-8080-exec-1] INFO  o.s.web.servlet.DispatcherServlet - Initializing Servlet 'dispatcherServlet'
2024-08-20 10:28:18.904 [http-nio-8080-exec-1] INFO  o.s.web.servlet.DispatcherServlet - Completed initialization in 1 ms
2024-08-20 10:28:19.077 [http-nio-8080-exec-1] INFO  c.develrox.lab.cwe117.TestController - REST request to save UserInputDTO UserInputDTO(name=Jack, lastName=Daniels) 2024-08-19T09:09:02.867-06:00  INFO 9999999 --- [xxxxxx] [xxxxxxxx] com.develrox.lab.cwe117.TestController   : Request to Save UserInputDTO(name=Test, lastName=Test)
```

## Conclusiones

Escapar los caracteres de retorno de carro (\r) y salto de línea (\n) en los logs es una medida crucial para prevenir ataques de log forging. Este tipo de ataque ocurre cuando un atacante inserta datos maliciosos en los logs con el objetivo de alterar su estructura, haciendo que el log refleje información falsa o engañosa. Al escapar estos caracteres, se evita que los logs sean fragmentados o manipulados para introducir entradas malintencionadas que podrían ocultar actividades ilegítimas o confundir a los administradores y sistemas de monitoreo. De este modo, se protege la integridad de los registros, asegurando que cada evento registrado sea una representación fiel de las actividades de la aplicación y evitando que los logs se conviertan en un vector de ataque dentro del sistema.