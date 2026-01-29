# 🔐 FASE 3: Seguridad Avanzada (CRÍTICO)

**Sprint:** 7  
**Fase:** 3 de 5  
**Duración estimada:** 5-7 días  
**Prioridad:** ⭐⭐⭐ CRÍTICA  
**Estado:** 📋 PENDIENTE (0/28 tareas)

---

## ⚠️ CRÍTICO: SEGURIDAD EMPRESARIAL

Esta fase implementa seguridad de nivel empresarial:

1. 🔒 **Autenticación de Dos Factores (2FA/MFA)**
2. 🔑 **JWT con Refresh Tokens**
3. 🛡️ **Prevención de Ataques**
4. 🚫 **Bloqueo de Cuentas**
5. 📜 **Políticas de Contraseña**
6. 🔍 **Rate Limiting**

---

## 📋 OBJETIVO DE LA FASE

Reforzar seguridad de la aplicación:
- Implementar 2FA/MFA
- JWT con refresh tokens
- Prevención de brute force
- Rate limiting
- Políticas de contraseña robustas
- Headers de seguridad
- Protección CSRF/XSS/SQL Injection

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/28] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Autenticación 2FA/MFA           [0/7]  ░░░░░░░░░░ 0%
├─ 2. JWT + Refresh Tokens            [0/6]  ░░░░░░░░░░ 0%
├─ 3. Bloqueo de Cuentas              [0/4]  ░░░░░░░░░░ 0%
├─ 4. Políticas de Contraseña         [0/3]  ░░░░░░░░░░ 0%
├─ 5. Rate Limiting                   [0/4]  ░░░░░░░░░░ 0%
└─ 6. Headers de Seguridad            [0/4]  ░░░░░░░░░░ 0%
```

---

## 📦 1. AUTENTICACIÓN 2FA/MFA (7 tareas)

### 1.1. Descripción

Implementar **autenticación de dos factores** usando:
- **TOTP (Time-based One-Time Password)** - Google Authenticator, Authy
- **Códigos por Email** - Backup method
- **Códigos de Recuperación** - Para cuando pierde acceso al dispositivo

#### Tareas:

- [ ] **1.1.1** Agregar dependencias para TOTP

```xml
<!-- pom.xml -->
<dependency>
    <groupId>dev.samstevens.totp</groupId>
    <artifactId>totp</artifactId>
    <version>1.7.1</version>
</dependency>

<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.1</version>
</dependency>

