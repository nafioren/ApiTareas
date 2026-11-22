package tareas.repositorio;

import tareas.dominio.Estado;
import tareas.dominio.Tarea;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

/**
 * QUÉ ES
 * La implementación real de {@link TareaRepository}: guarda y lee tareas de
 * MySQL usando JPA.
 *
 * CÓMO FUNCIONA
 * Todo pasa por el EntityManager, el objeto central de JPA. Le pedimos cosas
 * como "guardá este objeto" o "buscá la tarea 5" y él genera el SQL.
 *
 * QUIÉN LA CREA
 * Nadie hace "new JpaTareaRepository()". @ApplicationScoped la convierte en
 * un bean de CDI: WildFly crea UNA sola instancia para toda la aplicación y la
 * inyecta donde se pida un TareaRepository (en TareaService).
 */
@ApplicationScoped
public class JpaTareaRepository implements TareaRepository {

    /**
     * WildFly inyecta el EntityManager configurado en persistence.xml
     * (unidad "tareas", que apunta al datasource de MySQL).
     */
    @PersistenceContext(unitName = "tareas")
    private EntityManager em;

    @Override
    public Tarea guardar(Tarea tarea) {
        // Sin id = tarea nueva → persist genera un INSERT y MySQL asigna el id
        if (tarea.getId() == null) {
            em.persist(tarea);
            return tarea;
        }
        // Con id = tarea existente → merge sincroniza los cambios (UPDATE).
        // Si la tarea ya se leyó en esta misma transacción, Hibernate ya la
        // está siguiendo y el UPDATE ocurre igual al terminar la transacción.
        return em.merge(tarea);
    }

    @Override
    public Optional<Tarea> buscarPorId(long id) {
        // find devuelve null si no existe; ofNullable lo convierte en Optional vacío
        return Optional.ofNullable(em.find(Tarea.class, id));
    }

    /**
     * Las consultas están escritas en JPQL: parece SQL, pero habla de clases
     * y atributos Java ("Tarea t", "t.estado") en lugar de tablas y columnas.
     *
     * SEGURIDAD: ":estado" es un parámetro con nombre. JPA lo envía a la base
     * como un PreparedStatement, separado del texto de la consulta. Por eso es
     * imposible una inyección SQL acá, aunque el valor viniera del usuario.
     */
    @Override
    public List<Tarea> listar(Optional<Estado> estado) {
        return estado
                // Si viene un estado: consulta filtrada
                .map(e -> em.createQuery(
                                "SELECT t FROM Tarea t WHERE t.estado = :estado ORDER BY t.id DESC", Tarea.class)
                        .setParameter("estado", e)
                        .getResultList())
                // Si no: todas. orElseGet (y no orElse) para que esta consulta
                // solo se ejecute cuando de verdad no hay filtro.
                .orElseGet(() -> em.createQuery(
                                "SELECT t FROM Tarea t ORDER BY t.id DESC", Tarea.class)
                        .getResultList());
    }

    @Override
    public void eliminar(Tarea tarea) {
        // remove solo acepta objetos que el EntityManager esté siguiendo.
        // Si no lo está (por ejemplo, vino de otra transacción), primero merge.
        em.remove(em.contains(tarea) ? tarea : em.merge(tarea));
    }
}
