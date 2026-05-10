## 💡 NOTAS TÉCNICAS

### Cálculo de Nómina (Ejemplo)

**Empleado:** Juan Pérez  
**Salario bruto mensual:** ₡1,500,000  
**Jornada:** Ordinaria diurna  

#### Deducciones Obrero:
```
CCSS (9.34%):           ₡140,100
INS (1%):               ₡15,000
Impuesto Renta (15%):   ₡77,850  (sobre excedente de ₡941,000)
─────────────────────────────────
Total deducciones:      ₡232,950
```

#### Salario Neto:
```
Salario bruto:          ₡1,500,000
- Deducciones:          ₡232,950
═════════════════════════════════
SALARIO NETO:           ₡1,267,050
```

#### Cargas Patronales:
```
CCSS patronal (26.67%): ₡400,050
INS (~1%):              ₡15,000
Aguinaldo (8.33%):      ₡124,950
Cesantía (8.33%):       ₡124,950
─────────────────────────────────
Total carga patronal:   ₡664,950
```

#### Costo Total Empleado:
```
Salario bruto:          ₡1,500,000
+ Cargas patronales:    ₡664,950
═════════════════════════════════
COSTO TOTAL:            ₡2,164,950  (144% del salario bruto)
```

---

### Modelo de Datos

**Empleado:**
```java
@Entity
public class Empleado {
    @Id
    @GeneratedValue
    private Long id;
    
    private String nombre;
    private String cedula; // 1-1234-5678
    private String email;
    private String telefono;
    
    @ManyToOne
    private Departamento departamento;
    
    @ManyToOne
    private Puesto puesto;
    
    private BigDecimal salarioBruto;
    private LocalDate fechaIngreso;
    private LocalDate fechaSalida; // nullable
    private String tipoContrato; // PLAZO_FIJO, INDEFINIDO
    
    private Boolean activo;
}
```

**Nómina:**
```java
@Entity
public class Nomina {
    @Id
    @GeneratedValue
    private Long id;
    
    private Integer mes;
    private Integer anio;
    private LocalDate fechaProceso;
    private LocalDate fechaPago;
    
    private String estado; // BORRADOR, PROCESADA, PAGADA
    
    @OneToMany(mappedBy = "nomina")
    private List<DetalleNomina> detalles;
    
    private BigDecimal totalBruto;
    private BigDecimal totalDeducciones;
    private BigDecimal totalNeto;
    private BigDecimal totalCargasPatronales;
}
```

**DetalleNomina:**
```java
@Entity
public class DetalleNomina {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    private Nomina nomina;
    
    @ManyToOne
    private Empleado empleado;
    
    // Ingresos
    private BigDecimal salarioBruto;
    private BigDecimal horasExtra;
    private BigDecimal bonificaciones;
    
    // Deducciones
    private BigDecimal deduccionCCSS;
    private BigDecimal deduccionINS;
    private BigDecimal deduccionRenta;
    private BigDecimal prestamos;
    private BigDecimal embargos;
    
    // Prestaciones
    private BigDecimal aguinaldoAcumulado;
    private BigDecimal cesantiaAcumulada;
    
    // Cargas patronales
    private BigDecimal ccssPatronal;
    private BigDecimal insPatronal;
    
    // Resultado
    private BigDecimal salarioNeto;
    private BigDecimal costoTotal;
}
```

---

### Integración con Contabilidad

**Asiento contable de nómina:**
```
DEBE:
  Gastos de Salarios           ₡1,500,000
  Gastos CCSS Patronal         ₡400,050
  Gastos INS Patronal          ₡15,000
  Gastos Aguinaldo             ₡124,950
  Gastos Cesantía              ₡124,950
                               ───────────
  Total DEBE                   ₡2,164,950

HABER:
  CCSS por Pagar (obrero)      ₡140,100
  CCSS por Pagar (patronal)    ₡400,050
  INS por Pagar (obrero)       ₡15,000
  INS por Pagar (patronal)     ₡15,000
  Renta por Pagar              ₡77,850
  Provisión Aguinaldo          ₡124,950
  Provisión Cesantía           ₡124,950
  Salarios por Pagar           ₡1,267,050
                               ───────────
  Total HABER                  ₡2,164,950
```

---

### Reportes Financieros

**Estado de Resultados (Ejemplo simplificado):**
```
══════════════════════════════════════════════════
  ESTADO DE RESULTADOS - Enero 2026
══════════════════════════════════════════════════

INGRESOS
  Ventas                              ₡15,000,000
  Otros ingresos                         ₡500,000
                                      ────────────
  Total Ingresos                      ₡15,500,000

COSTOS Y GASTOS
  Costo de Ventas                      ₡8,000,000
  Gastos de Personal                   ₡2,164,950
  Gastos Operativos                    ₡1,500,000
  Gastos Administrativos               ₡1,000,000
                                      ────────────
  Total Costos y Gastos               ₡12,664,950

                                      ════════════
UTILIDAD NETA                          ₡2,835,050
══════════════════════════════════════════════════
```

---

**Documento creado:** 16 de enero de 2026  
**Creado por:** GitHub Copilot  
**Versión:** 1.0  
**Estado:** 📋 PLANIFICADO  
**Tipo:** ⚠️ **CONDICIONAL** - Solo si se gestiona personal  
**Decisión pendiente:** ¿La empresa gestiona empleados en planilla?
