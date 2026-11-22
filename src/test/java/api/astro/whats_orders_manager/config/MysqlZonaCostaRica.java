package api.astro.whats_orders_manager.config;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.Map;

/**
 * Configuración compartida de los MySQL de Testcontainers (T-050, ADR-060): el servidor corre en America/Costa_Rica,
 * igual que producción y el smoke. Las imágenes oficiales de mysql cargan las tablas de zonas con nombre al arrancar.
 * Cada prueba conserva su base y el nombre de su contenedor (sin volúmenes: tmpfs).
 */
public final class MysqlZonaCostaRica {

    public static final String ZONA = "America/Costa_Rica";
    public static final String IMAGEN = "mysql:8.0.32";

    private MysqlZonaCostaRica() {
    }

    /** Contenedor sin rutinas almacenadas (pruebas de migración sobre tablas mínimas). */
    @SuppressWarnings("resource")
    public static MySQLContainer<?> contenedor(String base, String nombreContenedor) {
        return new MySQLContainer<>(IMAGEN)
                .withDatabaseName(base)
                .withTmpFs(Map.of("/var/lib/mysql", "rw"))
                .withCommand("--default-time-zone=" + ZONA)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(nombreContenedor));
    }

    /** Contenedor con los procedimientos de database/routines (pruebas con el contexto completo). */
    public static MySQLContainer<?> contenedorConRutinas(String base, String nombreContenedor) {
        return contenedor(base, nombreContenedor)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(Path.of("database/routines/stored_procedures.sql")),
                        "/docker-entrypoint-initdb.d/01-routines.sql");
    }
}
