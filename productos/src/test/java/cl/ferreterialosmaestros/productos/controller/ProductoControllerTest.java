package cl.ferreterialosmaestros.productos.controller;

import cl.ferreterialosmaestros.productos.model.Producto;
import cl.ferreterialosmaestros.productos.service.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductoController.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductoService productoService;

    private Producto crearProductoEjemplo(Long id) {
        Producto p = new Producto();
        p.setId(id);
        p.setCodigo("HE001");
        p.setNombre("Taladro Percutor 850W");
        p.setCategoria("Herramientas");
        p.setSubcategoria("Eléctricas");
        p.setMarca("BOSCH");
        p.setUnidad("Unidad");
        p.setPrecioCompra(75000.0);
        p.setPrecio(124900.0);
        p.setStock(10);
        p.setStockMinimo(2);
        p.setImagen("img/prod_2_taladro-percutor.jpg");
        p.setDescripcion("Taladro percutor ideal para concreto y mampostería.");
        return p;
    }

    @Test
    void obtenerListaProductos_retornaOkYListado() throws Exception {
        Producto p1 = crearProductoEjemplo(1L);

        when(productoService.listarProductos()).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].codigo").value("HE001"))
                .andExpect(jsonPath("$[0].nombre").value("Taladro Percutor 850W"));
    }

    @Test
    void obtenerProductoPorId_conIdExistente_retornaOk() throws Exception {
        Producto p1 = crearProductoEjemplo(1L);

        when(productoService.obtenerProductoPorId(1L)).thenReturn(p1);

        mockMvc.perform(get("/api/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.marca").value("BOSCH"));
    }

    @Test
    void obtenerProductoPorId_conIdInexistente_retornaNotFound() throws Exception {
        when(productoService.obtenerProductoPorId(99L)).thenReturn(null);

        mockMvc.perform(get("/api/productos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearProducto_conDatosValidos_retornaCreated() throws Exception {
        Producto nuevo = crearProductoEjemplo(null);
        Producto guardado = crearProductoEjemplo(10L);

        when(productoService.guardarProducto(any(Producto.class))).thenReturn(guardado);

        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.codigo").value("HE001"));
    }

    @Test
    void actualizarProducto_conIdExistente_retornaOk() throws Exception {
        Producto existente = crearProductoEjemplo(1L);
        Producto actualizado = crearProductoEjemplo(1L);
        actualizado.setNombre("Taladro Percutor 1500W");
        actualizado.setStock(20);

        when(productoService.obtenerProductoPorId(1L)).thenReturn(existente);
        when(productoService.guardarProducto(any(Producto.class))).thenReturn(actualizado);

        mockMvc.perform(put("/api/productos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(actualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Taladro Percutor 1500W"))
                .andExpect(jsonPath("$.stock").value(20));
    }

    @Test
    void actualizarProducto_conIdInexistente_retornaNotFound() throws Exception {
        Producto datos = crearProductoEjemplo(99L);

        when(productoService.obtenerProductoPorId(99L)).thenReturn(null);

        mockMvc.perform(put("/api/productos/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(datos)))
                .andExpect(status().isNotFound());

        verify(productoService, never()).guardarProducto(any());
    }

    @Test
    void eliminarProductoPorId_conIdExistente_retornaNoContent() throws Exception {
        Producto existente = crearProductoEjemplo(1L);

        when(productoService.obtenerProductoPorId(1L)).thenReturn(existente);
        doNothing().when(productoService).eliminarProductoPorId(1L);

        mockMvc.perform(delete("/api/productos/1"))
                .andExpect(status().isNoContent());

        verify(productoService, times(1)).eliminarProductoPorId(1L);
    }

    @Test
    void eliminarProductoPorId_conIdInexistente_retornaNotFound() throws Exception {
        when(productoService.obtenerProductoPorId(99L)).thenReturn(null);

        mockMvc.perform(delete("/api/productos/99"))
                .andExpect(status().isNotFound());

        verify(productoService, never()).eliminarProductoPorId(anyLong());
    }

    @Test
    void crearProducto_conDatosInvalidos_retornaBadRequest() throws Exception {
        Producto invalido = new Producto(); // Faltan campos obligatorios (@NotEmpty y @NotNull)

        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());

        verify(productoService, never()).guardarProducto(any());
    }

    @Test
    void actualizarProducto_conDatosInvalidos_retornaBadRequest() throws Exception {
        Producto invalido = new Producto();

        mockMvc.perform(put("/api/productos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());

        verify(productoService, never()).guardarProducto(any());
    }

    @Test
    void modificarStock_conVariacionValida_retornaNoContent() throws Exception {
        doNothing().when(productoService).modificarStock(1L, -2);

        mockMvc.perform(post("/api/productos/1/movimiento-stock")
                .param("variacion", "-2"))
                .andExpect(status().isNoContent());

        verify(productoService, times(1)).modificarStock(1L, -2);
    }

    @Test
    void modificarStock_conVariacionCero_retornaBadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "La variacion no puede ser cero"))
                .when(productoService).modificarStock(1L, 0);

        mockMvc.perform(post("/api/productos/1/movimiento-stock")
                .param("variacion", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificarStock_conProductoInexistente_retornaNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"))
                .when(productoService).modificarStock(99L, -2);

        mockMvc.perform(post("/api/productos/99/movimiento-stock")
                .param("variacion", "-2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void modificarStock_conStockInsuficiente_retornaConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente"))
                .when(productoService).modificarStock(1L, -50);

        mockMvc.perform(post("/api/productos/1/movimiento-stock")
                .param("variacion", "-50"))
                .andExpect(status().isConflict());
    }
}