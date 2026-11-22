package tareas.servicio;

import tareas.dominio.Estado;
import tareas.dominio.Prioridad;
import tareas.dominio.Tarea;
import tareas.dominio.TareaNoEncontradaException;
import tareas.repositorio.TareaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * QUÉ ES
 * La capa de "casos de uso": cada método público es una acción que puede
 * hacer el usuario (crear una tarea, cambiarle el estado, etc.).
 *
 * QUÉ HACE Y QUÉ NO
 * - Coordina: busca la tarea, le pide que se modifique, la guarda.
 * - Maneja las transacciones.
 * - NO sabe nada de HTTP ni de JSON (eso es de TareaResource).
 * - NO decide las reglas de negocio (eso es de Tarea y Estado).
 * Separar así las capas hace que cada clase tenga una sola responsabilidad y
 * se pueda testear por separado.
 *
 * TRANSACCIONES
 * @Transactional sobre la clase aplica a todos los métodos: cada uno corre en
 * una transacción. Si termina bien → COMMIT. Si lanza una RuntimeException
 * (como TransicionInvalidaException) → ROLLBACK y no queda nada a medias en
 * la base.
 *
 * QUIÉN LA USA
 * TareaResource (la API REST) y TareaServiceTest (los tests).
 */
@ApplicationScoped
@Transactional
public class TareaService {

    private TareaRepository repositorio;

    /**
     * Constructor vacío exigido por CDI. Para aplicar @Transactional, CDI crea
     * una subclase "proxy" de este servicio que abre y cierra la transacción
     * alrededor de cada método, y para eso necesita un constructor sin
     * argumentos. Nuestro código nunca lo usa.
     */
    protected TareaService() {
    }

    /**
     * Inyección por constructor: CDI pasa automáticamente el TareaRepository
     * (JpaTareaRepository). En los tests lo llamamos a mano pasándole un
     * repositorio en memoria. Es la razón principal de preferir inyección por
     * constructor en lugar de @Inject sobre el atributo.
     */
    @Inject
    public TareaService(TareaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Tarea crear(String titulo, String descripcion, Prioridad prioridad) {
        // strip() (Java 11) saca espacios al principio y al final, incluidos
        // los espacios Unicode raros que trim() no reconoce.
        return repositorio.guardar(new Tarea(titulo.strip(), descripcion, prioridad));
    }

    public Tarea obtener(long id) {
        // Si el Optional viene vacío, se lanza la excepción → HTTP 404
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new TareaNoEncontradaException(id));
    }

    public List<Tarea> listar(Optional<Estado> estado) {
        return repositorio.listar(estado);
    }

    public Tarea actualizar(long id, String titulo, String descripcion, Prioridad prioridad) {
        // Paso 1: buscar (si no existe, 404)
        var tarea = obtener(id);
        // Paso 2: que la tarea se modifique a sí misma
        tarea.actualizar(titulo.strip(), descripcion, prioridad);
        // Paso 3: guardar. Al terminar el método, la transacción hace COMMIT.
        return repositorio.guardar(tarea);
    }

    public Tarea cambiarEstado(long id, Estado nuevo) {
        var tarea = obtener(id);
        // Si la transición no es válida, acá se lanza TransicionInvalidaException,
        // la transacción hace rollback y el controlador responde 409.
        tarea.cambiarEstado(nuevo);
        return repositorio.guardar(tarea);
    }

    public void eliminar(long id) {
        repositorio.eliminar(obtener(id));
    }
}
