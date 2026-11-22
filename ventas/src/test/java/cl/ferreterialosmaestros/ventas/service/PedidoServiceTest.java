package cl.ferreterialosmaestros.ventas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import cl.ferreterialosmaestros.ventas.client.ProductoClient;
import cl.ferreterialosmaestros.ventas.dto.PedidoRequest;
import cl.ferreterialosmaestros.ventas.dto.PedidoResponse;
import cl.ferreterialosmaestros.ventas.dto.ProductoDTO;
import cl.ferreterialosmaestros.ventas.model.DetallePedido;
import cl.ferreterialosmaestros.ventas.model.Pedido;
import cl.ferreterialosmaestros.ventas.repository.PedidoRepository;
import feign.FeignException;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repository;

    @Mock
    private ProductoClient productoClient;

    private PedidoService service;

    private Pedido pedidoBase;

    @BeforeEach
    void setUp() {
        TransactionTemplate transacciones = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }

            @Override
            public void executeWithoutResult(Consumer<TransactionStatus> action) {
                action.accept(new SimpleTransactionStatus());
            }
        };

        service = new PedidoService(repository, productoClient, transacciones);

        pedidoBase = new Pedido();
        pedidoBase.setId(1L);
        pedidoBase.setNombreCliente("Juan");
        pedidoBase.setApellidosCliente("Pérez");
        pedidoBase.setCorreoCliente("juan@ejemplo.cl");
        pedidoBase.setDireccionEnvio("Av. Los Maestros 123");
        pedidoBase.setRegion("Metropolitana");
        pedidoBase.setComuna("Santiago");
        pedidoBase.setEstado("CONFIRMADO");
        pedidoBase.setFechaPedido(LocalDateTime.of(2026, 11, 22, 18, 0));

        DetallePedido detalle = new DetallePedido();
        detalle.setProductoId(1L);
        detalle.setNombreProducto("Taladro Percutor");
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("10000.00"));
        detalle.setSubtotal(new BigDecimal("20000.00"));
        pedidoBase.agregarDetalle(detalle);
        pedidoBase.setTotal(new BigDecimal("20000.00"));
    }

    private ProductoDTO crearProducto(Long id, String nombre, Double precio, Integer stock) {
        ProductoDTO dto = new ProductoDTO();
        dto.setId(id);
        dto.setNombre(nombre);
        dto.setPrecio(precio);
        dto.setStock(stock);
        return dto;
    }

    private PedidoRequest crearSolicitudValida(List<PedidoRequest.Item> items) {
        return new PedidoRequest(
                "  Juan ",
                " Pérez  ",
                "juan@ejemplo.cl",
                "Av. Los Maestros 123",
                "Metropolitana",
                "Santiago",
                items
        );
    }

    // --- 1. CREAR PEDIDO ---

    @Test
    void crear_ConDatosValidos_DebeCalcularTotalDescontarStockYGuardar() {
        PedidoRequest request = crearSolicitudValida(List.of(
                new PedidoRequest.Item(1L, 2),
                new PedidoRequest.Item(2L, 1)
        ));

        when(productoClient.obtenerProductoPorId(1L))
                .thenReturn(crearProducto(1L, "Taladro", 10000.0, 10));
        when(productoClient.obtenerProductoPorId(2L))
                .thenReturn(crearProducto(2L, "Broca", 5500.5, 5));
        when(repository.saveAndFlush(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });

        PedidoResponse respuesta = service.crear(request);

        assertNotNull(respuesta);
        assertEquals(10L, respuesta.id());
        assertEquals("Juan", respuesta.nombreCliente());
        assertEquals("CONFIRMADO", respuesta.estado());
        assertEquals(new BigDecimal("25500.50"), respuesta.total());
        assertEquals(2, respuesta.productos().size());

        verify(productoClient).modificarStock(1L, -2);
        verify(productoClient).modificarStock(2L, -1);
        verify(repository).saveAndFlush(any(Pedido.class));
    }

    @Test
    void crear_CuandoSolicitudTieneDatosInvalidos_DebeLanzar400() {
        ResponseStatusException exNull = assertThrows(
                ResponseStatusException.class, () -> service.crear(null));
        assertEquals(HttpStatus.BAD_REQUEST, exNull.getStatusCode());

        PedidoRequest correoInvalido = new PedidoRequest(
                "Juan", "Pérez", "correo-malo", "Dir 1", "RM", "Santiago",
                List.of(new PedidoRequest.Item(1L, 1)));
        ResponseStatusException exCorreo = assertThrows(
                ResponseStatusException.class, () -> service.crear(correoInvalido));
        assertEquals(HttpStatus.BAD_REQUEST, exCorreo.getStatusCode());

        PedidoRequest sinProductos = crearSolicitudValida(List.of());
        ResponseStatusException exVacio = assertThrows(
                ResponseStatusException.class, () -> service.crear(sinProductos));
        assertEquals(HttpStatus.BAD_REQUEST, exVacio.getStatusCode());

        PedidoRequest cantidadInvalida = crearSolicitudValida(
                List.of(new PedidoRequest.Item(1L, 0)));
        ResponseStatusException exCantidad = assertThrows(
                ResponseStatusException.class, () -> service.crear(cantidadInvalida));
        assertEquals(HttpStatus.BAD_REQUEST, exCantidad.getStatusCode());

        PedidoRequest duplicados = crearSolicitudValida(List.of(
                new PedidoRequest.Item(1L, 1),
                new PedidoRequest.Item(1L, 2)
        ));
        ResponseStatusException exDuplicado = assertThrows(
                ResponseStatusException.class, () -> service.crear(duplicados));
        assertEquals(HttpStatus.BAD_REQUEST, exDuplicado.getStatusCode());
    }

    @Test
    void crear_CuandoProductoNoExisteEnMsProductos_DebeLanzar404() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(99L, 1)));
        FeignException feign404 = mock(FeignException.class);
        when(feign404.status()).thenReturn(404);
        when(productoClient.obtenerProductoPorId(99L)).thenThrow(feign404);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void crear_CuandoMsProductosNoDisponible_DebeLanzar503() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 1)));
        FeignException feign503 = mock(FeignException.class);
        when(feign503.status()).thenReturn(503);
        when(productoClient.obtenerProductoPorId(1L)).thenThrow(feign503);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void crear_CuandoDatosDeProductoSonInvalidos_DebeLanzar409() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 1)));
        when(productoClient.obtenerProductoPorId(1L))
                .thenReturn(crearProducto(1L, "Taladro", -500.0, 10));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void crear_CuandoStockEsInsuficiente_DebeLanzar409() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 5)));
        when(productoClient.obtenerProductoPorId(1L))
                .thenReturn(crearProducto(1L, "Taladro", 10000.0, 2));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void crear_CuandoMontoSuperaLimite_DebeLanzar400() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 20)));
        when(productoClient.obtenerProductoPorId(1L))
                .thenReturn(crearProducto(1L, "Maquinaria", 1_000_000_000_000.0, 50));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void crear_CuandoFallaSegundoDescuentoCon409_DebeCompensarElPrimero() {
        PedidoRequest request = crearSolicitudValida(List.of(
                new PedidoRequest.Item(1L, 2),
                new PedidoRequest.Item(2L, 1)
        ));

        when(productoClient.obtenerProductoPorId(1L))
                .thenReturn(crearProducto(1L, "Taladro", 10000.0, 10));
        when(productoClient.obtenerProductoPorId(2L))
                .thenReturn(crearProducto(2L, "Broca", 2000.0, 5));

        FeignException feign409 = mock(FeignException.class);
        when(feign409.status()).thenReturn(409);
        lenient().doThrow(feign409).when(productoClient).modificarStock(2L, -1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(productoClient).modificarStock(1L, -2);
        verify(productoClient).modificarStock(1L, 2); // Compensación
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void crear_CuandoMoverStockRetorna404_DebeLanzar404() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 1)));
        when(productoClient.obtenerProductoPorId(1L))
                .thenReturn(crearProducto(1L, "Taladro", 10000.0, 10));

        FeignException feign404 = mock(FeignException.class);
        when(feign404.status()).thenReturn(404);
        doThrow(feign404).when(productoClient).modificarStock(1L, -1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void crear_CuandoGuardadoRetornaNullYFallaCompensacion_DebeLanzar500() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 2)));
        when(productoClient.obtenerProductoPorId(1L))
                .thenReturn(crearProducto(1L, "Taladro", 10000.0, 10));
        when(repository.saveAndFlush(any(Pedido.class))).thenReturn(null);

        FeignException feign500 = mock(FeignException.class);
        when(feign500.status()).thenReturn(500);
        lenient().doThrow(feign500).when(productoClient).modificarStock(1L, 2);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.crear(request));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }

    // --- 2. LISTAR Y OBTENER ---

    @Test
    void listar_DebeRetornarPedidosOrdenadosPorIdDesc() {
        when(repository.findAll(Sort.by(Sort.Direction.DESC, "id")))
                .thenReturn(List.of(pedidoBase));

        List<PedidoResponse> lista = service.listar();

        assertEquals(1, lista.size());
        assertEquals(1L, lista.get(0).id());
        assertEquals("Taladro Percutor", lista.get(0).productos().get(0).nombreProducto());
    }

    @Test
    void obtener_CuandoExiste_DebeRetornarPedido() {
        when(repository.findById(1L)).thenReturn(Optional.of(pedidoBase));

        PedidoResponse respuesta = service.obtener(1L);

        assertEquals(1L, respuesta.id());
        assertEquals("juan@ejemplo.cl", respuesta.correoCliente());
    }

    @Test
    void obtener_CuandoNoExiste_DebeLanzar404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.obtener(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- 3. ANULAR PEDIDO ---

    @Test
    void anular_CuandoIdEsInvalido_DebeLanzar400() {
        ResponseStatusException exNull = assertThrows(
                ResponseStatusException.class, () -> service.anular(null));
        assertEquals(HttpStatus.BAD_REQUEST, exNull.getStatusCode());

        ResponseStatusException exCero = assertThrows(
                ResponseStatusException.class, () -> service.anular(0L));
        assertEquals(HttpStatus.BAD_REQUEST, exCero.getStatusCode());
    }

    @Test
    void anular_CuandoNoExiste_DebeLanzar404() {
        when(repository.buscarParaAnular(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.anular(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void anular_CuandoYaEstaAnulado_NoDebeDevolverStockNiGuardar() {
        pedidoBase.setEstado("ANULADO");
        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));

        service.anular(1L);

        verify(productoClient, never()).modificarStock(anyLong(), anyInt());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void anular_CuandoEstadoNoEsConfirmado_DebeLanzar409() {
        pedidoBase.setEstado("DESPACHADO");
        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.anular(1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void anular_CuandoConfirmado_DebeDevolverStockYMarcarAnulado() {
        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));

        service.anular(1L);

        assertEquals("ANULADO", pedidoBase.getEstado());
        verify(productoClient).modificarStock(1L, 2);
        verify(repository).saveAndFlush(pedidoBase);
    }

    @Test
    void anular_CuandoFallaSegundaDevolucion_DebeRevertirLaPrimera() {
        DetallePedido segundo = new DetallePedido();
        segundo.setProductoId(2L);
        segundo.setNombreProducto("Martillo");
        segundo.setCantidad(1);
        segundo.setPrecioUnitario(new BigDecimal("5000.00"));
        segundo.setSubtotal(new BigDecimal("5000.00"));
        pedidoBase.agregarDetalle(segundo);

        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));

        FeignException feign503 = mock(FeignException.class);
        when(feign503.status()).thenReturn(503);
        lenient().doThrow(feign503).when(productoClient).modificarStock(2L, 1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.anular(1L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        verify(productoClient).modificarStock(1L, 2);
        verify(productoClient).modificarStock(1L, -2); // Reversión de devolución parcial
    }

    // --- 4. ACTUALIZAR VENTA ---

    @Test
    void actualizar_CuandoIdEsInvalido_DebeLanzar400() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 1)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.actualizar(0L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void actualizar_CuandoVentaNoExiste_DebeLanzar404() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 1)));
        when(repository.buscarParaAnular(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.actualizar(99L, request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void actualizar_CuandoVentaNoEstaConfirmada_DebeLanzar409() {
        pedidoBase.setEstado("ANULADO");
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 1)));
        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.actualizar(1L, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void actualizar_ModificandoAgregandoYEliminandoProductos_DebeAjustarInventarioYTotal() {
        // Agregamos dos productos más a la venta original para probar disminuir y eliminar
        DetallePedido det2 = new DetallePedido();
        det2.setProductoId(2L);
        det2.setNombreProducto("Sierra");
        det2.setCantidad(3);
        det2.setPrecioUnitario(new BigDecimal("5000.00"));
        det2.setSubtotal(new BigDecimal("15000.00"));
        pedidoBase.agregarDetalle(det2);

        DetallePedido det3 = new DetallePedido();
        det3.setProductoId(3L);
        det3.setNombreProducto("Clavos");
        det3.setCantidad(1);
        det3.setPrecioUnitario(new BigDecimal("2000.00"));
        det3.setSubtotal(new BigDecimal("2000.00"));
        pedidoBase.agregarDetalle(det3);

        // Producto 1 sube (2 -> 4), Producto 2 baja (3 -> 1), Producto 3 se elimina, Producto 4 es nuevo (2)
        PedidoRequest request = new PedidoRequest(
                "Carlos", "Soto", "carlos@ejemplo.cl", "Nueva Dir 456", "Biobío", "Concepción",
                List.of(
                        new PedidoRequest.Item(1L, 4),
                        new PedidoRequest.Item(2L, 1),
                        new PedidoRequest.Item(4L, 2)
                )
        );

        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));
        when(productoClient.obtenerProductoPorId(1L))
                .thenReturn(crearProducto(1L, "Taladro Percutor", 12000.0, 10));
        when(productoClient.obtenerProductoPorId(4L))
                .thenReturn(crearProducto(4L, "Destornillador", 3000.0, 8));

        PedidoResponse actualizada = service.actualizar(1L, request);

        assertEquals("Carlos", actualizada.nombreCliente());
        assertEquals("carlos@ejemplo.cl", actualizada.correoCliente());
        // 4*10000 (mantiene precio original) + 1*5000 + 2*3000 = 51000.00
        assertEquals(new BigDecimal("51000.00"), actualizada.total());
        assertEquals(3, actualizada.productos().size());

        verify(productoClient).modificarStock(1L, -2);
        verify(productoClient).modificarStock(4L, -2);
        verify(productoClient).modificarStock(2L, 2);
        verify(productoClient).modificarStock(3L, 1);
        verify(repository).saveAndFlush(pedidoBase);
    }

    @Test
    void actualizar_CuandoStockEsInsuficienteAlAumentarCantidad_DebeLanzar409() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 10)));
        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));
        when(productoClient.obtenerProductoPorId(1L))
                .thenReturn(crearProducto(1L, "Taladro Percutor", 10000.0, 3));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.actualizar(1L, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void actualizar_CuandoProductoNuevoTienePrecioInvalido_DebeLanzar409() {
        PedidoRequest request = crearSolicitudValida(List.of(
                new PedidoRequest.Item(1L, 2),
                new PedidoRequest.Item(4L, 1)
        ));
        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));
        when(productoClient.obtenerProductoPorId(4L))
                .thenReturn(crearProducto(4L, "Producto Malo", -10.0, 5));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.actualizar(1L, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void actualizar_CuandoFallaGuardado_DebeRevertirMovimientosDeStock() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 1)));
        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));
        doThrow(new RuntimeException("Error BD")).when(repository).saveAndFlush(any(Pedido.class));

        assertThrows(RuntimeException.class, () -> service.actualizar(1L, request));

        verify(productoClient).modificarStock(1L, 1);  // Devolución aplicada (2 -> 1)
        verify(productoClient).modificarStock(1L, -1); // Reversión tras fallo en BD
    }

    @Test
    void actualizar_CuandoFallaReversionDeStock_DebeLanzar500() {
        PedidoRequest request = crearSolicitudValida(List.of(new PedidoRequest.Item(1L, 1)));
        when(repository.buscarParaAnular(1L)).thenReturn(Optional.of(pedidoBase));
        doThrow(new RuntimeException("Error BD")).when(repository).saveAndFlush(any(Pedido.class));

        FeignException feign503 = mock(FeignException.class);
        when(feign503.status()).thenReturn(503);
        lenient().doThrow(feign503).when(productoClient).modificarStock(1L, -1);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.actualizar(1L, request));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }
}