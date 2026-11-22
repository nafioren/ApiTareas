package tareas.dominio;

/**
 * QUÉ ES
 * La prioridad de una tarea.
 *
 * POR QUÉ ES UN ENUM
 * Igual que {@link Estado}: los valores posibles son una lista cerrada, y con
 * un enum el compilador y la base de datos rechazan cualquier otro valor.
 * En la base se guarda como texto ("ALTA"), ver {@link Tarea}.
 */
public enum Prioridad {
    BAJA,
    MEDIA,
    ALTA
}
