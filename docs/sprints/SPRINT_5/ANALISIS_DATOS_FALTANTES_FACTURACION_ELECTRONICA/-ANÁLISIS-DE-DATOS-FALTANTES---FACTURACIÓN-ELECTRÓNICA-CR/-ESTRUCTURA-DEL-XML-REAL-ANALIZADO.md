## 📦 ESTRUCTURA DEL XML REAL ANALIZADO

```xml
<FacturaElectronica>
  <Clave>50616022600011820087800100001010000000001180305021</Clave>
  <ProveedorSistemas>2100042005</ProveedorSistemas>
  <CodigoActividadEmisor>4773.0</CodigoActividadEmisor>
  <CodigoActividadReceptor>1080.0</CodigoActividadReceptor>
  
  <Emisor>
    <Nombre>SOTO LEAL EMANUEL</Nombre>
    <Identificacion>
      <Tipo>01</Tipo>
      <Numero>118200878</Numero>
    </Identificacion>
    <NombreComercial>MONRACHEM</NombreComercial>
    <Ubicacion>
      <Provincia>2</Provincia>
      <Canton>01</Canton>
      <Distrito>04</Distrito>
      <Barrio>EL ROBLE</Barrio>
      <OtrasSenas>150MTS SUR DE LA ESCUELA DEL ROBLE</OtrasSenas>
    </Ubicacion>
    <CorreoElectronico>EMAIL_PLACEHOLDER</CorreoElectronico>
  </Emisor>
  
  <Receptor>
    <Nombre>PROVEEDORA DE CONCENTRADOS SOCIEDAD ANONIMA</Nombre>
    <Identificacion>
      <Tipo>02</Tipo>
      <Numero>3101032295</Numero>
    </Identificacion>
    <Ubicacion>
      <Provincia>2</Provincia>
      <Canton>01</Canton>
      <Distrito>13</Distrito>
      <OtrasSenas>ALAJUELA</OtrasSenas>
    </Ubicacion>
    <CorreoElectronico>compras@proveedora.co.cr</CorreoElectronico>
  </Receptor>
  
  <CondicionVenta>01</CondicionVenta>
  
  <DetalleServicio>
    <LineaDetalle>
      <NumeroLinea>1</NumeroLinea>
      <CodigoCABYS>3532201060000</CodigoCABYS>
      <Cantidad>15.000</Cantidad>
      <UnidadMedida>Kg</UnidadMedida>
      <Detalle>Detergentes en polvo</Detalle>
      <PrecioUnitario>1300.00000</PrecioUnitario>
      <MontoTotal>19500.00000</MontoTotal>
      <SubTotal>19500.00000</SubTotal>
      <Impuesto>
        <Codigo>01</Codigo>
        <CodigoTarifaIVA>10</CodigoTarifaIVA>
        <Tarifa>0</Tarifa>
        <Monto>0.00000</Monto>
      </Impuesto>
      <MontoTotalLinea>19500.00000</MontoTotalLinea>
    </LineaDetalle>
  </DetalleServicio>
  
  <ResumenFactura>
    <CodigoTipoMoneda>
      <CodigoMoneda>CRC</CodigoMoneda>
      <TipoCambio>1.00000</TipoCambio>
    </CodigoTipoMoneda>
    <TotalExento>74420.00000</TotalExento>
    <TotalVenta>74420.00000</TotalVenta>
    <TotalImpuesto>0.00000</TotalImpuesto>
    <MedioPago>
      <TipoMedioPago>01</TipoMedioPago>
    </MedioPago>
    <TotalComprobante>74420.00000</TotalComprobante>
  </ResumenFactura>
</FacturaElectronica>
```

---

