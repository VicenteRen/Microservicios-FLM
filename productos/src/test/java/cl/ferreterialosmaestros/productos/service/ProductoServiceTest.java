package cl.ferreterialosmaestros.productos.service;

import cl.ferreterialosmaestros.productos.model.Producto;
import cl.ferreterialosmaestros.productos.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @InjectMocks
    private ProductoService service;

    @Test
    void listarProductos_retornaListaCompleta() {
        // preparar
        Producto p1 = new Producto();
        p1.setId(1L);
        p1.setCodigo("HE001");
        p1.setNombre("Taladro Percutor 850W");

        Producto p2 = new Producto();
        p2.setId(2L);
        p2.setCodigo("HM002");
        p2.setNombre("Huincha de medir 8m");

        when(repository.findAll()).thenReturn(List.of(p1, p2));

        // ejecucion
        List<Producto> resultado = service.listarProductos();

        // comprobar
        assertEquals(2, resultado.size());
        assertEquals("HE001", resultado.get(0).getCodigo());
        verify(repository, times(1)).findAll();
    }

    @Test
    void obtenerProductoPorId_conIdExistente_retornaProducto() {
        // preparar
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Taladro Percutor 850W");

        when(repository.findById(1L)).thenReturn(Optional.of(producto));

        // ejecucion
        Producto resultado = service.obtenerProductoPorId(1L);

        // comprobar
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Taladro Percutor 850W", resultado.getNombre());
    }

    @Test
    void obtenerProductoPorId_conIdInexistente_retornaNull() {
        // preparar
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // ejecucion
        Producto resultado = service.obtenerProductoPorId(99L);

        // comprobar
        assertNull(resultado);
    }

    @Test
    void guardarProducto_conPrecioCompraValido_mantienePrecioCompraOriginal() {
        // preparar
        Producto nuevo = new Producto();
        nuevo.setNombre("Sierra Circular");
        nuevo.setPrecio(235000.0);
        nuevo.setPrecioCompra(150000.0);

        when(repository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        // ejecucion
        Producto resultado = service.guardarProducto(nuevo);

        // comprobar
        assertEquals(150000.0, resultado.getPrecioCompra());
        assertEquals(235000.0, resultado.getPrecio());
        verify(repository, times(1)).save(any(Producto.class));
    }

    @Test
    void guardarProducto_conPrecioCompraNull_igualaAlPrecioDeVenta() {
        // preparar
        Producto nuevo = new Producto();
        nuevo.setNombre("Cortadora Manual");
        nuevo.setPrecio(15000.0);
        nuevo.setPrecioCompra(null);

        when(repository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        // ejecucion
        Producto resultado = service.guardarProducto(nuevo);

        // comprobar
        assertEquals(15000.0, resultado.getPrecioCompra());
        verify(repository, times(1)).save(any(Producto.class));
    }

    @Test
    void guardarProducto_conPrecioCompraCero_igualaAlPrecioDeVenta() {
        // preparar
        Producto nuevo = new Producto();
        nuevo.setNombre("Martillo Galponero");
        nuevo.setPrecio(9500.0);
        nuevo.setPrecioCompra(0.0);

        when(repository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        // ejecucion
        Producto resultado = service.guardarProducto(nuevo);

        // comprobar
        assertEquals(9500.0, resultado.getPrecioCompra());
        verify(repository, times(1)).save(any(Producto.class));
    }

    @Test
    void eliminarProductoPorId_invocaEliminacionEnRepositorio() {
        // preparar
        doNothing().when(repository).deleteById(1L);

        // ejecucion
        service.eliminarProductoPorId(1L);

        // comprobar
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void modificarStock_conVariacionNegativaValida_descuentaStock() {
        when(repository.modificarStock(1L, -2)).thenReturn(1);

        assertDoesNotThrow(() -> service.modificarStock(1L, -2));

        verify(repository, times(1)).modificarStock(1L, -2);
        verify(repository, never()).existsById(anyLong());
    }

    @Test
    void modificarStock_conVariacionPositivaValida_restituyeStock() {
        when(repository.modificarStock(1L, 3)).thenReturn(1);

        assertDoesNotThrow(() -> service.modificarStock(1L, 3));

        verify(repository, times(1)).modificarStock(1L, 3);
        verify(repository, never()).existsById(anyLong());
    }

    @Test
    void modificarStock_conVariacionCero_lanzaBadRequest() {
        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.modificarStock(1L, 0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(repository, never()).modificarStock(anyLong(), anyInt());
    }

    @Test
    void modificarStock_conProductoInexistente_lanzaNotFound() {
        when(repository.modificarStock(99L, -2)).thenReturn(0);
        when(repository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.modificarStock(99L, -2)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void modificarStock_conStockInsuficiente_lanzaConflict() {
        when(repository.modificarStock(1L, -50)).thenReturn(0);
        when(repository.existsById(1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> service.modificarStock(1L, -50)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}