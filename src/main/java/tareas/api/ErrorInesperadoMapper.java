package tareas.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * QUÉ ES
 * La "red de seguridad": atrapa cualquier error que no haya manejado otro
 * mapeador (un bug, la base caída, etc.).
 *
 * CÓMO SABE JAX-RS CUÁL MAPEADOR USAR
 * Elige el más específico. Para una TransicionInvalidaException usa
 * TareaExceptionMapper (más cercano en la jerarquía); para todo lo demás
 * cae acá, porque Throwable es la raíz de todas las excepciones.
 *
 * POR QUÉ ES IMPORTANTE PARA LA SEGURIDAD
 * Sin esto, el servidor podría devolver el stack trace completo al cliente:
 * nombres de clases, versiones de librerías, a veces hasta consultas SQL.
 * Para un atacante es un mapa del sistema. Acá el detalle queda en el log del
 * servidor y al cliente solo le llega un código para reportarlo.
 */
@Provider
public class ErrorInesperadoMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ErrorInesperadoMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {
        // Caso 1: errores HTTP "normales" que lanza el propio JAX-RS
        // (ruta inexistente → 404, verbo no soportado → 405, JSON mal formado → 400).
        // No son bugs: se respeta su código, pero con nuestro formato y un mensaje
        // genérico, sin detalles internos.
        // "instanceof WebApplicationException wae" es pattern matching (Java 16):
        // chequea el tipo y declara la variable ya casteada en un solo paso.
        if (e instanceof WebApplicationException wae) {
            var original = wae.getResponse();
            var status = Status.fromStatusCode(original.getStatus());
            var frase = status != null ? status.getReasonPhrase() : "Error";
            return Response.status(original.getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorResponse(original.getStatus(), frase, mensajeSeguro(original.getStatus())))
                    .build();
        }

        // Caso 2: un error de verdad inesperado.
        // Paso 1: generar un código único para este error
        var codigo = UUID.randomUUID().toString();
        // Paso 2: guardar TODO el detalle (con stack trace) en el log del servidor
        LOG.log(Level.SEVERE, "Error inesperado [" + codigo + "]", e);
        // Paso 3: al cliente, solo un mensaje genérico con el código.
        // Si un usuario reporta "me salió el código abc-123", se busca en el log.
        return Response.status(Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(500, "Internal Server Error",
                        "Ocurrió un error inesperado. Código: " + codigo))
                .build();
    }

    /** Mensajes fijos y genéricos según el código: nunca el texto original de la excepción. */
    private static String mensajeSeguro(int status) {
        return switch (status) {
            case 400 -> "La solicitud no es válida";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 415 -> "Tipo de contenido no soportado";
            default -> "Error en la solicitud";
        };
    }
}
