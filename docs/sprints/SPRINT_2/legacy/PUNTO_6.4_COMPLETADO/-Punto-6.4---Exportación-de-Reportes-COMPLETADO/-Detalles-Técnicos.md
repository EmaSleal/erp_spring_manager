## 🔍 Detalles Técnicos

### **Resolución de Conflicto de Nombres**

**Problema:** Conflicto entre `Cell` de iText (PDF) y `Cell` de Apache POI (Excel)

**Solución:**
```java
// En imports - eliminar:
import com.itextpdf.layout.element.Cell;

// En código - usar nombre completo para iText:
private com.itextpdf.layout.element.Cell crearCeldaHeader(String texto, DeviceRgb color) {
    return new com.itextpdf.layout.element.Cell()
            .add(new Paragraph(texto).setBold().setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(color)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(5);
}

// Apache POI usa automáticamente org.apache.poi.ss.usermodel.Cell
```

### **Formato de Fechas**

```java
private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
```

### **Formato de Moneda**

**PDF:**
```java
private String formatearMoneda(BigDecimal valor) {
    if (valor == null) return "S/ 0.00";
    return String.format("S/ %.2f", valor);
}
```

**Excel:**
```java
private CellStyle crearEstiloMoneda(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    style.setDataFormat(format.getFormat("S/ #,##0.00"));
    return style;
}
```

---

