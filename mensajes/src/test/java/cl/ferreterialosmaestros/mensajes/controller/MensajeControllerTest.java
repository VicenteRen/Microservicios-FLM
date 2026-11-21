package cl.ferreterialosmaestros.mensajes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import cl.ferreterialosmaestros.mensajes.model.Mensaje;
import cl.ferreterialosmaestros.mensajes.service.MensajeService;

@ExtendWith(MockitoExtension.class)
class MensajeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MensajeService service;

    @InjectMocks
    private MensajeController controller;

    private Mensaje mensajeBase;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mensajeBase = new Mensaje();
        mensajeBase.setId(1L);
        mensajeBase.setNombreCompleto("Juan Perez");
        mensajeBase.setCorreo("juan@example.com");
        mensajeBase.setMensaje("Consulta por stock de taladro");
        mensajeBase.setEstado("PENDIENTE");
    }

    @Test
    void guardar_DebeRetornar201CreatedConMensajeGuardado() throws Exception {
        when(service.guardar(any(Mensaje.class))).thenReturn(mensajeBase);

        String jsonBody = """
                {
                    "nombreCompleto": "Juan Perez",
                    "correo": "juan@example.com",
                    "mensaje": "Consulta por stock de taladro"
                }
                """;

        mockMvc.perform(post("/api/contactos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombreCompleto").value("Juan Perez"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));

        verify(service).guardar(any(Mensaje.class));
    }

    @Test
    void listar_DebeRetornar200OkConListaDeMensajes() throws Exception {
        when(service.listar()).thenReturn(List.of(mensajeBase));

        mockMvc.perform(get("/api/contactos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].correo").value("juan@example.com"));

        verify(service).listar();
    }

    @Test
    void atender_CuandoExiste_DebeRetornar200OkConEstadoAtendido() throws Exception {
        mensajeBase.setEstado("ATENDIDO");
        when(service.atender(1L)).thenReturn(mensajeBase);

        mockMvc.perform(put("/api/contactos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ATENDIDO"));

        verify(service).atender(1L);
    }

    @Test
    void atender_CuandoNoExiste_DebeRetornar404NotFound() throws Exception {
        when(service.atender(99L)).thenReturn(null);

        mockMvc.perform(put("/api/contactos/99"))
                .andExpect(status().isNotFound());

        verify(service).atender(99L);
    }

    @Test
    void eliminar_CuandoExiste_DebeRetornar204NoContent() throws Exception {
        when(service.eliminar(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/contactos/1"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(1L);
    }

    @Test
    void eliminar_CuandoNoExiste_DebeRetornar404NotFound() throws Exception {
        when(service.eliminar(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/contactos/99"))
                .andExpect(status().isNotFound());

        verify(service).eliminar(99L);
    }
}