<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.1</version>
</dependency>
```

- [ ] **1.1.2** Agregar campos 2FA a entidad `Usuario`

```java
@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {
    
    // ... campos existentes ...
    
    /**
     * Si el usuario tiene 2FA habilitado
     */
    @Column(name = "two_factor_enabled")
    private boolean twoFactorEnabled = false;
    
    /**
     * Secret para TOTP (encriptado)
     */
    @Column(name = "two_factor_secret", length = 32)
    private String twoFactorSecret;
    
    /**
     * Códigos de recuperación (JSON encriptado)
     */
    @Column(name = "recovery_codes", columnDefinition = "TEXT")
    private String recoveryCodes;
    
    /**
     * Método 2FA preferido
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "two_factor_method", length = 20)
    private TwoFactorMethod twoFactorMethod;
    
    // Getters y setters
}

enum TwoFactorMethod {
    TOTP,      // Google Authenticator, Authy
    EMAIL,     // Código por email
    SMS        // Código por SMS (futuro)
}
```

- [ ] **1.1.3** Crear migration SQL

```sql
-- Migration: MIGRATION_2FA_SPRINT_7.sql

ALTER TABLE usuarios
ADD COLUMN two_factor_enabled BOOLEAN DEFAULT FALSE,
ADD COLUMN two_factor_secret VARCHAR(32),
ADD COLUMN recovery_codes TEXT,
ADD COLUMN two_factor_method VARCHAR(20);

-- Tabla para códigos temporales de email
CREATE TABLE two_factor_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    codigo VARCHAR(6) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    usado BOOLEAN DEFAULT FALSE,
    ip_address VARCHAR(45),
    
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    
    INDEX idx_usuario (usuario_id),
    INDEX idx_codigo (codigo),
    INDEX idx_expiracion (fecha_expiracion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **1.1.4** Crear `TwoFactorService`

```java
@Service
@RequiredArgsConstructor
public class TwoFactorService {
    
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    
    private final UsuarioRepository usuarioRepository;
    private final TwoFactorCodeRepository twoFactorCodeRepository;
    private final EmailService emailService;
    
    /**
     * Genera un nuevo secret para TOTP.
     */
    public String generarSecret() {
        return secretGenerator.generate();
    }
    
    /**
     * Genera código QR para Google Authenticator.
     */
    public String generarCodigoQR(Usuario usuario, String secret) {
        QrData data = new QrData.Builder()
            .label(usuario.getEmail())
            .secret(secret)
            .issuer("ERP Orders Manager")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();
        
        return qrGenerator.getImageMimeType() + "," + 
               Base64.getEncoder().encodeToString(qrGenerator.generate(data));
    }
    
    /**
     * Verifica código TOTP.
     */
    public boolean verificarCodigoTOTP(String secret, String codigo) {
        return verifier.isValidCode(secret, codigo);
    }
    
    /**
     * Genera códigos de recuperación.
     */
    public List<String> generarCodigosRecuperacion() {
        List<String> codigos = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < 10; i++) {
            String codigo = String.format("%08d", random.nextInt(100000000));
            codigos.add(codigo);
        }
        
        return codigos;
    }
    
    /**
     * Habilita 2FA para un usuario.
     */
    @Transactional
    public Map<String, Object> habilitarTwoFactor(Usuario usuario, TwoFactorMethod metodo) {
        String secret = generarSecret();
        List<String> codigos = generarCodigosRecuperacion();
        
        usuario.setTwoFactorSecret(secret);
        usuario.setTwoFactorMethod(metodo);
        usuario.setRecoveryCodes(encriptarCodigos(codigos));
        usuario.setTwoFactorEnabled(false); // Se habilita después de verificar
        
        usuarioRepository.save(usuario);
        
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("secret", secret);
        resultado.put("qrCode", generarCodigoQR(usuario, secret));
        resultado.put("recoveryCodes", codigos);
        
        return resultado;
    }
    
    /**
     * Confirma habilitación de 2FA.
     */
    @Transactional
    public void confirmarHabilitacion(Usuario usuario, String codigo) {
        if (!verificarCodigoTOTP(usuario.getTwoFactorSecret(), codigo)) {
            throw new BusinessException("Código inválido");
        }
        
        usuario.setTwoFactorEnabled(true);
        usuarioRepository.save(usuario);
    }
    
    /**
     * Deshabilita 2FA.
     */
    @Transactional
    public void deshabilitarTwoFactor(Usuario usuario) {
        usuario.setTwoFactorEnabled(false);
        usuario.setTwoFactorSecret(null);
        usuario.setTwoFactorMethod(null);
        usuario.setRecoveryCodes(null);
        
        usuarioRepository.save(usuario);
    }
    
    /**
     * Genera y envía código por email.
     */
    public void enviarCodigoEmail(Usuario usuario) {
        // Generar código de 6 dígitos
        SecureRandom random = new SecureRandom();
        String codigo = String.format("%06d", random.nextInt(1000000));
        
        // Guardar en BD
        TwoFactorCode twoFactorCode = new TwoFactorCode();
        twoFactorCode.setUsuario(usuario);
        twoFactorCode.setCodigo(codigo);
        twoFactorCode.setFechaCreacion(LocalDateTime.now());
        twoFactorCode.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        
        twoFactorCodeRepository.save(twoFactorCode);
        
        // Enviar email
        emailService.enviarCodigoVerificacion(usuario.getEmail(), codigo);
    }
    
    /**
     * Verifica código de email.
     */
    public boolean verificarCodigoEmail(Usuario usuario, String codigo) {
        List<TwoFactorCode> codigos = twoFactorCodeRepository
            .findByUsuarioAndCodigoAndUsadoFalse(usuario, codigo);
        
        if (codigos.isEmpty()) {
            return false;
        }
        
        TwoFactorCode twoFactorCode = codigos.get(0);
        
        // Verificar expiración
        if (twoFactorCode.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        // Marcar como usado
        twoFactorCode.setUsado(true);
        twoFactorCodeRepository.save(twoFactorCode);
        
        return true;
    }
    
    private String encriptarCodigos(List<String> codigos) {
        // Implementar encriptación
        return String.join(",", codigos);
    }
}
```

- [ ] **1.1.5** Crear controlador para configuración de 2FA

```java
@Controller
@RequestMapping("/usuario/seguridad")
@RequiredArgsConstructor
public class SeguridadController {
    
    private final TwoFactorService twoFactorService;
    private final UsuarioService usuarioService;
    
