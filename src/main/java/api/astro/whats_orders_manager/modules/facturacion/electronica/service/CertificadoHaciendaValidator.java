package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.CertificadoInvalidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

/**
 * T-035 (P-035 punto 1): abre el keystore PKCS12 con su PIN al configurar el certificado, para que un PIN incorrecto,
 * un archivo corrupto, un certificado sin clave privada o vencido se rechacen al guardar y no al firmar la primera
 * factura. Los mensajes son fijos por causa; nunca llevan el texto de la excepción, y el PIN no se escribe en el log.
 */
@Component
@Slf4j
public class CertificadoHaciendaValidator {

    public enum Causa {
        PIN_O_ARCHIVO("El PIN es incorrecto o el archivo del certificado no se puede leer"),
        SIN_CLAVE_PRIVADA("El certificado no contiene una clave privada"),
        VENCIDO_O_NO_VIGENTE("El certificado está vencido o aún no es vigente");

        private final String mensaje;

        Causa(String mensaje) {
            this.mensaje = mensaje;
        }

        public String mensaje() {
            return mensaje;
        }
    }

    /** Datos del certificado válido. {@code identificacion}: dígitos del SERIALNUMBER del subject (puede ser vacío). */
    public record InfoCertificado(String fechaVencimiento, String titular, String identificacion) {
    }

    private static final Map<String, String> OIDS = Map.of("2.5.4.5", "SERIALNUMBER");

    /**
     * @throws CertificadoInvalidoException con la causa; sin efectos secundarios
     */
    public InfoCertificado validar(String ruta, String pin) {
        return validar(ruta, pin, Instant.now());
    }

    InfoCertificado validar(String ruta, String pin, Instant ahora) {
        KeyStore ks;
        try (InputStream in = Files.newInputStream(Path.of(ruta))) {
            ks = KeyStore.getInstance("PKCS12");
            ks.load(in, pin == null ? new char[0] : pin.toCharArray());
        } catch (Exception e) {
            throw rechazar(Causa.PIN_O_ARCHIVO, ruta);
        }
        char[] clave = pin == null ? new char[0] : pin.toCharArray();
        X509Certificate cert = null;
        try {
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements() && cert == null) {
                String alias = aliases.nextElement();
                if (!ks.isKeyEntry(alias)) {
                    continue;
                }
                Key key = ks.getKey(alias, clave);
                if (key != null && ks.getCertificate(alias) instanceof X509Certificate x) {
                    cert = x;
                }
            }
        } catch (Exception e) {
            throw rechazar(Causa.PIN_O_ARCHIVO, ruta);
        }
        if (cert == null) {
            throw rechazar(Causa.SIN_CLAVE_PRIVADA, ruta);
        }
        try {
            cert.checkValidity(Date.from(ahora));
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            throw rechazar(Causa.VENCIDO_O_NO_VIGENTE, ruta);
        }
        LocalDate vence = cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        X500Principal subject = cert.getSubjectX500Principal();
        String serial = valorRdn(subject, "SERIALNUMBER");
        return new InfoCertificado(vence.toString(), valorRdn(subject, "CN"),
                serial == null ? "" : serial.replaceAll("\\D", ""));
    }

    private static String valorRdn(X500Principal subject, String tipo) {
        try {
            for (Rdn rdn : new LdapName(subject.getName(X500Principal.RFC2253, OIDS)).getRdns()) {
                if (tipo.equalsIgnoreCase(rdn.getType())) {
                    return String.valueOf(rdn.getValue());
                }
            }
        } catch (InvalidNameException e) {
            // sin titular legible: se devuelve null
        }
        return null;
    }

    private static CertificadoInvalidoException rechazar(Causa causa, String ruta) {
        log.warn("Certificado rechazado: causa={} ruta={}", causa, ruta);
        return new CertificadoInvalidoException(causa);
    }
}
