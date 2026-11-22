package tareas.repositorio;

import tareas.dominio.Estado;
import tareas.dominio.Tarea;

import java.util.List;
import java.util.Optional;

/**
 * QUÉ ES
 * El "contrato" de acceso a datos: qué operaciones se pueden hacer con las
 * tareas guardadas, sin decir CÓMO se guardan.
 *
 * POR QUÉ UNA INTERFAZ Y NO DIRECTAMENTE LA CLASE JPA
 * TareaService depende de esta interfaz, no de JPA. Eso permite tener dos
 * implementaciones:
 *   - JpaTareaRepository: la real, que habla con MySQL.
 *   - RepositorioEnMemoria (en los tests): un Map en memoria.
 * Gracias a eso los tests del servicio corren en milisegundos sin levantar
 * ni la base ni el servidor. Esto es el principio de "inversión de
 * dependencias": el código de negocio no depende de los detalles técnicos.
 *
 * POR QUÉ Optional
 * buscarPorId devuelve Optional en lugar de null. Así el que llama está
 * obligado a pensar qué hacer si la tarea no existe, y se evitan los
 * NullPointerException.
 */
public interface TareaRepository {

    /** Inserta la tarea si es nueva (sin id) o la actualiza si ya existe. */
    Tarea guardar(Tarea tarea);

    Optional<Tarea> buscarPorId(long id);

    /** Lista todas, o solo las de un estado si "estado" viene con valor. */
    List<Tarea> listar(Optional<Estado> estado);

    void eliminar(Tarea tarea);
}