    @GetMapping("/2fa")
    public String configuracion2FA(Model model) {
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        model.addAttribute("twoFactorEnabled", usuario.isTwoFactorEnabled());
        model.addAttribute("twoFactorMethod", usuario.getTwoFactorMethod());
        return "seguridad/2fa-config";
    }
    
    @PostMapping("/2fa/habilitar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> habilitar2FA(
        @RequestParam TwoFactorMethod metodo
    ) {
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        Map<String, Object> resultado = twoFactorService.habilitarTwoFactor(usuario, metodo);
        return ResponseEntity.ok(resultado);
    }
    
    @PostMapping("/2fa/confirmar")
    @ResponseBody
    public ResponseEntity<String> confirmar2FA(@RequestParam String codigo) {
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        twoFactorService.confirmarHabilitacion(usuario, codigo);
        return ResponseEntity.ok("2FA habilitado exitosamente");
    }
    
    @PostMapping("/2fa/deshabilitar")
    @ResponseBody
    public ResponseEntity<String> deshabilitar2FA(@RequestParam String codigo) {
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        
        // Verificar código antes de deshabilitar
        if (!twoFactorService.verificarCodigoTOTP(usuario.getTwoFactorSecret(), codigo)) {
            throw new BusinessException("Código inválido");
        }
        
        twoFactorService.deshabilitarTwoFactor(usuario);
        return ResponseEntity.ok("2FA deshabilitado");
    }
}
```

- [ ] **1.1.6** Integrar 2FA en proceso de login

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private TwoFactorAuthenticationFilter twoFactorFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterAfter(twoFactorFilter, UsernamePasswordAuthenticationFilter.class)
            
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(new TwoFactorAuthenticationSuccessHandler())
                .permitAll()
            );
        
        return http.build();
    }
}

@Component
public class TwoFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    
    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        
        if (usuario.isTwoFactorEnabled()) {
            // Redirigir a página de verificación 2FA
            request.getSession().setAttribute("2FA_USER", usuario.getEmail());
            response.sendRedirect("/login/2fa");
        } else {
            // Login normal
            response.sendRedirect("/dashboard");
        }
    }
}
```

- [ ] **1.1.7** Crear vista de verificación 2FA

```html
<!-- 2fa-verificacion.html -->
<div class="container mt-5">
    <div class="row justify-content-center">
        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h4>🔐 Verificación en Dos Pasos</h4>
                </div>
                <div class="card-body">
                    <p>Ingresa el código de verificación de tu aplicación autenticadora:</p>
                    
                    <form id="form2FA" method="post" th:action="@{/login/2fa/verify}">
                        <div class="mb-3">
                            <input type="text" 
                                   name="codigo" 
                                   id="codigo" 
                                   class="form-control text-center" 
                                   maxlength="6"
                                   pattern="[0-9]{6}"
                                   placeholder="000000"
                                   required
                                   autofocus>
                        </div>
                        
                        <button type="submit" class="btn btn-primary w-100">
                            Verificar
                        </button>
                        
                        <hr>
                        
                        <a href="/login/2fa/email" class="btn btn-link w-100">
                            Enviar código por email
                        </a>
                        
                        <a href="/login/2fa/recovery" class="btn btn-link w-100">
                            Usar código de recuperación
                        </a>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
// Auto-submit cuando se ingresan 6 dígitos
document.getElementById('codigo').addEventListener('input', function(e) {
    if (e.target.value.length === 6) {
        document.getElementById('form2FA').submit();
    }
});
</script>
```

---

## 📦 2. JWT CON REFRESH TOKENS (6 tareas)

### 2.1. Descripción

Implementar **JWT (JSON Web Tokens)** para autenticación stateless con **refresh tokens** para renovación segura.

#### Tareas:

- [ ] **2.1.1** Agregar dependencias JWT

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **2.1.2** Crear tabla para refresh tokens

