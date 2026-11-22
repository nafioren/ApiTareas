package tareas.dominio;

/**
 * QUÉ ES
 * Los estados posibles de una tarea y las reglas para pasar de uno a otro.
 *
 * <pre>
 * PENDIENTE ──► EN_CURSO ──► HECHA
 *     │  ◄────────┘  │
 *     └──► CANCELADA ◄┘
 * </pre>
 *
 * POR QUÉ ES UN ENUM
 * Un enum limita los valores a una lista cerrada: es imposible tener una tarea
 * en estado "TERMINADA" o "pendiente" (con minúscula) por un error de tipeo,
 * cosa que sí pasaría si el estado fuera un String.
 *
 * POR QUÉ LA REGLA VIVE ACÁ
 * Quien mejor sabe qué transiciones son válidas es el propio estado. Si esta
 * lógica estuviera en el controlador o en el servicio, habría que repetirla en
 * cada lugar que cambie un estado.
 *
 * QUIÉN LA USA
 * {@link Tarea#cambiarEstado(Estado)} la consulta antes de cambiar el estado.
 */
public enum Estado {
    PENDIENTE,
    EN_CURSO,
    HECHA,
    CANCELADA;

    /**
     * Responde si desde este estado ("this") se puede pasar a "destino".
     *
     * Es una switch expression (Java 14+): devuelve un valor directamente, sin
     * "break" ni variables auxiliares. Además, como no tiene "default", el
     * compilador obliga a cubrir todos los estados: si mañana se agrega uno
     * nuevo, esto no compila hasta definir su regla. Así no se olvida nunca.
     */
    public boolean puedePasarA(Estado destino) {
        return switch (this) {
            // Una tarea pendiente se puede empezar o cancelar (no terminar sin empezarla)
            case PENDIENTE -> destino == EN_CURSO || destino == CANCELADA;
            // Una tarea en curso se puede terminar, pausar (volver a pendiente) o cancelar
            case EN_CURSO -> destino == HECHA || destino == PENDIENTE || destino == CANCELADA;
            // HECHA y CANCELADA son estados finales: ya no se mueven
            case HECHA, CANCELADA -> false;
        };
    }
}
