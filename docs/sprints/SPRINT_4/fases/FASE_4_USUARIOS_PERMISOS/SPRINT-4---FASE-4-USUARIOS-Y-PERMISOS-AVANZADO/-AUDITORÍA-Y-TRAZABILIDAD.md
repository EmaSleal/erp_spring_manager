## 📊 AUDITORÍA Y TRAZABILIDAD

### Configuración de Auditoría

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }
}

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername());
        }
        
        return Optional.of(principal.toString());
    }
}
```

### Consulta de Auditoría

```java
@Service
@Transactional(readOnly = true)
public class AuditoriaService {

    private final EntityManager entityManager;
    
    /**
     * Obtiene el historial de cambios de un usuario
     */
    public List<CambioAuditoriaDTO> obtenerHistorialUsuario(Long usuarioId) {
        String sql = """
            SELECT 
                'USUARIO' as tipo_entidad,
                u.id as entidad_id,
                u.modificado_por as modificado_por,
                u.modificado_en as modificado_en,
                'Modificación de usuario' as descripcion
            FROM usuarios u
            WHERE u.id = :usuarioId
            
            UNION ALL
            
            SELECT 
                'PERMISO' as tipo_entidad,
                up.id as entidad_id,
                up.asignado_por as modificado_por,
                up.asignado_en as modificado_en,
                CONCAT('Permiso ', up.permiso, ' ', IF(up.activo, 'asignado', 'revocado')) as descripcion
            FROM usuario_permiso up
            WHERE up.usuario_id = :usuarioId
            
            ORDER BY modificado_en DESC
            """;
        
        return entityManager.createNativeQuery(sql)
            .setParameter("usuarioId", usuarioId)
            .getResultList()
            .stream()
            .map(row -> {
                Object[] cols = (Object[]) row;
                return new CambioAuditoriaDTO(
                    (String) cols[0],      // tipo_entidad
                    ((Number) cols[1]).longValue(), // entidad_id
                    (String) cols[2],      // modificado_por
                    ((Timestamp) cols[3]).toLocalDateTime(), // modificado_en
                    (String) cols[4]       // descripcion
                );
            })
            .collect(Collectors.toList());
    }
}
```

---

