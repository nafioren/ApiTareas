package tareas.api;

import tareas.dominio.Estado;
import tareas.servicio.TareaService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import java.util.List;
import java.util.Optional;

/**
 * QUÉ ES
 * El "controlador" REST: recibe los pedidos HTTP, llama al servicio y arma la
 * respuesta. Es la puerta de entrada de la aplicación.
 *
 * QUÉ HACE Y QUÉ NO
 * - Traduce HTTP ↔ Java: parámetros de la URL, JSON del cuerpo, códigos de estado.
 * - NO tiene lógica de negocio: todo lo delega en TareaService. Si mañana se
 *   agregara otra entrada (por ejemplo, una cola de mensajes), reutilizaría
 *   el mismo servicio.
 *
 * CÓMO SE MAPEAN LAS URLS
 * @Path("/tareas") + el prefijo "/api" de RestApplication = /api/tareas.
 * Cada método tiene un verbo HTTP (@GET, @POST...) y opcionalmente un @Path extra.
 *
 * | Verbo  | URL                       | Método         |
 * |--------|---------------------------|----------------|
 * | GET    | /api/tareas?estado=X      | listar         |
 * | GET    | /api/tareas/{id}          | obtener        |
 * | POST   | /api/tareas               | crear          |
 * | PUT    | /api/tareas/{id}          | actualizar     |
 * | PATCH  | /api/tareas/{id}/estado   | cambiarEstado  |
 * | DELETE | /api/tareas/{id}          | eliminar       |
 *
 * ¿Y SI ALGO FALLA?
 * Este código no tiene try/catch. Las excepciones suben y las atrapan los
 * mapeadores: TareaExceptionMapper (errores de negocio) y
 * ErrorInesperadoMapper (todo lo demás).
 */
@Path("/tareas")
@Produces(MediaType.APPLICATION_JSON)   // todas las respuestas son JSON
@Consumes(MediaType.APPLICATION_JSON)   // los cuerpos que recibe son JSON
public class TareaResource {

    /** CDI inyecta el servicio (con su proxy transaccional). */
    @Inject
    TareaService servicio;

    /** Información de la URL del pedido actual; se usa para armar el header Location. */
    @Context
    UriInfo uriInfo;

    /**
     * GET /api/tareas o GET /api/tareas?estado=PENDIENTE
     *
     * @QueryParam lee el parámetro "?estado=" de la URL. Si no viene, llega null.
     * Si viene un valor que no es un Estado válido, JAX-RS responde un error
     * solo, sin llegar a este método (la especificación indica 404 en ese caso).
     */
    @GET
    public List<TareaResponse> listar(@QueryParam("estado") Estado estado) {
        return servicio.listar(Optional.ofNullable(estado)).stream()
                .map(TareaResponse::desde)   // cada entidad → su DTO
                .toList();                   // Stream.toList() es de Java 16 (antes: collect(Collectors.toList()))
    }

    /** GET /api/tareas/5 → @PathParam toma el "5" de la URL. */
    @GET
    @Path("/{id}")
    public TareaResponse obtener(@PathParam("id") long id) {
        return TareaResponse.desde(servicio.obtener(id));
    }

    /**
     * POST /api/tareas
     *
     * @Valid dispara las validaciones de TareaRequest; @NotNull rechaza un cuerpo vacío.
     */
    @POST
    public Response crear(@Valid @NotNull TareaRequest req) {
        // Paso 1: crear la tarea (el servicio la guarda y MySQL le asigna un id)
        var tarea = servicio.crear(req.titulo(), req.descripcion(), req.prioridad());

        // Paso 2: armar la URL del recurso nuevo, por ejemplo http://localhost:8080/api/tareas/7
        var ubicacion = uriInfo.getAbsolutePathBuilder().path(String.valueOf(tarea.getId())).build();

        // Paso 3: responder 201 Created + header "Location" con esa URL + la tarea en el cuerpo.
        // Es la convención REST para "se creó algo nuevo, y está acá".
        return Response.created(ubicacion).entity(TareaResponse.desde(tarea)).build();
    }

    /** PUT /api/tareas/5 → reemplaza título, descripción y prioridad. */
    @PUT
    @Path("/{id}")
    public TareaResponse actualizar(@PathParam("id") long id, @Valid @NotNull TareaRequest req) {
        return TareaResponse.desde(servicio.actualizar(id, req.titulo(), req.descripcion(), req.prioridad()));
    }

    /**
     * PATCH /api/tareas/5/estado
     *
     * Se usa PATCH (modificación parcial) y no PUT porque solo cambia un campo.
     */
    @PATCH
    @Path("/{id}/estado")
    public TareaResponse cambiarEstado(@PathParam("id") long id, @Valid @NotNull CambioEstadoRequest req) {
        return TareaResponse.desde(servicio.cambiarEstado(id, req.estado()));
    }

    /** DELETE /api/tareas/5 → responde 204 No Content: salió bien y no hay nada que devolver. */
    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") long id) {
        servicio.eliminar(id);
        return Response.noContent().build();
    }
}
