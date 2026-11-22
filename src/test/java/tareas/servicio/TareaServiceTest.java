package tareas.servicio;

import tareas.dominio.Estado;
import tareas.dominio.Prioridad;
import tareas.dominio.TareaNoEncontradaException;
import tareas.dominio.TransicionInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QUÉ ES
 * Los tests unitarios de TareaService (y, de paso, de las reglas de Estado y Tarea).
 *
 * CÓMO ESTÁN ORGANIZADOS
 * - @Nested agrupa tests relacionados en clases internas (Crear, Cambios de
 *   estado, Consultas). En el reporte de JUnit se ven como secciones.
 * - Cada test sigue el patrón "Preparar / Ejecutar / Verificar" (en inglés
 *   Arrange / Act / Assert), separado por líneas en blanco.
 * - Los nombres describen el comportamiento esperado, en lenguaje natural:
 *   si un test falla, el nombre ya dice qué regla se rompió.
 *
 * SIN BASE DE DATOS
 * El servicio recibe un RepositorioEnMemoria en lugar del repositorio JPA.
 */
class TareaServiceTest {

    private TareaService servicio;

    /**
     * @BeforeEach corre antes de CADA test: cada uno arranca con un servicio y
     * un repositorio vacíos, así los tests no se afectan entre sí.
     */
    @BeforeEach
    void setUp() {
        servicio = new TareaService(new RepositorioEnMemoria());
    }

    @Nested
    @DisplayName("Crear")
    class Crear {

        @Test
        void unaTareaNuevaArrancaPendienteYConPrioridadMediaPorDefecto() {
            // Ejecutar: crear sin prioridad (null)
            var tarea = servicio.crear("Migrar módulo de pagos", null, null);

            // Verificar los valores por defecto
            assertEquals(Estado.PENDIENTE, tarea.getEstado());
            assertEquals(Prioridad.MEDIA, tarea.getPrioridad());
        }

        @Test
        void seLimpianLosEspaciosDelTitulo() {
            var tarea = servicio.crear("   Revisar logs   ", null, Prioridad.ALTA);

            assertEquals("Revisar logs", tarea.getTitulo());
        }
    }

    @Nested
    @DisplayName("Cambios de estado")
    class CambiosDeEstado {

        /**
         * Test parametrizado: el mismo test corre una vez por cada fila de
         * @CsvSource. JUnit convierte el texto "PENDIENTE" en Estado.PENDIENTE
         * automáticamente. Es más claro que escribir 5 tests casi iguales.
         */
        @ParameterizedTest(name = "{0} -> {1} está permitido")
        @CsvSource({
                "PENDIENTE, EN_CURSO",
                "PENDIENTE, CANCELADA",
                "EN_CURSO, HECHA",
                "EN_CURSO, PENDIENTE",
                "EN_CURSO, CANCELADA"
        })
        void transicionesValidas(Estado desde, Estado hacia) {
            assertTrue(desde.puedePasarA(hacia));
        }

        @ParameterizedTest(name = "{0} -> {1} no está permitido")
        @CsvSource({
                "PENDIENTE, HECHA",
                "HECHA, EN_CURSO",
                "HECHA, PENDIENTE",
                "CANCELADA, EN_CURSO"
        })
        void transicionesInvalidas(Estado desde, Estado hacia) {
            assertFalse(desde.puedePasarA(hacia));
        }

        @Test
        void noSePuedeTerminarUnaTareaQueNuncaEmpezo() {
            // Preparar
            var tarea = servicio.crear("Escribir tests", null, null);

            // Ejecutar y verificar: assertThrows comprueba que se lance la
            // excepción esperada y la devuelve para revisar el mensaje
            var error = assertThrows(TransicionInvalidaException.class,
                    () -> servicio.cambiarEstado(tarea.getId(), Estado.HECHA));
            assertEquals("No se puede pasar de PENDIENTE a HECHA", error.getMessage());
        }

        @Test
        void flujoCompleto() {
            var tarea = servicio.crear("Deploy en JBoss", null, Prioridad.ALTA);

            servicio.cambiarEstado(tarea.getId(), Estado.EN_CURSO);
            var terminada = servicio.cambiarEstado(tarea.getId(), Estado.HECHA);

            assertEquals(Estado.HECHA, terminada.getEstado());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        void filtraPorEstado() {
            // Preparar: dos tareas, solo una en curso
            var a = servicio.crear("A", null, null);
            servicio.crear("B", null, null);
            servicio.cambiarEstado(a.getId(), Estado.EN_CURSO);

            // Ejecutar
            var enCurso = servicio.listar(Optional.of(Estado.EN_CURSO));

            // Verificar
            assertEquals(1, enCurso.size());
            assertEquals("A", enCurso.getFirst().getTitulo()); // getFirst(): Sequenced Collections, Java 21
        }

        @Test
        void sinFiltroDevuelveTodasDeLaMasNuevaALaMasVieja() {
            servicio.crear("Primera", null, null);
            servicio.crear("Segunda", null, null);

            var todas = servicio.listar(Optional.empty());

            assertEquals("Segunda", todas.getFirst().getTitulo());
            assertEquals("Primera", todas.getLast().getTitulo());
        }

        @Test
        void obtenerUnaQueNoExisteLanzaNoEncontrada() {
            assertThrows(TareaNoEncontradaException.class, () -> servicio.obtener(99));
        }

        @Test
        void eliminarLaSacaDelListado() {
            var tarea = servicio.crear("Temporal", null, null);

            servicio.eliminar(tarea.getId());

            assertThrows(TareaNoEncontradaException.class, () -> servicio.obtener(tarea.getId()));
        }
    }
}
