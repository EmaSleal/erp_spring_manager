# 🧪 FASE 4: Testing y Quality Assurance

**Sprint:** 8  
**Fase:** 4 de 5  
**Duración estimada:** 2-3 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Estado:** 📋 PENDIENTE (0/8 tareas)

---

## 📋 OBJETIVO DE LA FASE

Garantizar calidad y precisión de:
- **Cálculos de Nómina** (Costa Rica - normativa laboral)
- **Reportes Financieros**
- **Flujos de RRHH**
- **Integraciones contables**

**Cobertura objetivo:** ≥ 75%

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/8] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Tests de Cálculos de Nómina     [0/3]  ░░░░░░░░░░ 0%
├─ 2. Tests de Reportes Financieros   [0/2]  ░░░░░░░░░░ 0%
├─ 3. Tests de Gestión de RRHH        [0/2]  ░░░░░░░░░░ 0%
└─ 4. Tests de Integración            [0/1]  ░░░░░░░░░░ 0%
```

---

## 📦 1. TESTS DE CÁLCULOS DE NÓMINA (3 tareas)

### Objetivo:
Validar **precisión** de cálculos de:
- CCSS Obrero (9.34%)
- CCSS Patronal (26.67%)
- INS Riesgos (~1%)
- Impuesto Renta (progresivo)
- Aguinaldo (8.33%)
- Cesantía (8.33%)

### Tareas:

- [ ] **1.1** Tests de cálculo de deducciones CCSS e INS

```java
package com.erp.whatsorders.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Tests de Cálculos de Nómina - Costa Rica")
class CalculosNominaServiceTest {
    
    @Autowired
    private CalculosNominaService calculosService;
    
    @Test
    @DisplayName("Debe calcular correctamente deducción CCSS Obrero (9.34%)")
    void testCalcularDeduccionCCSSObrero() {
        // Salario bruto: ₡500,000
        BigDecimal salarioBruto = new BigDecimal("500000.00");
        
        // Expected: 500,000 * 9.34% = ₡46,700
        BigDecimal esperado = new BigDecimal("46700.00");
        
        BigDecimal resultado = calculosService.calcularDeduccionCCSSObrero(salarioBruto);
        
        assertEquals(0, esperado.compareTo(resultado), 
            "CCSS Obrero debe ser 9.34% del salario bruto");
    }
    
    @Test
    @DisplayName("Debe calcular correctamente deducción INS Riesgos (1%)")
    void testCalcularDeduccionINS() {
        BigDecimal salarioBruto = new BigDecimal("500000.00");
        
        // Expected: 500,000 * 1% = ₡5,000
        BigDecimal esperado = new BigDecimal("5000.00");
        
        BigDecimal resultado = calculosService.calcularDeduccionINS(salarioBruto);
        
        assertEquals(0, esperado.compareTo(resultado),
            "INS debe ser 1% del salario bruto");
    }
    
    @Test
    @DisplayName("Debe calcular correctamente carga patronal CCSS (26.67%)")
    void testCalcularCargaPatronalCCSS() {
        BigDecimal salarioBruto = new BigDecimal("500000.00");
        
        // Expected: 500,000 * 26.67% = ₡133,350
        BigDecimal esperado = new BigDecimal("133350.00");
        
        BigDecimal resultado = calculosService.calcularCargaPatronalCCSS(salarioBruto);
        
        assertEquals(0, esperado.compareTo(resultado),
            "Carga patronal CCSS debe ser 26.67% del salario bruto");
    }
}
```

- [ ] **1.2** Tests de Impuesto sobre la Renta (progresivo)

```java
@Test
@DisplayName("Debe calcular Impuesto Renta progresivo correctamente")
void testCalcularImpuestoRenta() {
    // Caso 1: Salario ₡500,000 - EXENTO (menos de ₡941,000)
    BigDecimal salario1 = new BigDecimal("500000.00");
    BigDecimal impuesto1 = calculosService.calcularImpuestoRenta(salario1);
    assertEquals(0, BigDecimal.ZERO.compareTo(impuesto1),
        "Salarios menores a ₡941,000 deben estar exentos");
    
    // Caso 2: Salario ₡1,200,000 - 10% sobre exceso
    BigDecimal salario2 = new BigDecimal("1200000.00");
    BigDecimal impuesto2 = calculosService.calcularImpuestoRenta(salario2);
    
    // Exceso: 1,200,000 - 941,000 = 259,000
    // Impuesto: 259,000 * 10% = 25,900
    BigDecimal esperado2 = new BigDecimal("25900.00");
    assertEquals(0, esperado2.compareTo(impuesto2),
        "Primer tramo debe aplicar 10% sobre exceso de ₡941k");
    
    // Caso 3: Salario ₡2,500,000 - Múltiples tramos
    BigDecimal salario3 = new BigDecimal("2500000.00");
    BigDecimal impuesto3 = calculosService.calcularImpuestoRenta(salario3);
    
    // Tramo 1: (1,381,000 - 941,000) * 10% = 44,000
    // Tramo 2: (2,500,000 - 1,381,000) * 15% = 167,850
    // Total: 211,850
    BigDecimal esperado3 = new BigDecimal("211850.00");
    assertEquals(0, esperado3.compareTo(impuesto3),
        "Debe calcular correctamente múltiples tramos progresivos");
}

