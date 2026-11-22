package tareas.api;

import tareas.dominio.Prioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * QUÉ ES
 * Los datos que manda el cliente para crear o editar una tarea. Es un DTO
 * (Data Transfer Object): un objeto que solo sirve para transportar datos
 * entre el cliente y la API.
 *
 * Ejemplo del JSON que llega:
 * <pre>
 * {"titulo": "Migrar módulo de pagos", "descripcion": "...", "prioridad": "ALTA"}
 * </pre>
 * JAX-RS lo convierte solo en un TareaRequest (usando JSON-B).
 *
 * POR QUÉ ES UN RECORD (Java 16+)
 * Un record es una clase inmutable para datos. Esta única línea genera el
 * constructor, los métodos de acceso (titulo(), descripcion(), prioridad()),
 * equals, hashCode y toString. En Java 8 eran unas 50 líneas a mano.
 *
 * VALIDACIÓN
 * Las anotaciones (@NotBlank, @Size) son de Bean Validation. En TareaResource
 * el parámetro tiene @Valid: si el JSON no cumple estas reglas, WildFly
 * responde 400 Bad Request ANTES de llegar a nuestro código. Así el servicio
 * nunca recibe datos inválidos.
 */
public record TareaRequest(
        @NotBlank(message = "el título es obligatorio")                    // ni null, ni vacío, ni solo espacios
        @Size(max = 120, message = "el título no puede superar 120 caracteres") // coincide con la columna de la base
        String titulo,

        @Size(max = 1000, message = "la descripción no puede superar 1000 caracteres")
        String descripcion,

        Prioridad prioridad) {   // opcional: si no viene, la tarea queda en MEDIA
}
