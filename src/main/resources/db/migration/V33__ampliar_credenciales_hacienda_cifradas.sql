-- ============================================================================
-- V33: Columnas de credenciales de Hacienda con espacio para el valor cifrado
--      (T-021)
-- ============================================================================
-- Las credenciales de configuracion_hacienda pasan a guardarse cifradas con
-- AES-256-GCM (CredencialCifradaConverter): "enc:v1:" + Base64(IV || cifrado +
-- tag). El valor cifrado ocupa ~4/3 del original más 44 caracteres, así que
-- password_hacienda y pin_certificado (VARCHAR(255)) pasan a TEXT, como ya eran
-- access_token y refresh_token. Se redeclaran estas dos para fijar su
-- nulabilidad.
--
-- Solo DDL, sin cambios de datos: el cifrado de los valores existentes en texto
-- plano lo hace la aplicación al arrancar (HaciendaCredencialesCifradoRunner),
-- con la clave HACIENDA_CRYPTO_KEY, que nunca llega a la base.
-- ============================================================================

ALTER TABLE configuracion_hacienda
    MODIFY password_hacienda TEXT NOT NULL,
    MODIFY pin_certificado TEXT NULL,
    MODIFY access_token TEXT NULL,
    MODIFY refresh_token TEXT NULL;
