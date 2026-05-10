## 📚 CATÁLOGOS NECESARIOS

### 1. Provincias de Costa Rica
```java
public enum Provincia {
    SAN_JOSE("1", "San José"),
    ALAJUELA("2", "Alajuela"),
    CARTAGO("3", "Cartago"),
    HEREDIA("4", "Heredia"),
    GUANACASTE("5", "Guanacaste"),
    PUNTARENAS("6", "Puntarenas"),
    LIMON("7", "Limón");
}
```

### 2. Unidades de Medida Comunes
```
- Unid (Unidad)
- Kg (Kilogramo)
- g (Gramo)
- L (Litro)
- mL (Mililitro)
- m (Metro)
- cm (Centímetro)
- Caja
- Paquete
- Botella
- Galon (Galón)
```

### 3. Códigos CABYS
**Importante:** El código CABYS es de 13 dígitos y es OBLIGATORIO.
- Debe consultarse en: https://www.hacienda.go.cr/ATV/ComprobanteElectronico/docs/esquemas/v43/Cabys/SistemaCABYS_versioned.xlsx
- Ejemplos del XML real:
  * `3532201060000` - Detergentes en polvo
  * `3219301000000` - Papel higiénico
  * `3466499990100` - Desinfectante bactericida

---

