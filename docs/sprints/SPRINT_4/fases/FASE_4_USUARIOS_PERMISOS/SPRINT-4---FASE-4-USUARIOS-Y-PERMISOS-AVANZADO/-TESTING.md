## ✅ TESTING

### Tests Unitarios

```java
@SpringBootTest
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void deberiaCrearUsuario() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre("Test User");
        dto.setEmail("test@example.com");
        dto.setRol(Rol.VENDEDOR);

        Usuario usuario = usuarioService.crear(dto);

        assertNotNull(usuario.getId());
        assertEquals("Test User", usuario.getNombre());
        assertTrue(usuario.getActivo());
    }

    @Test
    void deberiaBloquearUsuario() {
        Usuario usuario = crearUsuarioPrueba();
        
        usuarioService.bloquear(usuario.getId(), "Prueba");
        
        Usuario bloqueado = usuarioRepository.findById(usuario.getId()).get();
        assertTrue(bloqueado.getBloqueado());
        assertEquals("Prueba", bloqueado.getMotivoBloqueo());
    }
}
```

### Tests de Seguridad

```java
@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void noDeberiaPermitirAccesoSinAutenticacion() throws Exception {
        mockMvc.perform(get("/admin/usuarios/gestionar"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(authorities = "USUARIOS_VER")
    void deberiaPermitirAccesoConPermiso() throws Exception {
        mockMvc.perform(get("/admin/usuarios/gestionar"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "CLIENTES_VER")
    void noDeberiaPermitirAccesoSinPermiso() throws Exception {
        mockMvc.perform(get("/admin/usuarios/gestionar"))
            .andExpect(status().isForbidden());
    }
}
```

---

