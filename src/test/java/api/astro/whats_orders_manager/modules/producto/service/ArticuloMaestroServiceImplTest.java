package api.astro.whats_orders_manager.modules.producto.service;

import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ArticuloMaestroRepository;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.producto.service.impl.ArticuloMaestroServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ArticuloMaestroServiceImpl.
 *
 * Validates:
 * - save() write-through to all variant deprecated fields
 * - deactivate() cascades active=false to master and all variants
 * - reactivate() cascades active=true to master and all variants
 * - deleteById() guard: throws when variants exist
 * - deleteById() success: delegates to repository when no variants
 *
 * No Spring context loaded — pure Mockito, fast feedback loop.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArticuloMaestroServiceImpl — unit tests")
@SuppressWarnings("deprecation")
class ArticuloMaestroServiceImplTest {

    @Mock
    private ArticuloMaestroRepository maestroRepository;

    @Mock
    private ProductoRepository productoRepository;

    private ArticuloMaestroService service;

    @BeforeEach
    void setUp() {
        service = new ArticuloMaestroServiceImpl(maestroRepository, productoRepository);
    }

    // =========================================================================
    // save() — write-through
    // =========================================================================

    @Test
    @DisplayName("save() must propagate all common/FE fields to every linked variant")
    void testSave_writesThroughAllFieldsToVariants() {
        // Arrange
        ArticuloMaestro master = new ArticuloMaestro();
        master.setIdArticuloMaestro(1);
        master.setDescripcion("Jugo de naranja");
        master.setActive(true);
        master.setGravado(true);
        master.setPorcentajeImpuesto(new BigDecimal("13.00"));
        master.setAplicaOtroImpuesto(false);
        master.setCodigoCabys("2132100000100");
        master.setDescripcionCabys("Jugo de naranja concentrado");

        Producto variant1 = new Producto();
        variant1.setDescripcion("Old name 1");
        variant1.setActive(false);

        Producto variant2 = new Producto();
        variant2.setDescripcion("Old name 2");
        variant2.setActive(false);

        List<Producto> variants = List.of(variant1, variant2);

        when(maestroRepository.save(master)).thenReturn(master);
        when(productoRepository.findByArticuloMaestro_IdArticuloMaestro(1)).thenReturn(variants);

        // Act
        ArticuloMaestro result = service.save(master);

        // Assert — master returned as-is
        assertSame(master, result);

        // Assert — deprecated fields updated on all variants
        assertEquals("Jugo de naranja", variant1.getDescripcion());
        assertEquals("Jugo de naranja", variant2.getDescripcion());
        assertTrue(variant1.getActive());
        assertTrue(variant2.getActive());
        assertTrue(variant1.getGravado());
        assertTrue(variant2.getGravado());
        assertEquals(new BigDecimal("13.00"), variant1.getPorcentajeImpuesto());
        assertEquals(new BigDecimal("13.00"), variant2.getPorcentajeImpuesto());
        assertFalse(variant1.getAplicaOtroImpuesto());
        assertEquals("2132100000100", variant1.getCodigoCabys());
        assertEquals("Jugo de naranja concentrado", variant1.getDescripcionCabys());

        verify(productoRepository).saveAll(variants);
    }

    @Test
    @DisplayName("save() must not call saveAll when there are no linked variants")
    void testSave_noVariants_doesNotCallSaveAll() {
        ArticuloMaestro master = new ArticuloMaestro();
        master.setIdArticuloMaestro(2);
        master.setDescripcion("Nuevo producto");

        when(maestroRepository.save(master)).thenReturn(master);
        when(productoRepository.findByArticuloMaestro_IdArticuloMaestro(2))
                .thenReturn(Collections.emptyList());

        service.save(master);

        verify(productoRepository, never()).saveAll(anyList());
    }

    // =========================================================================
    // deactivate()
    // =========================================================================

