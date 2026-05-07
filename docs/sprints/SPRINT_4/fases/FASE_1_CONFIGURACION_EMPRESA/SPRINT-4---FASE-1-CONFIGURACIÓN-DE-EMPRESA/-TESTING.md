## ✅ TESTING

### Tests Unitarios

**Archivo:** `EmpresaServiceTest.java`

```java
@SpringBootTest
class EmpresaServiceTest {

    @Autowired
    private EmpresaService empresaService;

    @Test
    void deberiaObtenerConfiguracion() {
        EmpresaDTO empresa = empresaService.obtenerConfiguracion();
        assertNotNull(empresa);
        assertEquals(1L, empresa.getId());
    }

    @Test
    void deberiaActualizarDatos() {
        EmpresaDTO dto = empresaService.obtenerConfiguracion();
        dto.setNombre("Nueva Empresa S.L.");
        
        empresaService.actualizarConfiguracion(dto);
        
        EmpresaDTO actualizada = empresaService.obtenerConfiguracion();
        assertEquals("Nueva Empresa S.L.", actualizada.getNombre());
    }

    @Test
    void deberiaRechazarLogoInvalido() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "contenido".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> {
            empresaService.guardarLogo(file);
        });
    }
}
```

### Tests de Integración

**Archivo:** `EmpresaControllerIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
class EmpresaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "EMPRESA_EDITAR")
    void deberiaActualizarEmpresa() throws Exception {
        mockMvc.perform(post("/admin/empresa/actualizar")
                .param("nombre", "Test Empresa")
                .param("cif", "B12345678")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/empresa/editar"))
            .andExpect(flash().attributeExists("success"));
    }
}
```

---

