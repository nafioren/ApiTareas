package tareas.dominio;

/**
 * QUÉ ES
 * La "madre" de todos los errores de negocio de las tareas.
 *
 * POR QUÉ ES "SEALED" (SELLADA, Java 17+)
 * "permits" lista las únicas subclases que pueden existir. Nadie más puede
 * heredar de esta clase. ¿Para qué sirve? En TareaExceptionMapper hay un switch
 * sobre el tipo de excepción, y como el compilador conoce la lista completa,
 * puede verificar que el switch cubra todos los casos sin necesitar "default".
 *
 * POR QUÉ HEREDA DE RuntimeException
 * Las RuntimeException no obligan a escribir "throws" ni try/catch en cada
 * método intermedio. El error sube solo hasta la capa REST, donde se convierte
 * en una respuesta HTTP. Además, en una transacción (@Transactional), una
 * RuntimeException hace rollback automáticamente.
 *
 * POR QUÉ ES "abstract"
 * No tiene sentido lanzar un "error de tarea" genérico: siempre se lanza una
 * subclase concreta que dice qué pasó.
 */
public abstract sealed class TareaException extends RuntimeException
        permits TareaNoEncontradaException, TransicionInvalidaException {

    // Las excepciones son Serializable; este número identifica la versión de la
    // clase. Se declara para que el compilador no lo advierta con -Xlint:all.
    private static final long serialVersionUID = 1L;

    protected TareaException(String mensaje) {
        super(mensaje);
    }
}
