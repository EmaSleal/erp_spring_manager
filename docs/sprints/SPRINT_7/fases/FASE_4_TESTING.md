# 🧪 FASE 4: Testing y Validación

**Sprint:** 7  
**Fase:** 4 de 5  
**Duración estimada:** 2-3 días  
**Prioridad:** ⭐⭐ ALTA  
**Estado:** 📋 PENDIENTE (0/6 tareas)

---

## 📋 OBJETIVO DE LA FASE

Validar exhaustivamente las mejoras técnicas y de seguridad implementadas:
- Tests de migración username/email
- Tests de migración Timestamp→LocalDateTime
- Tests de Remember Me
- Tests de 2FA
- Tests de seguridad (JWT, bloqueo)
- Tests de integración completos

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/6] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Tests de Migraciones            [0/1]  ░░░░░░░░░░ 0%
├─ 2. Tests de Autenticación          [0/2]  ░░░░░░░░░░ 0%
├─ 3. Tests de Seguridad 2FA          [0/1]  ░░░░░░░░░░ 0%
├─ 4. Tests de JWT                    [0/1]  ░░░░░░░░░░ 0%
└─ 5. Tests de Integración            [0/1]  ░░░░░░░░░░ 0%
```

---

## 📦 1. TESTS DE MIGRACIONES (1 tarea)

### 1.1. Tests de Username → Email

#### Tareas:

- [ ] **1.1** Crear suite de tests de migración

```java
@SpringBootTest
@Transactional
class MigracionUsernameEmailTest {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Test
    @DisplayName("Usuario puede hacer login con email")
    void testLoginConEmail() {
        // Given
        Usuario usuario = crearUsuarioPrueba("test@example.com", "testuser", "12345678");
        
        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");
        
        // Then
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
    }
    
    @Test
    @DisplayName("Usuario puede hacer login con username")
    void testLoginConUsername() {
        // Given
        Usuario usuario = crearUsuarioPrueba("test@example.com", "testuser", "12345678");
        
        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");
        
        // Then
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("testuser", ((Usuario) userDetails).getUsernameDisplay());
    }
    
    @Test
    @DisplayName("Email debe ser único")
    void testEmailUnico() {
        // Given
        crearUsuarioPrueba("test@example.com", "user1", "12345678");
        
        // When & Then
        assertThrows(BusinessException.class, () -> {
            crearUsuarioPrueba("test@example.com", "user2", "87654321");
        });
    }
    
    @Test
    @DisplayName("Username debe ser único si se proporciona")
    void testUsernameUnico() {
        // Given
        crearUsuarioPrueba("test1@example.com", "testuser", "12345678");
        
        // When & Then
        assertThrows(BusinessException.class, () -> {
            crearUsuarioPrueba("test2@example.com", "testuser", "87654321");
        });
    }
    
    @Test
    @DisplayName("Username es opcional")
    void testUsernameOpcional() {
        // When
        Usuario usuario = crearUsuarioPrueba("test@example.com", null, "12345678");
        
        // Then
        assertNotNull(usuario);
        assertEquals("test@example.com", usuario.getEmail());
        assertNull(usuario.getUsername());
        
        // Puede hacer login solo con email
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");
        assertNotNull(userDetails);
    }
    
    @Test
    @DisplayName("Teléfono se separó correctamente del username")
    void testTelefonoSeparado() {
        // Given
        Usuario usuario = crearUsuarioPrueba("test@example.com", "testuser", "12345678");
        
        // When & Then
        assertEquals("12345678", usuario.getTelefono());
        assertNotEquals(usuario.getTelefono(), usuario.getUsername());
    }
    
    @Test
    @DisplayName("Migración desde username antiguo (teléfono)")
    void testMigracionDesdeUsernameAntiguo() {
        // Simular usuario antiguo (username = teléfono)
        // Este test valida que la migration SQL funcionó correctamente
        
        // Given: Crear usuario directamente en BD con estructura antigua
        // (Esto requeriría SQL nativo o setup específico)
        
        // Then: Verificar que la migración preservó los datos
        // email generado, username derivado, teléfono movido
    }
    
    private Usuario crearUsuarioPrueba(String email, String username, String telefono) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setEmail(email);
        dto.setUsername(username);
        dto.setTelefono(telefono);
        dto.setNombre("Usuario Test");
        dto.setPassword("Password123!");
        
