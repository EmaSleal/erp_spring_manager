package api.astro.whats_orders_manager.modules.compras.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adjunto del comprobante de una compra de contado (T-058, ADR-077 §7). Valida extensión (xml, pdf, jpg, jpeg, png), tamaño
 * (configurable con {@code app.compras.adjunto.tamano-maximo}, 5 MB por defecto) y los primeros bytes del contenido; guarda en
 * {@code <uploads>/cxp/<id>/<UUID>.<ext>} —el nombre sale de {@link UUID#randomUUID()} (SecureRandom), nunca del cliente— y
 * resuelve la descarga solo desde la ruta guardada en la CxP, sin ninguna entrada del usuario en la ruta.
 */
@Slf4j
@Service
public class AdjuntoComprobanteService {

    public static final String MSG_EXTENSION = "El adjunto debe ser un archivo XML, PDF, JPG o PNG";
    public static final String MSG_CONTENIDO = "El contenido del adjunto no corresponde a su extensión";
    public static final String MSG_NO_GUARDADO = "No se pudo guardar el adjunto del comprobante";

    static final String SUBDIRECTORIO = "cxp";
    private static final int MAX_NOMBRE_ORIGINAL = 255;
    private static final Map<String, String> TIPOS = Map.of(
        "xml", "application/xml", "pdf", "application/pdf", "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png");

    /** Archivo a servir con el nombre original (solo para el encabezado de descarga) y el tipo derivado de la extensión. */
    public record Descarga(Path archivo, String nombreOriginal, String tipo) {
    }

    private final CuentaPorPagarRepository cuentaRepository;
    private final Path base;
    private final DataSize tamanoMaximo;

    public AdjuntoComprobanteService(CuentaPorPagarRepository cuentaRepository,
                                     @Value("${app.uploads.directorio}") String uploadsDir,
                                     @Value("${app.compras.adjunto.tamano-maximo:5MB}") DataSize tamanoMaximo) {
        this.cuentaRepository = cuentaRepository;
        this.base = Paths.get(uploadsDir).toAbsolutePath().normalize();
        this.tamanoMaximo = tamanoMaximo;
    }

    /** Valida el archivo recibido; null o vacío = no hay adjunto. 400 (IllegalArgumentException) con mensaje fijo si no cumple. */
    public AdjuntoComprobante validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        String original = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        String extension = extensionDe(original);
        if (extension == null || !TIPOS.containsKey(extension)) {
            throw new IllegalArgumentException(MSG_EXTENSION);
        }
        if (archivo.getSize() > tamanoMaximo.toBytes()) {
            throw new IllegalArgumentException(mensajeTamano());
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException(MSG_CONTENIDO);
        }
        if (contenido.length > tamanoMaximo.toBytes()) {
            throw new IllegalArgumentException(mensajeTamano());
        }
        if (!contenidoCorresponde(extension, contenido)) {
            throw new IllegalArgumentException(MSG_CONTENIDO);
        }
        return new AdjuntoComprobante(contenido, nombreSeguro(original), extension, TIPOS.get(extension));
    }

    /**
     * Escribe el archivo en {@code cxp/<id>/<UUID>.<ext>} (sin sobrescribir) y devuelve la ruta relativa a guardar en la CxP.
     * Si la escritura falla no deja un archivo a medias y lanza IllegalStateException.
     */
    public String guardar(Long cuentaPorPagarId, AdjuntoComprobante adjunto) {
        String relativa = SUBDIRECTORIO + "/" + cuentaPorPagarId + "/" + UUID.randomUUID() + "." + adjunto.extension();
        Path destino = base.resolve(relativa).normalize();
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, adjunto.contenido(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("No se pudo guardar el adjunto de la CxP {}: {}", cuentaPorPagarId, e.getMessage());
            borrarSilencioso(destino);
            throw new IllegalStateException(MSG_NO_GUARDADO, e);
        }
        return relativa;
    }

    /** Borra un archivo guardado (reversión de la transacción); nunca lanza. */
    public void eliminar(String rutaRelativa) {
        Path p = resolverSeguro(rutaRelativa);
        if (p != null) {
            borrarSilencioso(p);
        }
    }

    /** Adjunto de la CxP, si lo tiene y su ruta guardada está dentro de uploads/cxp y existe; vacío en cualquier otro caso. */
    public Optional<Descarga> cargar(Long cuentaPorPagarId) {
        Optional<CuentaPorPagar> cuenta = cuentaRepository.findById(cuentaPorPagarId);
        if (cuenta.isEmpty() || cuenta.get().getAdjuntoRuta() == null) {
            return Optional.empty();
        }
        Path archivo = resolverSeguro(cuenta.get().getAdjuntoRuta());
        String tipo = archivo == null ? null : TIPOS.get(extensionDe(archivo.getFileName().toString()));
        if (archivo == null || tipo == null || !Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        String nombre = cuenta.get().getAdjuntoNombreOriginal() != null
            ? cuenta.get().getAdjuntoNombreOriginal() : archivo.getFileName().toString();
        return Optional.of(new Descarga(archivo, nombre, tipo));
    }

    private Path resolverSeguro(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return null;
        }
        try {
            Path p = base.resolve(rutaRelativa).normalize();
            return p.startsWith(base.resolve(SUBDIRECTORIO)) ? p : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void borrarSilencioso(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("No se pudo borrar el adjunto {}: {}", p.getFileName(), e.getMessage());
        }
    }

    private static String extensionDe(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return null;
        }
        return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    /** Solo el nombre base, sin caracteres de control ni separadores de ruta; se usa para mostrar y nombrar la descarga. */
    private static String nombreSeguro(String original) {
        String n = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1)
            .replaceAll("\\p{Cntrl}", "").trim();
        return n.length() > MAX_NOMBRE_ORIGINAL ? n.substring(n.length() - MAX_NOMBRE_ORIGINAL) : n;
    }

    private String mensajeTamano() {
        long bytes = tamanoMaximo.toBytes();
        String limite = bytes % (1024 * 1024) == 0 ? (bytes / (1024 * 1024)) + " MB"
            : bytes % 1024 == 0 ? (bytes / 1024) + " KB" : bytes + " bytes";
        return "El adjunto excede el tamaño máximo permitido de " + limite;
    }

    private static boolean contenidoCorresponde(String extension, byte[] c) {
        switch (extension) {
            case "pdf":
                return empieza(c, 0, '%', 'P', 'D', 'F');
            case "png":
                return empieza(c, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            case "jpg":
            case "jpeg":
                return empieza(c, 0, 0xFF, 0xD8, 0xFF);
            case "xml": {
                int i = empieza(c, 0, 0xEF, 0xBB, 0xBF) ? 3 : 0;
                while (i < c.length && (c[i] == ' ' || c[i] == '\t' || c[i] == '\r' || c[i] == '\n')) {
                    i++;
                }
                return i < c.length && c[i] == '<';
            }
            default:
                return false;
        }
    }

    private static boolean empieza(byte[] c, int desde, int... esperado) {
        if (c.length < desde + esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if ((c[desde + i] & 0xFF) != esperado[i]) {
                return false;
            }
        }
        return true;
    }
}