```sql
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    fecha_expiracion DATETIME NOT NULL,
    revocado BOOLEAN DEFAULT FALSE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    fecha_creacion DATETIME NOT NULL,
    
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    
    INDEX idx_token (token),
    INDEX idx_usuario (usuario_id),
    INDEX idx_expiracion (fecha_expiracion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **2.1.3** Crear `JwtService`

```java
@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration:3600000}") // 1 hora por defecto
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration:2592000000}") // 30 días
    private long refreshExpiration;
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Genera JWT access token.
     */
    public String generarToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", usuario.getEmail());
        claims.put("roles", usuario.getRoles().stream()
            .map(r -> r.getNombre())
            .collect(Collectors.toList()));
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(usuario.getEmail())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    /**
     * Genera refresh token.
     */
    public String generarRefreshToken(Usuario usuario) {
        return Jwts.builder()
            .setSubject(usuario.getEmail())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    /**
     * Extrae email del token.
     */
    public String extraerEmail(String token) {
        return extraerClaim(token, Claims::getSubject);
    }
    
    /**
     * Extrae claim del token.
     */
    public <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extraerTodosClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    /**
     * Valida token.
     */
    public boolean validarToken(String token, UserDetails userDetails) {
        final String email = extraerEmail(token);
        return (email.equals(userDetails.getUsername()) && !tokenExpirado(token));
    }
    
    private boolean tokenExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }
}
```

- [ ] **2.1.4** Crear `RefreshTokenService`

```java
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    
    /**
     * Crea refresh token.
     */
    @Transactional
    public RefreshToken crearRefreshToken(Usuario usuario, String ipAddress, String userAgent) {
        String tokenString = jwtService.generarRefreshToken(usuario);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenString);
        refreshToken.setUsuario(usuario);
        refreshToken.setFechaExpiracion(LocalDateTime.now().plusDays(30));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setFechaCreacion(LocalDateTime.now());
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    /**
     * Verifica y obtiene refresh token.
     */
    public RefreshToken verificarRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new BusinessException("Refresh token no encontrado"));
        
        if (refreshToken.isRevocado()) {
            throw new BusinessException("Refresh token revocado");
        }
        
        if (refreshToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token expirado");
        }
        
        return refreshToken;
    }
    
    /**
     * Revoca refresh token.
     */
    @Transactional
    public void revocarRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new BusinessException("Refresh token no encontrado"));
        
        refreshToken.setRevocado(true);
        refreshTokenRepository.save(refreshToken);
    }
    
    /**
     * Revoca todos los refresh tokens de un usuario.
     */
    @Transactional
    public void revocarTodosLosTokens(Usuario usuario) {
        refreshTokenRepository.findByUsuarioAndRevocadoFalse(usuario)
            .forEach(token -> {
                token.setRevocado(true);
                refreshTokenRepository.save(token);
            });
    }
}
```

- [ ] **2.1.5** Crear filtro JWT

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String jwt = authHeader.substring(7);
        final String userEmail = jwtService.extraerEmail(jwt);
        
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            
            if (jwtService.validarToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **2.1.6** Crear endpoint para refresh

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );
        
        Usuario usuario = (Usuario) authentication.getPrincipal();
        
        String accessToken = jwtService.generarToken(usuario);
        RefreshToken refreshToken = refreshTokenService.crearRefreshToken(
            usuario,
            request.getIpAddress(),
            request.getUserAgent()
        );
        
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.verificarRefreshToken(request.getRefreshToken());
        
        String accessToken = jwtService.generarToken(refreshToken.getUsuario());
        
        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
    }
}
```

---

## 📦 3. BLOQUEO DE CUENTAS (4 tareas)

### 3.1. Descripción

Implementar **bloqueo automático de cuentas** después de múltiples intentos fallidos de login.

#### Tareas:

- [ ] **3.1.1** Agregar campos a entidad `Usuario`

```java
@Column(name = "intentos_fallidos")
private int intentosFallidos = 0;

@Column(name = "cuenta_bloqueada")
private boolean cuentaBloqueada = false;

@Column(name = "fecha_bloqueo")
private LocalDateTime fechaBloqueo;

@Column(name = "bloqueada_hasta")
private LocalDateTime bloqueadaHasta;
```

- [ ] **3.1.2** Crear `LoginAttemptService`

