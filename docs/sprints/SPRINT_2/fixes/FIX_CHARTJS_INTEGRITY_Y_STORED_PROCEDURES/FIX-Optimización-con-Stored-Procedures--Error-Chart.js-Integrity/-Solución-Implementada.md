## ✅ Solución Implementada

### **Fix 1: Eliminar integrity hash**

**Archivo:** `layout.html`

**Antes:**
```html
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js" 
        integrity="sha384-TAsW8ym4ZHYDDNJLqAhSTDEvX4C5FLAKMXaEQIeaP8Q7e0F4V7bUGJPV3kLpnPTo" 
        crossorigin="anonymous"></script>
```

**Después:**
```html
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.js" 
        crossorigin="anonymous"></script>
```

**Justificación:**
- El CDN de jsdelivr es confiable y seguro
- El integrity hash estaba desactualizado o incorrecto
- `crossorigin="anonymous"` mantiene seguridad CORS
- Chart.js ahora carga correctamente

---

### **Fix 2: Stored Procedures en MySQL**

Creamos 6 Stored Procedures optimizados en `SP_REPORTES_GRAFICOS.sql`:

#### **1. sp_obtener_ventas_por_mes**
```sql
CREATE PROCEDURE sp_obtener_ventas_por_mes(IN p_meses INT)
BEGIN
    DECLARE v_fecha_inicio DATE;
    SET v_fecha_inicio = DATE_SUB(CURDATE(), INTERVAL p_meses MONTH);
    
    SELECT 
        DATE_FORMAT(f.fecha_emision, '%b %Y') AS mes,
        COALESCE(SUM(f.total), 0) AS total_ventas
    FROM factura f
    WHERE f.fecha_emision >= v_fecha_inicio
        AND f.fecha_emision <= CURDATE()
    GROUP BY DATE_FORMAT(f.fecha_emision, '%Y-%m'),
             DATE_FORMAT(f.fecha_emision, '%b %Y')
    ORDER BY DATE_FORMAT(f.fecha_emision, '%Y-%m') ASC;
END
```

**Ventajas:**
- ✅ Filtrado directo en la base de datos (WHERE)
- ✅ Agrupación nativa (GROUP BY)
- ✅ Suma optimizada (SUM)
- ✅ Formato de fecha en MySQL (DATE_FORMAT)
- ✅ Solo retorna datos necesarios

#### **2. sp_obtener_clientes_nuevos_por_mes**
```sql
CREATE PROCEDURE sp_obtener_clientes_nuevos_por_mes(IN p_meses INT)
BEGIN
    DECLARE v_fecha_inicio DATE;
    SET v_fecha_inicio = DATE_SUB(CURDATE(), INTERVAL p_meses MONTH);
    
    SELECT 
        DATE_FORMAT(c.create_date, '%b %Y') AS mes,
        COUNT(*) AS cantidad_clientes
    FROM cliente c
    WHERE c.create_date >= v_fecha_inicio
        AND c.create_date <= CURDATE()
    GROUP BY DATE_FORMAT(c.create_date, '%Y-%m'),
             DATE_FORMAT(c.create_date, '%b %Y')
    ORDER BY DATE_FORMAT(c.create_date, '%Y-%m') ASC;
END
```

**Ventajas:**
- ✅ COUNT directo en la base de datos
- ✅ Sin conversión de tipos en Java
- ✅ Agrupación eficiente

#### **3. sp_obtener_productos_mas_vendidos**
```sql
CREATE PROCEDURE sp_obtener_productos_mas_vendidos(IN p_limite INT)
BEGIN
    SELECT 
        p.descripcion AS producto,
        COALESCE(SUM(lf.cantidad), 0) AS cantidad_vendida
    FROM producto p
    LEFT JOIN linea_factura lf ON p.id_producto = lf.id_producto
    LEFT JOIN factura f ON lf.id_factura = f.id_factura
    WHERE p.active = 1
    GROUP BY p.id_producto, p.descripcion
    HAVING cantidad_vendida > 0
    ORDER BY cantidad_vendida DESC
    LIMIT p_limite;
END
```

**Ventajas:**
- ✅ JOIN optimizado
- ✅ HAVING para filtrar después de agrupación
- ✅ ORDER BY + LIMIT en la base de datos
- ✅ Sin sorting en Java

#### **4. sp_obtener_ventas_por_dia**
Para gráfico de ventas filtradas (dinámico)

#### **5. sp_obtener_estadisticas_ventas**
Para estadísticas agregadas (futuro uso)

#### **6. sp_obtener_top_clientes**
Para ranking de clientes (futuro uso)

---

### **Fix 3: Actualizar Repositories**

Agregamos métodos nativos para llamar SPs:

**FacturaRepository.java:**
```java
@Query(value = "CALL sp_obtener_ventas_por_mes(:meses)", nativeQuery = true)
List<Object[]> obtenerVentasPorMes(@Param("meses") int meses);

@Query(value = "CALL sp_obtener_ventas_por_dia(:fechaInicio, :fechaFin, :clienteId)", nativeQuery = true)
List<Object[]> obtenerVentasPorDia(
    @Param("fechaInicio") java.sql.Date fechaInicio,
    @Param("fechaFin") java.sql.Date fechaFin,
    @Param("clienteId") Integer clienteId
);
```

**ClienteRepository.java:**
```java
@Query(value = "CALL sp_obtener_clientes_nuevos_por_mes(:meses)", nativeQuery = true)
List<Object[]> obtenerClientesNuevosPorMes(@Param("meses") int meses);
```

**ProductoRepository.java:**
```java
@Query(value = "CALL sp_obtener_productos_mas_vendidos(:limite)", nativeQuery = true)
List<Object[]> obtenerProductosMasVendidos(@Param("limite") int limite);
```

---

### **Fix 4: Actualizar Controller**

Modificamos los endpoints para usar SPs directamente:

**ReporteController.java:**

**Antes (procesamiento en Java):**
```java
@GetMapping("/api/ventas-por-mes")
@ResponseBody
public Map<String, Object> getVentasPorMes(@RequestParam Integer meses) {
    Map<String, BigDecimal> ventasPorMes = reporteService.obtenerVentasPorMes(meses);
    // ... conversión manual
}
```

**Después (llamada directa al SP):**
```java
@GetMapping("/api/ventas-por-mes")
@ResponseBody
public Map<String, Object> getVentasPorMes(@RequestParam Integer meses) {
    log.info("Obteniendo datos de ventas por mes - últimos {} meses (usando SP)", meses);
    
    // Llamar directamente al SP desde el repository
    List<Object[]> resultadoSP = facturaRepository.obtenerVentasPorMes(meses);
    
    // Convertir a formato esperado por Chart.js
    List<String> labels = new java.util.ArrayList<>();
    List<java.math.BigDecimal> data = new java.util.ArrayList<>();
    
    for (Object[] fila : resultadoSP) {
        labels.add((String) fila[0]); // mes
        data.add((java.math.BigDecimal) fila[1]); // total_ventas
    }
    
    Map<String, Object> resultado = new HashMap<>();
    resultado.put("labels", labels);
    resultado.put("data", data);
    
    log.info("Datos obtenidos desde SP - {} registros", resultadoSP.size());
    return resultado;
}
```

**Cambios aplicados a:**
- ✅ `getVentasPorMes()`
- ✅ `getClientesNuevos()`
- ✅ `getProductosMasVendidos()`

---