@Test
@DisplayName("Debe calcular Aguinaldo correctamente (8.33%)")
void testCalcularAguinaldo() {
    BigDecimal salarioBruto = new BigDecimal("500000.00");
    
    // Expected: 500,000 * 8.33% = ₡41,650
    BigDecimal esperado = new BigDecimal("41650.00");
    
    BigDecimal resultado = calculosService.calcularAguinaldo(salarioBruto);
    
    assertEquals(0, esperado.compareTo(resultado),
        "Aguinaldo debe ser 8.33% del salario bruto");
}

@Test
@DisplayName("Debe calcular Cesantía correctamente (8.33%)")
void testCalcularCesantia() {
    BigDecimal salarioBruto = new BigDecimal("500000.00");
    
    // Expected: 500,000 * 8.33% = ₡41,650
    BigDecimal esperado = new BigDecimal("41650.00");
    
    BigDecimal resultado = calculosService.calcularCesantia(salarioBruto);
    
    assertEquals(0, esperado.compareTo(resultado),
        "Cesantía debe ser 8.33% del salario bruto");
}
```

- [ ] **1.3** Tests de cálculo completo de nómina

```java
@Test
@DisplayName("Debe calcular detalle de nómina completo correctamente")
void testCalcularDetalleCompleto() {
    // Setup: Empleado con salario ₡1,000,000
    Empleado empleado = new Empleado();
    empleado.setId(1L);
    empleado.setNombre("Juan Pérez");
    empleado.setSalarioBase(new BigDecimal("1000000.00"));
    
    // Horas extras al 50%: 10 horas * ₡10,000 * 1.5 = ₡150,000
    BigDecimal horasExtra = new BigDecimal("10");
    BigDecimal valorHora = new BigDecimal("10000.00");
    
    DetalleNomina detalle = calculosService.calcularDetalleCompleto(
        empleado, horasExtra, BigDecimal.ZERO
    );
    
    // Validar salario bruto
    BigDecimal salarioBrutoEsperado = new BigDecimal("1150000.00"); // 1M + 150k
    assertEquals(0, salarioBrutoEsperado.compareTo(detalle.getSalarioBruto()));
    
    // Validar CCSS Obrero: 1,150,000 * 9.34% = ₡107,410
    BigDecimal ccssEsperado = new BigDecimal("107410.00");
    assertEquals(0, ccssEsperado.compareTo(detalle.getDeduccionCCSS()));
    
    // Validar INS: 1,150,000 * 1% = ₡11,500
    BigDecimal insEsperado = new BigDecimal("11500.00");
    assertEquals(0, insEsperado.compareTo(detalle.getDeduccionINS()));
    
    // Validar salario neto
    assertNotNull(detalle.getSalarioNeto());
    assertTrue(detalle.getSalarioNeto().compareTo(BigDecimal.ZERO) > 0);
}
```

---

## 📦 2. TESTS DE REPORTES FINANCIEROS (2 tareas)

- [ ] **2.1** Tests de Estado de Resultados

```java
package com.erp.whatsorders.service;

import com.erp.whatsorders.dto.EstadoResultadosDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Tests de Estado de Resultados")
class EstadoResultadosServiceTest {
    
    @Autowired
    private EstadoResultadosService estadoResultadosService;
    
    @Test
    @DisplayName("Debe generar Estado de Resultados correctamente")
    void testGenerarEstadoResultados() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        
        EstadoResultadosDTO estado = estadoResultadosService.generar(desde, hasta);
        
        assertNotNull(estado);
        assertNotNull(estado.getTotalIngresos());
        assertNotNull(estado.getUtilidadBruta());
        assertNotNull(estado.getUtilidadNeta());
        
        // Validar cálculos
        BigDecimal utilidadBruta = estado.getTotalIngresos()
            .subtract(estado.getCostoVentas());
        assertEquals(0, utilidadBruta.compareTo(estado.getUtilidadBruta()));
        
