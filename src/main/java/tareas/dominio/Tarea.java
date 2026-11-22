package tareas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import tareas.repositorio.JpaTareaRepository;
import tareas.servicio.TareaService;

import java.time.Instant;
import java.util.Objects;

/**
 * QUÉ ES
 * La entidad principal del sistema: una tarea. Cada objeto Tarea corresponde
 * a una fila de la tabla "tarea" en MySQL.
 *
 * CÓMO SE CONECTA CON LA BASE
 * Las anotaciones de JPA (@Entity, @Column, etc.) le dicen a Hibernate (la
 * implementación de JPA que trae WildFly) cómo convertir este objeto en una
 * fila y al revés. Nosotros nunca escribimos un INSERT o un UPDATE a mano.
 *
 * POR QUÉ TIENE MÉTODOS Y NO SOLO GETTERS/SETTERS
 * No tiene setters públicos a propósito. Si los tuviera, cualquier parte del
 * código podría hacer tarea.setEstado(HECHA) salteándose las reglas. En cambio,
 * para modificarla hay que usar métodos con nombre de negocio
 * (cambiarEstado, actualizar) que validan antes de cambiar.
 * Esto se llama "encapsulamiento": el objeto protege su propia consistencia.
 *
 * QUIÉN LA USA
 * {@link TareaService} la crea y modifica;
 * {@link JpaTareaRepository} la guarda.
 */
@Entity                  // "esta clase se guarda en la base"
@Table(name = "tarea")   // nombre de la tabla (ver db/schema.sql)
public class Tarea {

    @Id                                                   // clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // el id lo genera MySQL (AUTO_INCREMENT)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    // EnumType.STRING guarda "PENDIENTE" en la base. La alternativa (ORDINAL)
    // guarda 0, 1, 2... y si alguien reordena el enum, los datos viejos pasan
    // a significar otra cosa. Por eso siempre STRING.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;   // toda tarea nueva arranca pendiente

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridad prioridad = Prioridad.MEDIA;

    // Instant = un momento exacto en UTC, sin problemas de zona horaria.
    // Reemplaza a java.util.Date (ver MIGRACION.md).
    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en", nullable = false)
    private Instant actualizadaEn;

    /**
     * Bloqueo optimista. Hibernate suma 1 a este número en cada UPDATE y
     * verifica que nadie lo haya cambiado mientras tanto.
     *
     * Ejemplo: Ana y Beto abren la misma tarea (versión 3). Ana guarda primero
     * (pasa a versión 4). Cuando Beto guarda, Hibernate ve que ya no es la 3 y
     * rechaza el cambio, en lugar de pisar silenciosamente lo que hizo Ana.
     */
    @Version
    private long version;

    /**
     * Constructor vacío: JPA lo necesita para crear el objeto al leer de la base.
     * Es "protected" para que el resto del código no lo use y no pueda crear
     * tareas sin título.
     */
    protected Tarea() {
    }

    /** El constructor que usa nuestro código: obliga a pasar un título. */
    public Tarea(String titulo, String descripcion, Prioridad prioridad) {
        // requireNonNull corta en el acto si llega null, con un mensaje claro,
        // en vez de dejar que el error aparezca más tarde en la base.
        this.titulo = Objects.requireNonNull(titulo, "titulo");
        this.descripcion = descripcion;
        this.prioridad = prioridad != null ? prioridad : Prioridad.MEDIA;
    }

    /**
     * Hibernate llama a este método automáticamente justo antes del INSERT.
     * Así las fechas se completan siempre, sin depender de que alguien se acuerde.
     */
    @PrePersist
    void alCrear() {
        creadaEn = Instant.now();
        actualizadaEn = creadaEn;
    }

    /** Hibernate lo llama automáticamente justo antes de cada UPDATE. */
    @PreUpdate
    void alActualizar() {
        actualizadaEn = Instant.now();
    }

    /**
     * Cambia el estado respetando las reglas de {@link Estado#puedePasarA}.
     *
     * @throws TransicionInvalidaException si el cambio no está permitido.
     *         El controlador la convierte en un HTTP 409 (ver TareaExceptionMapper).
     */
    public void cambiarEstado(Estado nuevo) {
        // Paso 1: preguntarle al estado actual si permite el cambio
        if (!estado.puedePasarA(nuevo)) {
            // Paso 2a: si no, cortar con una excepción de negocio
            throw new TransicionInvalidaException(estado, nuevo);
        }
        // Paso 2b: si sí, cambiar el valor en memoria. El UPDATE en la base lo
        // hace Hibernate cuando TareaService termina la transacción.
        estado = nuevo;
    }

    /** Actualiza los datos editables. La prioridad solo cambia si viene informada. */
    public void actualizar(String titulo, String descripcion, Prioridad prioridad) {
        this.titulo = Objects.requireNonNull(titulo, "titulo");
        this.descripcion = descripcion;
        if (prioridad != null) {
            this.prioridad = prioridad;
        }
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public Estado getEstado() { return estado; }
    public Prioridad getPrioridad() { return prioridad; }
    public Instant getCreadaEn() { return creadaEn; }
    public Instant getActualizadaEn() { return actualizadaEn; }

    /**
     * Solo para tests y para el repositorio en memoria (RepositorioEnMemoria),
     * que no tienen una base que genere el id.
     */
    public void asignarId(long id) { this.id = id; }
}
