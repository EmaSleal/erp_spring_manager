package api.astro.whats_orders_manager.models;

import api.astro.whats_orders_manager.models.enums.CanalNotificacion;
import api.astro.whats_orders_manager.models.enums.TipoNotificacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * PLANTILLA DE NOTIFICACIÓN
 * WhatsApp Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Modelo de entidad para almacenar plantillas reutilizables de notificaciones.
 * 
 * Funcionalidades:
 * - Plantillas HTML para emails
 * - Plantillas de texto plano para WhatsApp/SMS
 * - Plantillas web para notificaciones en la aplicación
 * - Soporte de variables dinámicas ({{nombreCliente}}, {{montoFactura}}, etc.)
 * - Plantillas por tipo de notificación y canal
 * - Sistema de versionado (activa/inactiva)
 * 
 * Variables disponibles:
 * - {{nombreCliente}}, {{nombreUsuario}}, {{nombreEmpresa}}
 * - {{numeroFactura}}, {{montoFactura}}, {{fechaVencimiento}}
 * - {{nombreProducto}}, {{stockActual}}, {{stockMinimo}}
 * - Cualquier variable personalizada según el tipo
 * 
 * Tabla: plantilla_notificacion
 * ============================================================================
 */
@Entity
@Table(name = "plantilla_notificacion",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_nombre_version",
            columnNames = {"nombre", "version"}
        )
    },
    indexes = {
        @Index(name = "idx_tipo_canal_activa", columnList = "tipo, canal, activa"),
        @Index(name = "idx_nombre", columnList = "nombre"),
        @Index(name = "idx_activa_predeterminada", columnList = "activa, predeterminada")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PlantillaNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla")
    private Integer idPlantilla;

    // ==================== IDENTIFICACIÓN ====================

    /**
     * Nombre descriptivo de la plantilla
     */
    @NotBlank(message = "El nombre de la plantilla es requerido")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /**
     * Código único de la plantilla (para referencia en código)
     * Ejemplo: FACTURA_CREADA_EMAIL, PAGO_RECIBIDO_WEB
     */
    @NotBlank(message = "El código de la plantilla es requerido")
    @Size(max = 50, message = "El código no puede exceder 50 caracteres")
    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    /**
     * Descripción de la plantilla (para qué se usa)
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Versión de la plantilla
     */
    @NotNull(message = "La versión es requerida")
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    // ==================== TIPO Y CANAL ====================

    /**
     * Tipo de notificación para el que sirve esta plantilla
     */
    @NotNull(message = "El tipo de notificación es requerido")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoNotificacion tipo;

    /**
     * Canal para el que está diseñada esta plantilla
     */
    @NotNull(message = "El canal es requerido")
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 30)
    private CanalNotificacion canal;

    // ==================== CONTENIDO ====================

    /**
     * Asunto/Título de la notificación
     * Puede contener variables: "Factura {{numeroFactura}} creada"
     */
    @NotBlank(message = "El asunto es requerido")
    @Size(max = 200, message = "El asunto no puede exceder 200 caracteres")
    @Column(name = "asunto", nullable = false, length = 200)
    private String asunto;

    /**
     * Contenido de la plantilla
     * - HTML para emails
     * - Texto plano para WhatsApp/SMS
     * - HTML simple para notificaciones web
     */
    @NotBlank(message = "El contenido es requerido")
    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    /**
     * Texto del botón de acción (opcional)
     * Ejemplo: "Ver Factura", "Descargar PDF"
     */
    @Column(name = "texto_boton", length = 50)
    private String textoBoton;

    /**
     * URL de acción para el botón (puede contener variables)
     * Ejemplo: "/facturas/detalle/{{idFactura}}"
     */
    @Column(name = "url_accion", length = 255)
    private String urlAccion;

    // ==================== VARIABLES ====================

    /**
     * Lista de variables disponibles en formato JSON
     * Ejemplo: ["nombreCliente", "numeroFactura", "montoTotal"]
     */
    @Column(name = "variables_disponibles", columnDefinition = "TEXT")
    private String variablesDisponibles;

    /**
     * Ejemplo de datos para previsualización (JSON)
     */
    @Column(name = "datos_ejemplo", columnDefinition = "TEXT")
    private String datosEjemplo;

    // ==================== ESTADO ====================

    /**
     * Indica si la plantilla está activa
     * Solo plantillas activas pueden ser usadas para enviar notificaciones
     */
    @NotNull(message = "El estado activo es requerido")
    @Column(name = "activa", nullable = false)
    @Builder.Default
    private Boolean activa = true;

    /**
     * Indica si es la plantilla predeterminada para este tipo/canal
     * Solo puede haber una plantilla predeterminada por tipo/canal
     */
    @NotNull(message = "El estado predeterminado es requerido")
    @Column(name = "predeterminada", nullable = false)
    @Builder.Default
    private Boolean predeterminada = false;

    /**
     * Indica si es una plantilla del sistema (no editable)
     */
    @NotNull(message = "El estado del sistema es requerido")
    @Column(name = "plantilla_sistema", nullable = false)
    @Builder.Default
    private Boolean plantillaSistema = false;

    // ==================== AUDITORÍA ====================

    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private Integer createBy;

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private Timestamp createDate;

    @LastModifiedBy
    @Column(name = "update_by")
    private Integer updateBy;

    @LastModifiedDate
    @Column(name = "update_date")
    private Timestamp updateDate;

    // ==================== MÉTODOS DE NEGOCIO ====================

    /**
     * Procesa la plantilla reemplazando las variables con valores reales
     * 
     * @param variables Mapa de variables y sus valores
     * @return Contenido procesado con variables reemplazadas
     */
    public String procesarContenido(Map<String, Object> variables) {
        String resultado = contenido;

        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String variable = "{{" + entry.getKey() + "}}";
                String valor = entry.getValue() != null ? entry.getValue().toString() : "";
                resultado = resultado.replace(variable, valor);
            }
        }

        return resultado;
    }

    /**
     * Procesa el asunto reemplazando las variables
     */
    public String procesarAsunto(Map<String, Object> variables) {
        String resultado = asunto;

        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String variable = "{{" + entry.getKey() + "}}";
                String valor = entry.getValue() != null ? entry.getValue().toString() : "";
                resultado = resultado.replace(variable, valor);
            }
        }

        return resultado;
    }

    /**
     * Procesa la URL de acción reemplazando las variables
     */
    public String procesarUrlAccion(Map<String, Object> variables) {
        if (urlAccion == null || urlAccion.isEmpty()) {
            return null;
        }

        String resultado = urlAccion;

        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String variable = "{{" + entry.getKey() + "}}";
                String valor = entry.getValue() != null ? entry.getValue().toString() : "";
                resultado = resultado.replace(variable, valor);
            }
        }

        return resultado;
    }

    /**
     * Extrae las variables utilizadas en la plantilla
     * Busca patrones como {{nombreVariable}}
     */
    public java.util.List<String> extraerVariables() {
        java.util.List<String> variables = new java.util.ArrayList<>();
        Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
        
        // Buscar en asunto
        Matcher matcherAsunto = pattern.matcher(asunto);
        while (matcherAsunto.find()) {
            String variable = matcherAsunto.group(1).trim();
            if (!variables.contains(variable)) {
                variables.add(variable);
            }
        }

        // Buscar en contenido
        Matcher matcherContenido = pattern.matcher(contenido);
        while (matcherContenido.find()) {
            String variable = matcherContenido.group(1).trim();
            if (!variables.contains(variable)) {
                variables.add(variable);
            }
        }

        // Buscar en URL de acción
        if (urlAccion != null) {
            Matcher matcherUrl = pattern.matcher(urlAccion);
            while (matcherUrl.find()) {
                String variable = matcherUrl.group(1).trim();
                if (!variables.contains(variable)) {
                    variables.add(variable);
                }
            }
        }

        return variables;
    }

    /**
     * Verifica si la plantilla tiene todas las variables necesarias
     */
    public boolean tieneTodasLasVariables(Map<String, Object> variables) {
        java.util.List<String> variablesNecesarias = extraerVariables();
        
        if (variables == null || variables.isEmpty()) {
            return variablesNecesarias.isEmpty();
        }

        for (String variableNecesaria : variablesNecesarias) {
            if (!variables.containsKey(variableNecesaria)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Activa esta plantilla
     */
    public void activar() {
        this.activa = true;
    }

    /**
     * Desactiva esta plantilla
     */
    public void desactivar() {
        this.activa = false;
    }

    /**
     * Establece como plantilla predeterminada
     */
    public void establecerComoPredeterminada() {
        this.predeterminada = true;
    }

    /**
     * Quita el estado de plantilla predeterminada
     */
    public void quitarPredeterminada() {
        this.predeterminada = false;
    }

    /**
     * Crea una nueva versión de la plantilla
     */
    public PlantillaNotificacion crearNuevaVersion() {
        return PlantillaNotificacion.builder()
                .nombre(this.nombre)
                .codigo(this.codigo + "_v" + (this.version + 1))
                .descripcion(this.descripcion)
                .version(this.version + 1)
                .tipo(this.tipo)
                .canal(this.canal)
                .asunto(this.asunto)
                .contenido(this.contenido)
                .textoBoton(this.textoBoton)
                .urlAccion(this.urlAccion)
                .variablesDisponibles(this.variablesDisponibles)
                .datosEjemplo(this.datosEjemplo)
                .activa(false) // Nueva versión inactiva por defecto
                .predeterminada(false)
                .plantillaSistema(this.plantillaSistema)
                .build();
    }

    /**
     * Verifica si es una plantilla de email (HTML)
     */
    public boolean esEmailHtml() {
        return canal == CanalNotificacion.EMAIL;
    }

    /**
     * Verifica si es una plantilla de texto plano
     */
    public boolean esTextoPlano() {
        return canal == CanalNotificacion.WHATSAPP || canal == CanalNotificacion.SMS;
    }

    /**
     * Obtiene el tipo MIME del contenido
     */
    public String getTipoMime() {
        return esEmailHtml() ? "text/html" : "text/plain";
    }

    @Override
    public String toString() {
        return "PlantillaNotificacion{" +
                "id=" + idPlantilla +
                ", nombre='" + nombre + '\'' +
                ", codigo='" + codigo + '\'' +
                ", tipo=" + tipo +
                ", canal=" + canal +
                ", version=" + version +
                ", activa=" + activa +
                ", predeterminada=" + predeterminada +
                '}';
    }
}