        // Validar margen bruto
        if (estado.getTotalIngresos().compareTo(BigDecimal.ZERO) > 0) {
            assertNotNull(estado.getMargenBruto());
            assertTrue(estado.getMargenBruto().compareTo(BigDecimal.ZERO) >= 0);
        }
    }
    
    @Test
    @DisplayName("Debe calcular márgenes correctamente")
    void testCalcularMargenes() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);
        
        EstadoResultadosDTO estado = estadoResultadosService.generar(desde, hasta);
        
        if (estado.getTotalIngresos().compareTo(BigDecimal.ZERO) > 0) {
            // Margen bruto = (Utilidad Bruta / Ingresos) * 100
            BigDecimal margenBrutoCalculado = estado.getUtilidadBruta()
                .multiply(new BigDecimal("100"))
                .divide(estado.getTotalIngresos(), 2, java.math.RoundingMode.HALF_UP);
            
            assertEquals(0, margenBrutoCalculado.compareTo(estado.getMargenBruto()));
        }
    }
}
```

- [ ] **2.2** Tests de Balance General y Flujo de Caja

---

## 📦 3. TESTS DE GESTIÓN DE RRHH (2 tareas)

- [ ] **3.1** Tests de gestión de empleados

```java
@SpringBootTest
@DisplayName("Tests de Gestión de Empleados")
class EmpleadoServiceTest {
    
    @Autowired
    private EmpleadoService empleadoService;
    
    @Test
    @DisplayName("Debe crear empleado correctamente")
    void testCrearEmpleado() {
        EmpleadoDTO dto = new EmpleadoDTO();
        dto.setNombre("María López");
        dto.setCedula("1-2345-6789");
        dto.setEmail("maria.lopez@empresa.com");
        dto.setSalarioBase(new BigDecimal("800000.00"));
        
        EmpleadoDTO creado = empleadoService.crear(dto);
        
        assertNotNull(creado.getId());
        assertEquals("María López", creado.getNombre());
        assertEquals("1-2345-6789", creado.getCedula());
    }
    
    @Test
    @DisplayName("Debe validar cédula costarricense")
    void testValidarCedula() {
        assertThrows(ValidationException.class, () -> {
            EmpleadoDTO dto = new EmpleadoDTO();
            dto.setCedula("INVALIDA");
            empleadoService.crear(dto);
        });
    }
}
```

- [ ] **3.2** Tests de control de asistencia y vacaciones

---

## 📦 4. TESTS DE INTEGRACIÓN (1 tarea)

- [ ] **4.1** Tests de integración Nómina → Contabilidad

```java
@SpringBootTest
@Transactional
@DisplayName("Tests de Integración Nómina-Contabilidad")
class NominaContabilidadIntegrationTest {
    
    @Autowired
    private NominaService nominaService;
    
    @Autowired
    private AsientoContableRepository asientoRepository;
    
    @Test
    @DisplayName("Debe generar asiento contable al aprobar nómina")
    void testGenerarAsientoContable() {
        // Crear nómina
        NominaDTO nomina = nominaService.crear(
            LocalDate.now(), 
            "Nómina Enero 2026"
        );
        
        // Calcular
        nominaService.calcular(nomina.getId());
        
        // Aprobar - debe generar asiento contable
        nominaService.aprobar(nomina.getId());
        
        // Verificar asiento
        List<AsientoContable> asientos = asientoRepository
            .findByReferenciaExterna("NOMINA-" + nomina.getId());
        
        assertFalse(asientos.isEmpty(), 
            "Debe generar asiento contable al aprobar nómina");
        
        AsientoContable asiento = asientos.get(0);
        assertEquals("NOMINA", asiento.getTipo());
        assertTrue(asiento.getTotalDebe().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(asiento.getTotalHaber().compareTo(BigDecimal.ZERO) > 0);
    }
}
```

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ **Cálculos de nómina validados** (CCSS, INS, Renta, Aguinaldo, Cesantía)  
✅ **Reportes financieros testeados**  
✅ **Flujos de RRHH verificados**  
✅ **Integración con contabilidad funcionando**  
✅ **Cobertura ≥ 75%**  
✅ **Tests pasando en CI/CD**  

---

## 🎯 MÉTRICAS DE CALIDAD

- **Cobertura de código:** ≥ 75%
- **Bugs críticos:** 0
- **Performance:** Nómina de 100 empleados < 5 segundos
- **Precisión:** Cálculos exactos (sin redondeos incorrectos)

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 8 Fase 1: RRHH
- ✅ Sprint 8 Fase 2: Nómina
- ✅ Sprint 8 Fase 3: Reportes

**Habilita:**
- 🚀 Fase 5: Documentación

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** QA Team  
**Prioridad:** CRÍTICA - Validación de cálculos normativa CR
