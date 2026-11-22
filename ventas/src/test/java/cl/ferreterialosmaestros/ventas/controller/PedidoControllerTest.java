package cl.ferreterialosmaestros.ventas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import cl.ferreterialosmaestros.ventas.dto.PedidoRequest;
import cl.ferreterialosmaestros.ventas.dto.PedidoResponse;
import cl.ferreterialosmaestros.ventas.service.PedidoService;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PedidoService service;

    @InjectMocks
    private PedidoController controller;

    private PedidoResponse respuestaBase;

    private static final String JSON_PEDIDO = """
            {
                "nombreCliente": "Juan",
                "apellidosCliente": "Pérez",
                "correoCliente": "juan@ejemplo.cl",
                "direccionEnvio": "Av. Los Maestros 123",
                "region": "Metropolitana",
                "comuna": "Santiago",
                "productos": [
                    { "productoId": 1, "cantidad": 2 }
                ]
            }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        respuestaBase = new PedidoResponse(
                1L,
                "Juan",
                "Pérez",
                "juan@ejemplo.cl",
                "Av. Los Maestros 123",
                "Metropolitana",
                "Santiago",
                new BigDecimal("25500.50"),
                LocalDateTime.of(2026, 11, 22, 18, 0),
                "CONFIRMADO",
                List.of(new PedidoResponse.Item(
                        1L,
                        "Taladro Percutor",
                        2,
                        new BigDecimal("12750.25"),
                        new BigDecimal("25500.50")
                ))
        );
    }

    @Test
    void crearPedido_DatosValidos_DebeRetornar201Created() throws Exception {
        when(service.crear(any(PedidoRequest.class))).thenReturn(respuestaBase);

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PEDIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.total").value(25500.5))
                .andExpect(jsonPath("$.productos[0].nombreProducto").value("Taladro Percutor"));

        verify(service).crear(any(PedidoRequest.class));
    }

    @Test
    void crearPedido_CuandoStockInsuficiente_DebeRetornar409Conflict() throws Exception {
        when(service.crear(any(PedidoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente"));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PEDIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void listarPedidos_DebeRetornar200OkConListado() throws Exception {
        when(service.listar()).thenReturn(List.of(respuestaBase));

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].correoCliente").value("juan@ejemplo.cl"));

        verify(service).listar();
    }

    @Test
    void obtenerPedido_CuandoExiste_DebeRetornar200Ok() throws Exception {
        when(service.obtener(1L)).thenReturn(respuestaBase);

        mockMvc.perform(get("/api/ventas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombreCliente").value("Juan"));

        verify(service).obtener(1L);
    }

    @Test
    void obtenerPedido_CuandoNoExiste_DebeRetornar404NotFound() throws Exception {
        when(service.obtener(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        mockMvc.perform(get("/api/ventas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarVenta_DatosValidos_DebeRetornar200Ok() throws Exception {
        when(service.actualizar(eq(1L), any(PedidoRequest.class))).thenReturn(respuestaBase);

        mockMvc.perform(put("/api/ventas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PEDIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));

        verify(service).actualizar(eq(1L), any(PedidoRequest.class));
    }

    @Test
    void actualizarVenta_CuandoEstaAnulada_DebeRetornar409Conflict() throws Exception {
        when(service.actualizar(eq(1L), any(PedidoRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Solo pueden editarse ventas confirmadas"));

        mockMvc.perform(put("/api/ventas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PEDIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void anularPedido_CuandoExiste_DebeRetornar204NoContent() throws Exception {
        doNothing().when(service).anular(1L);

        mockMvc.perform(delete("/api/ventas/1"))
                .andExpect(status().isNoContent());

        verify(service).anular(1L);
    }

    @Test
    void anularPedido_CuandoNoExiste_DebeRetornar404NotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"))
                .when(service).anular(99L);

        mockMvc.perform(delete("/api/ventas/99"))
                .andExpect(status().isNotFound());
    }
}