    @Test
    @DisplayName("deactivate() must set master.active=false and all variants active=false")
    void testDeactivate_cascadesToMasterAndAllVariants() {
        // Arrange
        ArticuloMaestro master = new ArticuloMaestro();
        master.setIdArticuloMaestro(1);
        master.setActive(true);

        Producto v1 = new Producto();
        v1.setActive(true);
        Producto v2 = new Producto();
        v2.setActive(true);
        Producto v3 = new Producto();
        v3.setActive(true);

        List<Producto> variants = List.of(v1, v2, v3);

        when(maestroRepository.findById(1)).thenReturn(Optional.of(master));
        when(productoRepository.findByArticuloMaestro_IdArticuloMaestro(1)).thenReturn(variants);

        // Act
        service.deactivate(1);

        // Assert — master deactivated
        assertFalse(master.getActive(), "Master must be deactivated");

        // Assert — all variants deactivated
        assertFalse(v1.getActive(), "Variant 1 must be deactivated");
        assertFalse(v2.getActive(), "Variant 2 must be deactivated");
        assertFalse(v3.getActive(), "Variant 3 must be deactivated");

        verify(maestroRepository).save(master);
        verify(productoRepository).saveAll(variants);
    }

    @Test
    @DisplayName("deactivate() must throw IllegalArgumentException when master not found")
    void testDeactivate_masterNotFound_throwsIllegalArgument() {
        when(maestroRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.deactivate(99));
    }

    // =========================================================================
    // reactivate()
    // =========================================================================

    @Test
    @DisplayName("reactivate() must set master.active=true and all variants active=true")
    void testReactivate_cascadesToMasterAndAllVariants() {
        // Arrange
        ArticuloMaestro master = new ArticuloMaestro();
        master.setIdArticuloMaestro(1);
        master.setActive(false);

        Producto v1 = new Producto();
        v1.setActive(false);
        Producto v2 = new Producto();
        v2.setActive(false);

        List<Producto> variants = List.of(v1, v2);

        when(maestroRepository.findById(1)).thenReturn(Optional.of(master));
        when(productoRepository.findByArticuloMaestro_IdArticuloMaestro(1)).thenReturn(variants);

        // Act
        service.reactivate(1);

        // Assert — master reactivated
        assertTrue(master.getActive(), "Master must be reactivated");

        // Assert — all variants reactivated
        assertTrue(v1.getActive(), "Variant 1 must be reactivated");
        assertTrue(v2.getActive(), "Variant 2 must be reactivated");

        verify(maestroRepository).save(master);
        verify(productoRepository).saveAll(variants);
    }

    @Test
    @DisplayName("reactivate() must throw IllegalArgumentException when master not found")
    void testReactivate_masterNotFound_throwsIllegalArgument() {
        when(maestroRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.reactivate(99));
    }

    // =========================================================================
    // deleteById() — variant guard
    // =========================================================================

    @Test
    @DisplayName("deleteById() must throw IllegalStateException when variants exist")
    void testDeleteById_withVariants_throwsIllegalState() {
        Producto existingVariant = new Producto();
        when(productoRepository.findByArticuloMaestro_IdArticuloMaestro(1))
                .thenReturn(List.of(existingVariant));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.deleteById(1)
        );

        assertTrue(ex.getMessage().contains("Cannot delete master with existing variants"));
        // Repository must NOT be asked to delete
        verify(maestroRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteById() must call repository.deleteById when no variants exist")
    void testDeleteById_noVariants_deletesSuccessfully() {
        when(productoRepository.findByArticuloMaestro_IdArticuloMaestro(1))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> service.deleteById(1));

        verify(maestroRepository).deleteById(1);
    }

    // =========================================================================
    // deleteVariante() — last-variant guard
    // =========================================================================

    @Test
    @DisplayName("deleteVariante() must throw IllegalStateException when trying to delete the last variant")
    void deleteVariante_throwsWhenLastVariant() {
        Producto lastVariant = new Producto();
        when(productoRepository.findByArticuloMaestro_IdArticuloMaestro(1))
                .thenReturn(List.of(lastVariant));

        assertThrows(IllegalStateException.class, () -> service.deleteVariante(1, 10));
        verify(productoRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("deleteVariante() must call deleteById when multiple variants exist")
    void deleteVariante_succeedsWhenMultipleVariants() {
        Producto v1 = new Producto();
        Producto v2 = new Producto();
        when(productoRepository.findByArticuloMaestro_IdArticuloMaestro(1))
                .thenReturn(List.of(v1, v2));

        assertDoesNotThrow(() -> service.deleteVariante(1, 10));
        verify(productoRepository).deleteById(10);
    }
}
