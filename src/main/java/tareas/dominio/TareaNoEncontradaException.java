package tareas.dominio;

/**
 * QUÉ ES
 * El error que se lanza cuando se pide una tarea con un id que no existe.
 *
 * DÓNDE SE LANZA Y QUÉ PASA DESPUÉS
 * La lanza TareaService.obtener(). Sube sin ser atrapada hasta la capa REST,
 * donde TareaExceptionMapper la convierte en un HTTP 404 Not Found.
 *
 * "final" porque es una de las subclases permitidas de la clase sellada
 * TareaException: las subclases de una sealed tienen que ser final, sealed o
 * non-sealed.
 */
public final class TareaNoEncontradaException extends TareaException {

    private static final long serialVersionUID = 1L;

    public TareaNoEncontradaException(long id) {
        super("No existe la tarea con id " + id);
    }
}
