## Conclusión

La arquitectura de cards se basa en estos principios:

1. **Tabla = Fuente de Verdad** durante toda la sesión
2. **Cards = Representación Visual** de la tabla en móvil
3. **Sincronización Selectiva** evita reconstrucciones completas
4. **IDs Negativos Decrecientes** para líneas temporales
5. **Localización Versátil** permite eventos desde cualquier vista
6. **Async Control con Promise** para secuenciar factura → líneas
7. **Eliminar sin Reconstruir** para preservar UX

Con estos patrones, la UI se mantiene responsiva y los datos siempre consistentes entre desktop y móvil.
