# api-tareas

[![CI](https://github.com/nafioren/api-tareas/actions/workflows/ci.yml/badge.svg)](https://github.com/nafioren/api-tareas/actions/workflows/ci.yml)
[![CodeQL](https://github.com/nafioren/api-tareas/actions/workflows/codeql.yml/badge.svg)](https://github.com/nafioren/api-tareas/actions/workflows/codeql.yml)

API REST para gestionar tareas, hecha con **Java 21**, **Jakarta EE 10**, **WildFly** y **MySQL**.

El objetivo no es la app en sí sino mostrar cómo se ve un servicio Java empresarial escrito con
**Java moderno**: records, clases selladas, pattern matching, buenas prácticas de seguridad, tests
y un pipeline que valida todo en cada push.

> 📖 En [MIGRACION.md](MIGRACION.md) hay una guía práctica de migración de **Java 8 a Java 21**,
> con ejemplos de antes y después tomados de este código.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 (LTS) |
| API REST | Jakarta REST (JAX-RS) |
| Persistencia | Jakarta Persistence (JPA) + MySQL 8.4 |
| Inyección y transacciones | CDI + Jakarta Transactions |
| Validación | Jakarta Bean Validation |
| Servidor | WildFly (la versión comunitaria de JBoss EAP) |
| Tests | JUnit 5 + prueba de punta a punta con curl |
| CI / Seguridad | GitHub Actions, CodeQL, Dependabot |

## Cómo levantarlo

Necesitás [Docker](https://docs.docker.com/get-docker/) y nada más (ni Java ni Maven instalados).

```bash
cp .env.example .env        # y cambiá las contraseñas
docker compose up --build
```

La API queda en `http://localhost:8080/api/tareas`.

Para correr solo los tests unitarios (requiere Java 21 y Maven):

```bash
mvn verify
```

Para probar la API de punta a punta una vez levantada:

```bash
./scripts/prueba-humo.sh
```

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/tareas?estado=PENDIENTE` | Lista tareas (filtro opcional) |
| `GET` | `/api/tareas/{id}` | Obtiene una tarea |
| `POST` | `/api/tareas` | Crea una tarea |
| `PUT` | `/api/tareas/{id}` | Actualiza título, descripción y prioridad |
| `PATCH` | `/api/tareas/{id}/estado` | Cambia el estado |
| `DELETE` | `/api/tareas/{id}` | Elimina una tarea |

### Ejemplos

```bash
# Crear
curl -X POST localhost:8080/api/tareas \
  -H 'Content-Type: application/json' \
  -d '{"titulo": "Migrar módulo de pagos", "prioridad": "ALTA"}'

# Empezarla
curl -X PATCH localhost:8080/api/tareas/1/estado \
  -H 'Content-Type: application/json' \
  -d '{"estado": "EN_CURSO"}'
```

### Ciclo de vida de una tarea

```
PENDIENTE ──► EN_CURSO ──► HECHA
    │  ◄────────┘  │
    └──► CANCELADA ◄┘
```

Una transición no permitida (por ejemplo, de `PENDIENTE` directo a `HECHA`) devuelve `409 Conflict`:

```json
{ "status": 409, "error": "Conflict", "mensaje": "No se puede pasar de PENDIENTE a HECHA" }
```

## Estructura

```
src/main/java/io/github/nafioren/tareas/
├── dominio/       Entidad, enums y reglas de negocio (sin dependencias de la API)
├── repositorio/   Interfaz de acceso a datos + implementación JPA
├── servicio/      Casos de uso y transacciones
└── api/           Recursos REST, DTOs y manejo de errores
```

Las reglas de negocio viven en el **dominio** (`Estado.puedePasarA`, `Tarea.cambiarEstado`), no en
los controladores. El servicio depende de una **interfaz** de repositorio, por eso los tests corren
en milisegundos con un repositorio en memoria, sin levantar la base ni el servidor.

## Cómo leer el código

Cada clase empieza con un comentario que explica **qué es, por qué existe y quién la usa**. Los
métodos con lógica tienen los pasos numerados. Orden recomendado, siguiendo el recorrido de un pedido:

1. [`Estado`](src/main/java/tareas/dominio/Estado.java) y
   [`Tarea`](src/main/java/tareas/dominio/Tarea.java): el modelo y sus reglas.
2. [`TareaException`](src/main/java/tareas/dominio/TareaException.java) y sus dos
   subclases: los errores de negocio.
3. [`TareaRepository`](src/main/java/tareas/repositorio/TareaRepository.java) y
   [`JpaTareaRepository`](src/main/java/tareas/repositorio/JpaTareaRepository.java):
   cómo se guarda.
4. [`TareaService`](src/main/java/tareas/servicio/TareaService.java): los casos de
   uso y las transacciones.
5. [`TareaResource`](src/main/java/tareas/api/TareaResource.java) y los DTOs
   (`TareaRequest`, `TareaResponse`): la API REST.
6. [`TareaExceptionMapper`](src/main/java/tareas/api/TareaExceptionMapper.java) y
   [`ErrorInesperadoMapper`](src/main/java/tareas/api/ErrorInesperadoMapper.java):
   cómo los errores se convierten en respuestas HTTP.
7. [`TareaServiceTest`](src/test/java/io/github/nafioren/tareas/servicio/TareaServiceTest.java): los
   tests, que también sirven como ejemplos de uso.

**El recorrido de un pedido**, por ejemplo `PATCH /api/tareas/1/estado`:

```
TareaResource.cambiarEstado()      recibe el JSON y lo valida
   └─ TareaService.cambiarEstado()   abre la transacción
        ├─ obtener()                    busca la tarea (si no existe → 404)
        ├─ Tarea.cambiarEstado()        aplica la regla (si no se puede → 409)
        └─ repositorio.guardar()        se guarda al hacer COMMIT
```

## Seguridad

- **Validación de entrada** con Bean Validation: los datos inválidos se rechazan con `400` antes de llegar al servicio.
- **Sin inyección SQL**: todas las consultas usan parámetros de JPA (`PreparedStatement` por debajo).
- **Errores sin filtrar información**: un error inesperado devuelve un código para rastrearlo en el log, nunca el stack trace.
- **DTOs**: la entidad JPA nunca sale por la API, así que no se exponen campos internos.
- **Secretos fuera del código**: las credenciales vienen de variables de entorno, y el `.env` está en `.gitignore`.
- **Red mínima**: MySQL no se expone fuera de Docker y la app solo escucha en `127.0.0.1`.
- **Bloqueo optimista** (`@Version`): dos actualizaciones simultáneas no se pisan.
- **Constraints en la base**: `CHECK` sobre estado y prioridad, como segunda línea de defensa.
- **Análisis automático**: CodeQL revisa el código y Dependabot las dependencias.

## Próximos pasos

- [ ] Autenticación con JWT y roles (Jakarta Security / MicroProfile JWT)
- [ ] Tests de integración con Testcontainers
- [ ] Paginación en el listado
- [ ] Documentación OpenAPI

## Licencia

[MIT](LICENSE)