```java
@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    
    private static final int MAX_INTENTOS = 5;
    private static final int DURACION_BLOQUEO_MINUTOS = 30;
    
    /**
     * Registra intento fallido.
     */
    @Transactional
    public void registrarIntentoFallido(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            // Incrementar intentos
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            
            // Bloquear si excede max intentos
            if (usuario.getIntentosFallidos() >= MAX_INTENTOS) {
                usuario.setCuentaBloqueada(true);
                usuario.setFechaBloqueo(LocalDateTime.now());
                usuario.setBloqueadaHasta(LocalDateTime.now().plusMinutes(DURACION_BLOQUEO_MINUTOS));
                
                auditoriaService.registrarEvento(
                    TipoEventoAuditoria.SEGURIDAD,
                    usuario,
                    "CUENTA_BLOQUEADA",
                    "Usuario",
                    usuario.getId(),
                    null,
                    null,
                    "Cuenta bloqueada por múltiples intentos fallidos"
                );
            }
            
            usuarioRepository.save(usuario);
        }
    }
    
    /**
     * Resetea intentos fallidos.
     */
    @Transactional
    public void resetearIntentos(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);
        }
    }
    
    /**
     * Verifica si cuenta está bloqueada.
     */
    public boolean estaBloqueada(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        
        if (usuarioOpt.isEmpty()) {
            return false;
        }
        
        Usuario usuario = usuarioOpt.get();
        
        if (!usuario.isCuentaBloqueada()) {
            return false;
        }
        
        // Verificar si expiró el bloqueo
        if (usuario.getBloqueadaHasta().isBefore(LocalDateTime.now())) {
            desbloquear(usuario);
            return false;
        }
        
        return true;
    }
    
    @Transactional
    private void desbloquear(Usuario usuario) {
        usuario.setCuentaBloqueada(false);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
        usuario.setBloqueadaHasta(null);
        usuarioRepository.save(usuario);
    }
}
```

- [ ] **3.1.3** Integrar en AuthenticationFailureHandler

```java
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    
    private final LoginAttemptService loginAttemptService;
    
    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        
        String email = request.getParameter("username");
        
        loginAttemptService.registrarIntentoFallido(email);
        
        response.sendRedirect("/login?error=true");
    }
}
```

- [ ] **3.1.4** Validar cuenta bloqueada en login

```java
@Override
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    // Verificar bloqueo
    if (loginAttemptService.estaBloqueada(email)) {
        throw new LockedException("Cuenta bloqueada temporalmente. Intenta más tarde.");
    }
    
    Usuario usuario = usuarioRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    
    return usuario;
}
```

---

## 📦 4. POLÍTICAS DE CONTRASEÑA (3 tareas)

#### Tareas:

- [ ] **4.1** Implementar validador de contraseña robusto

```java
@Component
public class PasswordPolicyValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final int MIN_UPPERCASE = 1;
    private static final int MIN_LOWERCASE = 1;
    private static final int MIN_DIGITS = 1;
    private static final int MIN_SPECIAL = 1;
    
    public void validar(String password) {
        if (password.length() < MIN_LENGTH) {
            throw new BusinessException("La contraseña debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("La contraseña debe contener al menos una mayúscula");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("La contraseña debe contener al menos una minúscula");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new BusinessException("La contraseña debe contener al menos un número");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new BusinessException("La contraseña debe contener al menos un carácter especial");
        }
    }
}
```

- [ ] **4.2** Implementar historial de contraseñas

```sql
CREATE TABLE password_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    fecha_cambio DATETIME NOT NULL,
    
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_usuario (usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **4.3** Forzar cambio de contraseña periódicamente

```java
@Column(name = "password_expira_en")
private LocalDateTime passwordExpiraEn;

// Al cambiar contraseña:
usuario.setPasswordExpiraEn(LocalDateTime.now().plusMonths(3));
```

---

## 📦 5. RATE LIMITING (4 tareas)

#### Tareas:

- [ ] **5.1** Configurar Bucket4j para rate limiting

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>
```

- [ ] **5.2** Crear interceptor de rate limiting

- [ ] **5.3** Aplicar límites por IP

- [ ] **5.4** Aplicar límites por usuario

---

## 📦 6. HEADERS DE SEGURIDAD (4 tareas)

#### Tareas:

- [ ] **6.1** Configurar headers de seguridad en Spring Security

```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"))
    .xssProtection(xss -> xss.headerValue("1; mode=block"))
    .frameOptions(frame -> frame.deny())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))
);
```

- [ ] **6.2** Habilitar HTTPS en producción

- [ ] **6.3** Configurar CORS correctamente

- [ ] **6.4** Protección contra clickjacking

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ 2FA/MFA funcionando con TOTP  
✅ JWT con refresh tokens implementado  
✅ Cuentas se bloquean tras 5 intentos fallidos  
✅ Políticas de contraseña robustas  
✅ Rate limiting activo  
✅ Headers de seguridad configurados  
✅ Auditoría de eventos de seguridad  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ FASE 2: Mejoras Técnicas (auditoría)

**Habilita:**
- 🚀 Seguridad nivel empresarial
- 🚀 Protección contra ataques comunes

---

## 🔄 PRÓXIMOS PASOS

1. ✅ Configurar 2FA primero
2. ✅ Implementar JWT
3. 🚀 Continuar con **FASE 4: Testing**

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de Desarrollo  
**Prioridad:** CRÍTICA - Seguridad empresarial
