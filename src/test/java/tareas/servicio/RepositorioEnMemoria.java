package tareas.servicio;

import tareas.dominio.Estado;
import tareas.dominio.Tarea;
import tareas.repositorio.TareaRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QUÉ ES
 * Una implementación "falsa" de TareaRepository que guarda las tareas en un
 * Map en memoria en lugar de MySQL. En la jerga de testing se llama "fake".
 *
 * POR QUÉ EXISTE
 * Para testear TareaService sin base de datos ni servidor. Como el servicio
 * depende de la interfaz TareaRepository (y no de JPA), en los tests le
 * pasamos esta clase y el servicio ni se entera de la diferencia.
 * Resultado: los tests corren en milisegundos y no dependen de nada externo.
 */
class RepositorioEnMemoria implements TareaRepository {

    /** Las tareas guardadas, por id. LinkedHashMap mantiene el orden de inserción. */
    private final Map<Long, Tarea> datos = new LinkedHashMap<>();

    /** Contador que imita al AUTO_INCREMENT de MySQL. */
    private final AtomicLong secuencia = new AtomicLong();

    @Override
    public Tarea guardar(Tarea tarea) {
        // Si es nueva, le asignamos el siguiente id (lo que haría la base)
        if (tarea.getId() == null) {
            tarea.asignarId(secuencia.incrementAndGet());
        }
        datos.put(tarea.getId(), tarea);
        return tarea;
    }

    @Override
    public Optional<Tarea> buscarPorId(long id) {
        return Optional.ofNullable(datos.get(id));
    }

    /** Imita el comportamiento de JpaTareaRepository: filtra por estado y ordena del id más alto al más bajo. */
    @Override
    public List<Tarea> listar(Optional<Estado> estado) {
        return datos.values().stream()
                // Sin filtro (Optional vacío) → orElse(true) deja pasar todas
                .filter(t -> estado.map(e -> t.getEstado() == e).orElse(true))
                .sorted(Comparator.comparing(Tarea::getId).reversed())
                .toList();
    }

    @Override
    public void eliminar(Tarea tarea) {
        datos.remove(tarea.getId());
    }
}
