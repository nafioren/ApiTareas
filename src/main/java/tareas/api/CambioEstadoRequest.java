package tareas.api;

import tareas.dominio.Estado;
import jakarta.validation.constraints.NotNull;

/**
 * QUÉ ES
 * El cuerpo del pedido PATCH /api/tareas/{id}/estado. Ejemplo:
 * <pre>
 * {"estado": "EN_CURSO"}
 * </pre>
 *
 * POR QUÉ UN DTO SEPARADO Y NO REUSAR TareaRequest
 * Cambiar el estado es una operación distinta de editar los datos, con sus
 * propias reglas. Tener un DTO por operación deja claro qué campos se pueden
 * mandar en cada caso. Si llegara un texto que no es un Estado válido
 * (por ejemplo "TERMINADA"), la conversión falla y se responde 400.
 */
public record CambioEstadoRequest(
        @NotNull(message = "el estado es obligatorio")
        Estado estado) {
}
