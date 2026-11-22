package tareas.api;

import tareas.dominio.TareaException;
import tareas.dominio.TareaNoEncontradaException;
import tareas.dominio.TransicionInvalidaException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * QUÉ ES
 * Convierte los errores de negocio (TareaException y sus subclases) en
 * respuestas HTTP con el formato de ErrorResponse.
 *
 * CÓMO FUNCIONA
 * @Provider registra la clase en JAX-RS. Cuando un recurso lanza una excepción
 * que es (o hereda de) TareaException, JAX-RS llama a toResponse() en lugar de
 * devolver un error 500 genérico.
 *
 * POR QUÉ ASÍ Y NO CON try/catch EN CADA ENDPOINT
 * Se escribe una sola vez, y los controladores quedan limpios: solo describen
 * el caso feliz.
 */
@Provider
public class TareaExceptionMapper implements ExceptionMapper<TareaException> {

    @Override
    public Response toResponse(TareaException e) {
        // Paso 1: elegir el código HTTP según el tipo de excepción.
        // Es un switch con pattern matching (Java 21): "case Tipo variable"
        // pregunta "¿e es de este tipo?". Como TareaException es sealed, el
        // compilador sabe que solo existen estas dos subclases y no exige
        // "default". Si mañana se agrega una tercera, esto deja de compilar
        // hasta decidir qué código le corresponde. Imposible olvidarse.
        // (La variable "ignored" no se usa; en Java 22+ se podría escribir "_".)
        Status status = switch (e) {
            case TareaNoEncontradaException ignored -> Status.NOT_FOUND;   // 404
            case TransicionInvalidaException ignored -> Status.CONFLICT;   // 409
        };

        // Paso 2: armar la respuesta JSON con el formato común de errores
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(status.getStatusCode(), status.getReasonPhrase(), e.getMessage()))
                .build();
    }
}
