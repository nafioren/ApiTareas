package tareas.dominio;

/**
 * QUÉ ES
 * El error que se lanza cuando se intenta un cambio de estado no permitido,
 * por ejemplo de PENDIENTE directo a HECHA.
 *
 * DÓNDE SE LANZA Y QUÉ PASA DESPUÉS
 * La lanza Tarea.cambiarEstado(). TareaExceptionMapper la convierte en un
 * HTTP 409 Conflict ("tu pedido choca con el estado actual del recurso").
 */
public final class TransicionInvalidaException extends TareaException {

    private static final long serialVersionUID = 1L;

    public TransicionInvalidaException(Estado desde, Estado hacia) {
        // String.formatted() es de Java 15: equivale a String.format(texto, desde, hacia)
        super("No se puede pasar de %s a %s".formatted(desde, hacia));
    }
}
