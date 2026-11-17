package api.astro.whats_orders_manager.modules.facturacion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * T-036: certificado PKCS12 de prueba (autofirmado, generado con keytool) para que las pruebas de integración firmen
 * con FirmaDigitalServiceImpl real. Nunca es el certificado de producción. Se genera una vez por JVM.
 */
final class CertificadoDePrueba {

    static final String PIN = "t036pin";

    private static Path p12;

    private CertificadoDePrueba() {
    }

    /** Ruta del .p12 de prueba (se crea la primera vez y se borra al terminar la JVM). */
    static synchronized String ruta() {
        if (p12 == null) {
            try {
                Path dir = Files.createTempDirectory("t036-cert-");
                Path archivo = dir.resolve("t036-prueba.p12");
                String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
                Process proceso = new ProcessBuilder(keytool, "-genkeypair", "-alias", "t036", "-keyalg", "RSA",
                        "-keysize", "2048", "-validity", "30", "-storetype", "PKCS12", "-keystore", archivo.toString(),
                        "-storepass", PIN, "-keypass", PIN,
                        "-dname", "CN=EMPRESA T036 PRUEBA, OU=CPJ, O=PERSONA JURIDICA, C=CR, SERIALNUMBER=CPJ-3-101-234567")
                        .redirectErrorStream(true).start();
                if (!proceso.waitFor(60, TimeUnit.SECONDS) || proceso.exitValue() != 0) {
                    throw new IllegalStateException("keytool falló: " + new String(proceso.getInputStream().readAllBytes()));
                }
                // deleteOnExit se ejecuta en orden inverso: primero el directorio, para que se borre al final
                dir.toFile().deleteOnExit();
                archivo.toFile().deleteOnExit();
                p12 = archivo;
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException("No se pudo generar el certificado de prueba", e);
            }
        }
        return p12.toString();
    }
}
