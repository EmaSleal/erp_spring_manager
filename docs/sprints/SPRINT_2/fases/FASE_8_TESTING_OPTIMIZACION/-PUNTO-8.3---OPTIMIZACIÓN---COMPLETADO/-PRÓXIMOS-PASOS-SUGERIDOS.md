## 🔄 PRÓXIMOS PASOS SUGERIDOS

### Para el Sistema de Caché

1. **Agregar configuración de TTL en application.yml**:
   ```yaml
   spring:
     cache:
       type: caffeine
       caffeine:
         spec: maximumSize=500,expireAfterWrite=3600s
   ```

2. **Migrar a Redis** (opcional, si se requiere cache distribuido):
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-redis</artifactId>
   </dependency>
   ```

3. **Implementar cache warming** al inicio de la aplicación:
   ```java
   @EventListener(ApplicationReadyEvent.class)
   public void warmUpCache() {
       configuracionFacturacionService.getConfiguracionActiva();
       configuracionNotificacionesService.getConfiguracionActiva();
       empresaService.getEmpresaPrincipal();
   }
   ```

4. **Agregar métricas de caché** (Micrometer):
   ```java
   @Autowired
   private CacheManager cacheManager;
   
   public CacheStatistics getCacheStats() {
       // Implementar recolección de estadísticas
   }
   ```

### Para Paginación

5. **Agregar selector de tamaño de página** en las vistas:
   ```html
   <select name="size" onchange="this.form.submit()">
       <option value="10">10</option>
       <option value="25">25</option>
       <option value="50">50</option>
       <option value="100">100</option>
   </select>
   ```

6. **Implementar búsqueda con paginación** en todos los módulos

7. **Agregar export con paginación** (CSV/Excel por páginas)

### Para Base de Datos

8. **Monitorear performance de índices** con EXPLAIN:
   ```sql
   EXPLAIN SELECT * FROM factura WHERE estado = 'PENDIENTE';
   ```

9. **Revisar SPs no utilizados** y deprecar si no son necesarios

10. **Implementar índices compuestos** si se detectan queries lentas:
    ```sql
    CREATE INDEX idx_factura_estado_fecha 
    ON factura(estado, fecha_vencimiento);
    ```

---

