-- ============================================================================
-- V34: Columna smtp_password con espacio para el valor cifrado (T-022, ADR-026)
-- ============================================================================
-- La contraseña SMTP de configuracion_email pasa a guardarse cifrada con
-- CredencialCifradaConverter, como las credenciales de Hacienda (V33):
-- "enc:v1:" + Base64(IV || cifrado + tag). El valor cifrado ocupa ~4/3 del
-- original más 44 caracteres; en VARCHAR(255) solo cabrían contraseñas de hasta
-- ~158 caracteres, así que pasa a TEXT, igual que en V33.
--
-- Solo DDL, sin cambios de datos: el cifrado de los valores existentes (texto
-- plano o el formato anterior "{enc}") lo hace la aplicación al arrancar
-- (SmtpPasswordCifradoRunner), con la clave HACIENDA_CRYPTO_KEY, que nunca llega
-- a la base.
-- ============================================================================

ALTER TABLE configuracion_email
    MODIFY smtp_password TEXT NOT NULL;
