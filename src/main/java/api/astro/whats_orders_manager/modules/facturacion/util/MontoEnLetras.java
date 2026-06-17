package api.astro.whats_orders_manager.modules.facturacion.util;

import java.math.BigDecimal;

public final class MontoEnLetras {

    private MontoEnLetras() {}

    private static final String[] UNIDADES = {
        "", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
        "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS",
        "DIECISIETE", "DIECIOCHO", "DIECINUEVE"
    };

    private static final String[] DECENAS = {
        "", "DIEZ", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA",
        "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };

    private static final String[] CENTENAS = {
        "", "CIEN", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
        "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    public static String convertir(BigDecimal monto, String moneda) {
        if (monto == null) {
            return "CERO CON 00/100 " + moneda;
        }
        long entero = monto.longValue();
        int centavos = monto.subtract(BigDecimal.valueOf(entero)).multiply(BigDecimal.valueOf(100)).intValue();
        String letras = convertirEntero(entero);
        return letras + String.format(" CON %02d/100 ", centavos) + moneda;
    }

    private static String convertirEntero(long n) {
        if (n == 0) return "CERO";
        if (n < 0) return "MENOS " + convertirEntero(-n);

        StringBuilder sb = new StringBuilder();

        if (n >= 1_000_000L) {
            long millones = n / 1_000_000L;
            n = n % 1_000_000L;
            if (millones == 1) {
                sb.append("UN MILLÓN");
            } else {
                sb.append(convertirMenorMillon(millones)).append(" MILLONES");
            }
            if (n > 0) sb.append(" ");
        }

        if (n >= 1_000L) {
            long miles = n / 1_000L;
            n = n % 1_000L;
            if (miles == 1) {
                sb.append("MIL");
            } else {
                sb.append(convertirMenorMillon(miles)).append(" MIL");
            }
            if (n > 0) sb.append(" ");
        }

        if (n > 0) {
            sb.append(convertirMenorMillon(n));
        }

        return sb.toString().trim();
    }

    private static String convertirMenorMillon(long n) {
        if (n == 0) return "";
        StringBuilder sb = new StringBuilder();

        if (n >= 100) {
            int c = (int) (n / 100);
            n = n % 100;
            if (c == 1 && n > 0) {
                sb.append("CIENTO");
            } else {
                sb.append(CENTENAS[c]);
            }
            if (n > 0) sb.append(" ");
        }

        if (n >= 20) {
            int d = (int) (n / 10);
            int u = (int) (n % 10);
            if (u == 0) {
                sb.append(DECENAS[d]);
            } else {
                sb.append(DECENAS[d]).append(" Y ").append(UNIDADES[u]);
            }
        } else if (n > 0) {
            sb.append(UNIDADES[(int) n]);
        }

        return sb.toString().trim();
    }
}
