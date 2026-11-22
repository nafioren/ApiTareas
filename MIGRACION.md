# Guía práctica: migrar de Java 8 a Java 21

Notas de lo que cambia al pasar un proyecto de Java 7/8 a Java 21 sobre JBoss/WildFly: qué se
rompe, qué conviene aprovechar y cómo se ve en código real. Los ejemplos de "después" están
tomados de este repositorio.

---

## 1. Lo que se rompe (y cómo arreglarlo)

### `javax.*` → `jakarta.*`

Es el cambio que más archivos toca. Desde Jakarta EE 9 todas las APIs empresariales cambiaron de
paquete. Las versiones nuevas de WildFly / JBoss EAP 8 ya solo traen `jakarta.*`.

```java
// Antes
import javax.persistence.Entity;
import javax.ws.rs.GET;
import javax.inject.Inject;

// Después
import jakarta.persistence.Entity;
import jakarta.ws.rs.GET;
import jakarta.inject.Inject;
```

Ojo: `javax.sql`, `javax.crypto`, `javax.net` **no cambian**, porque son parte del JDK y no de Jakarta EE.

En el `pom.xml` se reemplazan las APIs sueltas por una sola dependencia `provided`:

```xml
<dependency>
    <groupId>jakarta.platform</groupId>
    <artifactId>jakarta.jakartaee-api</artifactId>
    <version>10.0.0</version>
    <scope>provided</scope>
</dependency>
```

### APIs que se fueron del JDK

| Qué | Se eliminó en | Reemplazo |
|---|---|---|
| JAXB (`javax.xml.bind`) | Java 11 | `jakarta.xml.bind` (lo trae el servidor de aplicaciones) |
| JAX-WS, CORBA, `javax.activation` | Java 11 | Las versiones Jakarta |
| Motor JavaScript Nashorn | Java 15 | GraalJS o sacarlo |
| `sun.misc.BASE64Encoder` | Java 9 | `java.util.Base64` |

### Encapsulamiento fuerte del JDK (Java 17)

Las librerías viejas que usaban reflexión sobre clases internas del JDK fallan con
`InaccessibleObjectException`. Primero hay que actualizar la librería; `--add-opens` es solo un parche
temporal.

Para encontrar dónde se usan APIs internas:

```bash
jdeps --jdk-internals --multi-release 21 -cp 'lib/*' mi-app.jar
```

### Compilar con `release`, no con `source`/`target`

```xml
<!-- Antes -->
<maven.compiler.source>1.8</maven.compiler.source>
<maven.compiler.target>1.8</maven.compiler.target>

<!-- Después: además valida que no uses APIs que no existen en esa versión -->
<maven.compiler.release>21</maven.compiler.release>
```

### Herramienta que ayuda