        return usuarioService.registrar(dto).toEntity();
    }
}
```

### 1.2. Tests de Timestamp → LocalDateTime

```java
@SpringBootTest
class MigracionTimestampLocalDateTimeTest {
    
    @Test
    @DisplayName("Fechas se guardan como LocalDateTime")
    void testFechasComoLocalDateTime() {
        // Given
        Usuario usuario = new Usuario();
        usuario.setEmail("test@example.com");
        usuario.setNombre("Test");
        
        // When
        usuario = usuarioRepository.save(usuario);
        
        // Then
        assertNotNull(usuario.getCreatedAt());
        assertInstanceOf(LocalDateTime.class, usuario.getCreatedAt());
        assertNotNull(usuario.getUpdatedAt());
        assertInstanceOf(LocalDateTime.class, usuario.getUpdatedAt());
    }
    
    @Test
    @DisplayName("@PrePersist establece fechas automáticamente")
    void testPrePersist() {
        // Given
        LocalDateTime antes = LocalDateTime.now();
        
        Usuario usuario = new Usuario();
        usuario.setEmail("test@example.com");
        
        // When
        usuario = usuarioRepository.save(usuario);
        
        // Then
        assertNotNull(usuario.getCreatedAt());
        assertTrue(usuario.getCreatedAt().isAfter(antes.minusSeconds(1)));
        assertTrue(usuario.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }
    
    @Test
    @DisplayName("@PreUpdate actualiza updatedAt")
    void testPreUpdate() throws InterruptedException {
        // Given
        Usuario usuario = new Usuario();
        usuario.setEmail("test@example.com");
        usuario = usuarioRepository.save(usuario);
        
        LocalDateTime createdAt = usuario.getCreatedAt();
        Thread.sleep(1000); // Esperar 1 segundo
        
        // When
        usuario.setNombre("Nombre Actualizado");
        usuario = usuarioRepository.save(usuario);
        
        // Then
        assertEquals(createdAt, usuario.getCreatedAt()); // No debe cambiar
        assertTrue(usuario.getUpdatedAt().isAfter(usuario.getCreatedAt())); // Debe ser posterior
    }
    
    @Test
    @DisplayName("Comparaciones de fechas con LocalDateTime")
    void testComparacionFechas() {
        // Given
        Usuario usuario = crearUsuario();
        
        // When
        boolean esReciente = usuario.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1));
        
