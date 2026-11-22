package tareas.api;

/**
 * QUÉ ES
 * El formato JSON de todos los errores que devuelve la API. Ejemplo:
 * <pre>
 * {"status": 409, "error": "Conflict", "mensaje": "No se puede pasar de PENDIENTE a HECHA"}
 * </pre>
 *
 * POR QUÉ UN FORMATO ÚNICO
 * Si cada error tuviera una forma distinta, quien consume la API (un front,
 * otra app) tendría que manejar cada caso por separado. Con un formato fijo,
 * lee siempre los mismos campos.
 *
 * QUIÉN LO USA
 * TareaExceptionMapper y ErrorInesperadoMapper.
 */
public record ErrorResponse(int status, String error, String mensaje) {
}
