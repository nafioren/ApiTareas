package tareas.api;

import tareas.dominio.Estado;
import tareas.dominio.Prioridad;
import tareas.dominio.Tarea;

import java.time.Instant;

/**
 * QUÉ ES
 * Lo que la API devuelve cuando responde con una tarea. Se convierte a JSON
 * automáticamente.
 *
 * POR QUÉ NO DEVOLVER DIRECTAMENTE LA ENTIDAD Tarea
 * 1. Seguridad: la entidad tiene campos internos (como "version") que el
 *    cliente no necesita ver. Con un DTO elegimos exactamente qué sale.
 * 2. Estabilidad: si mañana se renombra una columna, el JSON de la API no
 *    cambia y los clientes no se rompen.
 * 3. Evita problemas de JPA al convertir entidades a JSON (por ejemplo, con
 *    relaciones que se cargan de forma diferida).
 */
public record TareaResponse(
        long id,
        String titulo,
        String descripcion,
        Estado estado,
        Prioridad prioridad,
        Instant creadaEn,
        Instant actualizadaEn) {

    /**
     * "Método fábrica": convierte una entidad en su DTO. Tenerlo acá evita
     * repetir esta conversión en cada endpoint.
     * Se usa así: TareaResponse.desde(tarea), o como TareaResponse::desde en un stream.
     */
    public static TareaResponse desde(Tarea t) {
        return new TareaResponse(
                t.getId(), t.getTitulo(), t.getDescripcion(),
                t.getEstado(), t.getPrioridad(),
                t.getCreadaEn(), t.getActualizadaEn());
    }
}