        // Then
        assertTrue(esReciente);
    }
    
    @Test
    @DisplayName("Cálculos de diferencia con ChronoUnit")
    void testCalculoDiferencia() {
        // Given
        Usuario usuario = crearUsuario();
        
        // When
        long dias = ChronoUnit.DAYS.between(usuario.getCreatedAt(), LocalDateTime.now());
        
        // Then
        assertEquals(0, dias); // Creado hace menos de 1 día
    }
}
```

---

## 📦 2. TESTS DE AUTENTICACIÓN (2 tareas)

#### Tareas:

- [ ] **2.1** Tests de Remember Me

```java
@SpringBootTest
@AutoConfigureMockMvc
class RememberMeTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("Remember Me crea cookie")
    void testRememberMeCookie() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "test@example.com")
                .param("password", "password")
                .param("remember-me", "on"))
            .andExpect(status().is3xxRedirection())
            .andExpect(cookie().exists("erp-remember-me"))
            .andExpect(cookie().maxAge("erp-remember-me", 30 * 24 * 60 * 60));
    }
    
    @Test
    @DisplayName("Sin Remember Me no crea cookie")
    void testSinRememberMe() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "test@example.com")
                .param("password", "password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(cookie().doesNotExist("erp-remember-me"));
    }
    
    @Test
    @DisplayName("Remember Me permite acceso sin password")
    void testAccesoConRememberMe() throws Exception {
        // Simular cookie válida
        Cookie rememberMeCookie = generarCookieValida();
        
        mockMvc.perform(get("/dashboard")
                .cookie(rememberMeCookie))
            .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Logout elimina cookie Remember Me")
    void testLogoutEliminaCookie() throws Exception {
        mockMvc.perform(post("/logout")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(cookie().maxAge("erp-remember-me", 0));
    }
}
```

- [ ] **2.2** Tests de bloqueo de cuenta

```java
@SpringBootTest
class BloqueoLoginTest {
    
    @Autowired
    private LoginAttemptService loginAttemptService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Test
    @DisplayName("Cuenta se bloquea después de 5 intentos fallidos")
    void testBloqueoTras5Intentos() {
        // Given
        String email = "test@example.com";
        crearUsuario(email);
        
        // When: 5 intentos fallidos
        for (int i = 0; i < 5; i++) {
            loginAttemptService.registrarIntentoFallido(email);
        }
        
        // Then
        assertTrue(loginAttemptService.estaBloqueada(email));
        
        Usuario usuario = usuarioRepository.findByEmail(email).get();
        assertTrue(usuario.isCuentaBloqueada());
        assertEquals(5, usuario.getIntentosFallidos());
        assertNotNull(usuario.getBloqueadaHasta());
    }
    
    @Test
    @DisplayName("Cuenta se desbloquea automáticamente después del tiempo")
    void testDesbloqueoAutomatico() throws InterruptedException {
        // Given
        String email = "test@example.com";
        Usuario usuario = crearUsuario(email);
        
        // Bloquear manualmente con tiempo corto
        usuario.setCuentaBloqueada(true);
        usuario.setBloqueadaHasta(LocalDateTime.now().plusSeconds(2));
        usuarioRepository.save(usuario);
        
        // When: Esperar
        Thread.sleep(2100);
        
        // Then
        assertFalse(loginAttemptService.estaBloqueada(email));
    }
    
    @Test
    @DisplayName("Intentos se resetean tras login exitoso")
    void testReseteoIntentosExitoso() {
        // Given
        String email = "test@example.com";
        crearUsuario(email);
        
        // When
        loginAttemptService.registrarIntentoFallido(email);
        loginAttemptService.registrarIntentoFallido(email);
        loginAttemptService.resetearIntentos(email);
        
        // Then
        Usuario usuario = usuarioRepository.findByEmail(email).get();
        assertEquals(0, usuario.getIntentosFallidos());
        assertFalse(usuario.isCuentaBloqueada());
    }
}
```

---

## 📦 3. TESTS DE SEGURIDAD 2FA (1 tarea)

#### Tareas:

- [ ] **3.1** Tests de 2FA

```java
@SpringBootTest
class TwoFactorAuthTest {
    
    @Autowired
    private TwoFactorService twoFactorService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Test
    @DisplayName("Generar secret para TOTP")
    void testGenerarSecret() {
        String secret = twoFactorService.generarSecret();
        
        assertNotNull(secret);
        assertTrue(secret.length() > 0);
    }
    
    @Test
    @DisplayName("Generar QR code para Google Authenticator")
    void testGenerarQRCode() {
        Usuario usuario = crearUsuario("test@example.com");
        String secret = twoFactorService.generarSecret();
        
        String qrCode = twoFactorService.generarCodigoQR(usuario, secret);
        
        assertNotNull(qrCode);
        assertTrue(qrCode.startsWith("data:image/png;base64,"));
    }
    
    @Test
    @DisplayName("Verificar código TOTP válido")
    void testVerificarCodigoTOTPValido() {
        String secret = twoFactorService.generarSecret();
        String codigo = generarCodigoTOTP(secret); // Usar librería TOTP
        
        boolean valido = twoFactorService.verificarCodigoTOTP(secret, codigo);
        
        assertTrue(valido);
    }
    
    @Test
    @DisplayName("Rechazar código TOTP inválido")
    void testRechazarCodigoInvalido() {
        String secret = twoFactorService.generarSecret();
        
        boolean valido = twoFactorService.verificarCodigoTOTP(secret, "123456");
        
        assertFalse(valido);
    }
    
    @Test
    @DisplayName("Generar códigos de recuperación")
    void testGenerarCodigosRecuperacion() {
        List<String> codigos = twoFactorService.generarCodigosRecuperacion();
        
        assertEquals(10, codigos.size());
        
        // Todos deben ser únicos
        Set<String> uniqueCodigos = new HashSet<>(codigos);
        assertEquals(10, uniqueCodigos.size());
        
        // Todos deben tener 8 dígitos
        codigos.forEach(codigo -> {
            assertEquals(8, codigo.length());
            assertTrue(codigo.matches("\\d{8}"));
        });
    }
    
    @Test
    @DisplayName("Habilitar 2FA para usuario")
    void testHabilitar2FA() {
        Usuario usuario = crearUsuario("test@example.com");
        
        Map<String, Object> resultado = twoFactorService.habilitarTwoFactor(usuario, TwoFactorMethod.TOTP);
        
        assertNotNull(resultado.get("secret"));
        assertNotNull(resultado.get("qrCode"));
        assertNotNull(resultado.get("recoveryCodes"));
        
        Usuario usuarioActualizado = usuarioRepository.findById(usuario.getId()).get();
        assertNotNull(usuarioActualizado.getTwoFactorSecret());
        assertFalse(usuarioActualizado.isTwoFactorEnabled()); // Aún no confirmado
    }
    
    @Test
    @DisplayName("Confirmar habilitación de 2FA")
    void testConfirmarHabilitacion() {
        Usuario usuario = crearUsuario("test@example.com");
        Map<String, Object> resultado = twoFactorService.habilitarTwoFactor(usuario, TwoFactorMethod.TOTP);
        
        String secret = (String) resultado.get("secret");
        String codigo = generarCodigoTOTP(secret);
        
        twoFactorService.confirmarHabilitacion(usuario, codigo);
        
        Usuario usuarioActualizado = usuarioRepository.findById(usuario.getId()).get();
        assertTrue(usuarioActualizado.isTwoFactorEnabled());
    }
    
    @Test
    @DisplayName("Enviar código por email")
    void testEnviarCodigoEmail() {
        Usuario usuario = crearUsuario("test@example.com");
        
        twoFactorService.enviarCodigoEmail(usuario);
        
        // Verificar que se guardó en BD
        List<TwoFactorCode> codigos = twoFactorCodeRepository.findByUsuario(usuario);
        assertFalse(codigos.isEmpty());
        
        TwoFactorCode codigo = codigos.get(0);
        assertEquals(6, codigo.getCodigo().length());
        assertTrue(codigo.getFechaExpiracion().isAfter(LocalDateTime.now()));
    }
}
```

---

## 📦 4. TESTS DE JWT (1 tarea)

#### Tareas:

- [ ] **4.1** Tests de JWT y Refresh Tokens

```java
@SpringBootTest
class JwtServiceTest {
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private RefreshTokenService refreshTokenService;
    
    @Test
    @DisplayName("Generar JWT válido")
    void testGenerarJWT() {
        Usuario usuario = crearUsuario("test@example.com");
        
        String token = jwtService.generarToken(usuario);
        
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }
    
    @Test
    @DisplayName("Extraer email del JWT")
    void testExtraerEmail() {
        Usuario usuario = crearUsuario("test@example.com");
        String token = jwtService.generarToken(usuario);
        
        String email = jwtService.extraerEmail(token);
        
        assertEquals("test@example.com", email);
    }
    
    @Test
    @DisplayName("Validar JWT correcto")
    void testValidarJWT() {
        Usuario usuario = crearUsuario("test@example.com");
        String token = jwtService.generarToken(usuario);
        
        boolean valido = jwtService.validarToken(token, usuario);
        
        assertTrue(valido);
    }
    
    @Test
    @DisplayName("Rechazar JWT expirado")
    void testJWTExpirado() throws InterruptedException {
        // Configurar JWT con expiración de 1 segundo
        Usuario usuario = crearUsuario("test@example.com");
        String token = jwtService.generarToken(usuario);
        
        Thread.sleep(2000);
        
        boolean valido = jwtService.validarToken(token, usuario);
        
        assertFalse(valido);
    }
    
    @Test
    @DisplayName("Generar Refresh Token")
    void testGenerarRefreshToken() {
        Usuario usuario = crearUsuario("test@example.com");
        
        RefreshToken refreshToken = refreshTokenService.crearRefreshToken(
            usuario, "127.0.0.1", "Mozilla/5.0"
        );
        
        assertNotNull(refreshToken);
        assertNotNull(refreshToken.getToken());
        assertEquals(usuario.getId(), refreshToken.getUsuario().getId());
    }
    
    @Test
    @DisplayName("Verificar Refresh Token válido")
    void testVerificarRefreshToken() {
        Usuario usuario = crearUsuario("test@example.com");
        RefreshToken refreshToken = refreshTokenService.crearRefreshToken(
            usuario, "127.0.0.1", "Mozilla/5.0"
        );
        
        RefreshToken verificado = refreshTokenService.verificarRefreshToken(refreshToken.getToken());
        
        assertNotNull(verificado);
        assertEquals(refreshToken.getId(), verificado.getId());
    }
    
    @Test
    @DisplayName("Revocar Refresh Token")
    void testRevocarRefreshToken() {
        Usuario usuario = crearUsuario("test@example.com");
        RefreshToken refreshToken = refreshTokenService.crearRefreshToken(
            usuario, "127.0.0.1", "Mozilla/5.0"
        );
        
        refreshTokenService.revocarRefreshToken(refreshToken.getToken());
        
        assertThrows(BusinessException.class, () -> {
            refreshTokenService.verificarRefreshToken(refreshToken.getToken());
        });
    }
}
```

---

## 📦 5. TESTS DE INTEGRACIÓN (1 tarea)

#### Tareas:

- [ ] **5.1** Suite completa de integración

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegracionSeguridadTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String accessToken;
    private String refreshToken;
    
    @Test
    @Order(1)
    @DisplayName("Registro de usuario completo")
    void testRegistro() {
        RegistroDTO dto = new RegistroDTO();
        dto.setEmail("integration@example.com");
        dto.setUsername("integration");
        dto.setTelefono("12345678");
        dto.setNombre("Usuario Integración");
        dto.setPassword("Password123!");
        
        ResponseEntity<UsuarioDTO> response = restTemplate.postForEntity(
            "/api/usuarios/registro",
            dto,
            UsuarioDTO.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("integration@example.com", response.getBody().getEmail());
    }
    
    @Test
    @Order(2)
    @DisplayName("Login y obtención de tokens")
    void testLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("integration@example.com");
        request.setPassword("Password123!");
        
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/auth/login",
            request,
            AuthResponse.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        accessToken = response.getBody().getAccessToken();
        refreshToken = response.getBody().getRefreshToken();
        
        assertNotNull(accessToken);
        assertNotNull(refreshToken);
    }
    
    @Test
    @Order(3)
    @DisplayName("Acceso a recurso protegido con JWT")
    void testAccesoConJWT() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/usuario/perfil",
            HttpMethod.GET,
            entity,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    
    @Test
    @Order(4)
    @DisplayName("Refresh token para nuevo access token")
    void testRefreshToken() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);
        
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/auth/refresh",
            request,
            AuthResponse.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getAccessToken());
    }
    
    @Test
    @Order(5)
    @DisplayName("Habilitar 2FA completo")
    void testHabilitar2FACompleto() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        
        // Step 1: Iniciar habilitación
        HttpEntity<TwoFactorMethod> requestEntity = new HttpEntity<>(TwoFactorMethod.TOTP, headers);
        
        ResponseEntity<Map> response1 = restTemplate.exchange(
            "/api/usuario/seguridad/2fa/habilitar",
            HttpMethod.POST,
            requestEntity,
            Map.class
        );
        
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        String secret = (String) response1.getBody().get("secret");
        
        // Step 2: Confirmar con código
        String codigo = generarCodigoTOTP(secret);
        
        // ... continuar flujo
    }
}
```

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ Todos los tests de migración pasan  
✅ Tests de Remember Me validan funcionalidad  
✅ Tests de bloqueo de cuenta verifican comportamiento  
✅ Tests de 2FA cubren flujo completo  
✅ Tests de JWT validan generación y validación  
✅ Suite de integración ejecuta flujo end-to-end  
✅ Cobertura de tests > 80% en código de seguridad  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ FASE 2: Mejoras Técnicas
- ✅ FASE 3: Seguridad Avanzada

**Habilita:**
- 🚀 Despliegue confiable de mejoras

---

## 🔄 PRÓXIMOS PASOS

1. ✅ Ejecutar todos los tests
2. ✅ Validar cobertura > 80%
3. 🚀 Continuar con **FASE 5: Documentación**

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo QA  
**Prioridad:** ALTA - Validación crítica
