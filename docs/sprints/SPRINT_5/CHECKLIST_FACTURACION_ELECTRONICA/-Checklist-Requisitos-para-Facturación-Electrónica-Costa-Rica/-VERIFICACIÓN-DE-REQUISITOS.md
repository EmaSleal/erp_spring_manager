## 🔍 VERIFICACIÓN DE REQUISITOS

### Test de Certificado
```java
// Crear test en ComprobanteElectronicoController
@GetMapping("/test/certificado")
public ResponseEntity<Map<String, Object>> testCertificado() {
    try {
        CertificadoInfo info = firmaDigitalService.obtenerInfoCertificado(
            configuracion.getCertificadoPath(),
            configuracion.getCertificadoPin()
        );
        return ResponseEntity.ok(Map.of(
            "success", true,
            "titular", info.titular(),
            "cedula", info.cedula(),
            "vigente", info.vigente(),
            "expira", info.fechaExpiracion()
        ));
    } catch (Exception e) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "error", e.getMessage()
        ));
    }
}
```

### Test de API Hacienda
```java
@GetMapping("/test/api-hacienda")
public ResponseEntity<Map<String, Object>> testApiHacienda() {
    try {
        String token = haciendaApiService.obtenerToken(
            configuracion.getUsuarioApi(),
            configuracion.getPasswordApi()
        );
        return ResponseEntity.ok(Map.of(
            "success", true,
            "token_length", token.length(),
            "token_preview", token.substring(0, 20) + "..."
        ));
    } catch (Exception e) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "error", e.getMessage()
        ));
    }
}
```

### Test de XSD
```java
@GetMapping("/test/xsd")
public ResponseEntity<Map<String, Object>> testXsd() {
    List<String> archivos = Arrays.asList(
        "FacturaElectronica_V4.4.xsd",
        "TiqueteElectronico_V4.4.xsd",
        "NotaCreditoElectronica_V4.4.xsd",
        "NotaDebitoElectronica_V4.4.xsd"
    );
    
    Map<String, Boolean> resultados = new HashMap<>();
    for (String archivo : archivos) {
        File f = new File("src/main/resources/xsd/" + archivo);
        resultados.put(archivo, f.exists());
    }
    
    return ResponseEntity.ok(Map.of(
        "archivos", resultados,
        "todos_presentes", resultados.values().stream().allMatch(b -> b)
    ));
}
```

---