[OpenRewrite](https://docs.openrewrite.org/) tiene recetas que hacen buena parte de la migración
automáticamente (incluido `javax` → `jakarta`):

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-migrate-java:RELEASE \
  -Drewrite.activeRecipes=org.openrewrite.java.migrate.UpgradeToJava21
```

Siempre en una rama aparte y revisando el diff.

---

## 2. Lo que conviene aprovechar

### Records (Java 16): adiós a los DTOs de 60 líneas

```java
// Java 8
public class TareaRequest {
    private final String titulo;
    private final String descripcion;

    public TareaRequest(String titulo, String descripcion) { ... }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    @Override public boolean equals(Object o) { ... }
    @Override public int hashCode() { ... }
    @Override public String toString() { ... }
}

// Java 21 → ver api/TareaRequest.java
public record TareaRequest(String titulo, String descripcion, Prioridad prioridad) {}
```

### Switch expressions (Java 14)

```java
// Java 8: fácil olvidarse un break o un caso
boolean puede;
switch (this) {
    case PENDIENTE: puede = destino == EN_CURSO || destino == CANCELADA; break;
    case EN_CURSO:  puede = destino == HECHA || destino == PENDIENTE || destino == CANCELADA; break;
    default:        puede = false;
}

// Java 21 → ver dominio/Estado.java
return switch (this) {
    case PENDIENTE -> destino == EN_CURSO || destino == CANCELADA;
    case EN_CURSO -> destino == HECHA || destino == PENDIENTE || destino == CANCELADA;
    case HECHA, CANCELADA -> false;
};
```

Sin `default`: si alguien agrega un estado nuevo, **no compila** hasta que se defina su regla.

### Clases selladas + pattern matching en switch (Java 17 / 21)

```java
// Java 8: cadena de instanceof con casteo manual
if (e instanceof TareaNoEncontradaException) {
    status = 404;
} else if (e instanceof TransicionInvalidaException) {
    status = 409;
} else {
    status = 500; // ¿y si me olvidé de un tipo?
}

// Java 21 → ver dominio/TareaException.java y api/TareaExceptionMapper.java
public abstract sealed class TareaException extends RuntimeException
        permits TareaNoEncontradaException, TransicionInvalidaException { }

Status status = switch (e) {
    case TareaNoEncontradaException ignored -> Status.NOT_FOUND;
    case TransicionInvalidaException ignored -> Status.CONFLICT;
};
```

### Pattern matching de `instanceof` (Java 16)

```java
// Java 8
if (e instanceof WebApplicationException) {
    WebApplicationException wae = (WebApplicationException) e;
    ...
}

// Java 21 → ver api/ErrorInesperadoMapper.java
if (e instanceof WebApplicationException wae) {
    ...
}
```

### Detalles que suman

```java
var tarea = obtener(id);                     // var (Java 10)
lista.stream().map(...).toList();            // en vez de collect(Collectors.toList()) (Java 16)
lista.getFirst(); lista.getLast();           // Sequenced Collections (Java 21)
"No se puede pasar de %s a %s".formatted(a, b); // String.formatted (Java 15)
titulo.strip(); texto.isBlank();             // String (Java 11)
```

### Text blocks (Java 15): SQL y JSON legibles

```java
// Java 8
String sql = "SELECT t.id, t.titulo\n" +
             "FROM tarea t\n" +
             "WHERE t.estado = ?";

// Java 21
String sql = """
        SELECT t.id, t.titulo
        FROM tarea t
        WHERE t.estado = ?
        """;
```

### Virtual threads (Java 21)

Para tareas con mucha espera de I/O (llamadas a otros servicios, archivos) permiten miles de hilos
livianos:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    pedidos.forEach(p -> executor.submit(() -> procesar(p)));
}
```

Dentro de un servidor de aplicaciones conviene usar lo que ofrece el contenedor
(`ManagedExecutorService`) en lugar de crear hilos a mano.

---

## 3. ¿Y Java 25?

Java 25 es la siguiente versión LTS (septiembre de 2025). Un proyecto en 21 pasa a 25 con muchos
menos cambios que de 8 a 21. Algunas novedades que quedan disponibles:

- **Scoped values**: una alternativa más segura y eficiente a `ThreadLocal`.
- **Constructores flexibles**: se puede validar argumentos antes de llamar a `super(...)`.
- **Import de módulos** (`import module java.base;`).

---

## 4. Checklist de migración

- [ ] Inventario de dependencias: ¿cuáles tienen versión compatible con Java 21 y Jakarta?
- [ ] `jdeps --jdk-internals` para detectar uso de APIs internas
- [ ] Rama aparte + OpenRewrite para `javax` → `jakarta`
- [ ] `maven.compiler.release=21`
- [ ] Actualizar el servidor (WildFly / JBoss EAP 8) y sus módulos (drivers JDBC, datasources)
- [ ] Correr todos los tests; agregar tests en las zonas sin cobertura **antes** de tocar código
- [ ] Revisar flags de la JVM: algunos ya no existen y desde Java 17 impiden que arranque la JVM (por ejemplo `-XX:MaxPermSize` o `-XX:PermSize`, que se pasan a `-XX:MaxMetaspaceSize`)
- [ ] Probar en un ambiente igual al de producción antes del deploy
