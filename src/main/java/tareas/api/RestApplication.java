package tareas.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * QUÉ ES
 * El punto de arranque de la API REST (Jakarta REST, antes llamado JAX-RS).
 *
 * POR QUÉ ESTÁ VACÍA
 * Con solo existir y tener @ApplicationPath, WildFly activa JAX-RS y busca
 * automáticamente todas las clases con @Path (los recursos) y @Provider (los
 * mapeadores de errores). No hace falta registrarlas a mano ni escribir un
 * web.xml.
 *
 * "/api" es el prefijo de todas las rutas: TareaResource tiene @Path("/tareas"),
 * así que sus URLs quedan en /api/tareas.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
