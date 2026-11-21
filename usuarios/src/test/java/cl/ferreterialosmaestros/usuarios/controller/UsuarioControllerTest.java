package cl.ferreterialosmaestros.usuarios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

import cl.ferreterialosmaestros.usuarios.model.Rol;
import cl.ferreterialosmaestros.usuarios.model.Usuario;
import cl.ferreterialosmaestros.usuarios.service.UsuarioService;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuarioService service;

    @InjectMocks
    private UsuarioController controller;

    private Usuario usuarioBase;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        usuarioBase = new Usuario();
        usuarioBase.setId(1L);
        usuarioBase.setRut("112223334");
        usuarioBase.setNombre("Admin");
        usuarioBase.setApellidos("Sistema");
        usuarioBase.setNombreCompleto("Admin Sistema");
        usuarioBase.setCorreo("admin@ejemplo.cl");
        usuarioBase.setRolEntidad(new Rol(1L, "admin"));
    }

    // --- 1. LOGIN ---

    @Test
    void login_CredencialesCorrectas_DebeRetornar200Ok() throws Exception {
        when(service.login("admin@ejemplo.cl", "Admin123")).thenReturn(usuarioBase);

        String json = """
                {
                    "correo": "admin@ejemplo.cl",
                    "contrasena": "Admin123"
                }
                """;

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("admin@ejemplo.cl"))
                .andExpect(jsonPath("$.rol").value("admin"));
    }

    @Test
    void login_CredencialesIncorrectas_DebeRetornar401Unauthorized() throws Exception {
        when(service.login("admin@ejemplo.cl", "Mala")).thenReturn(null);

        String json = """
                {
                    "correo": "admin@ejemplo.cl",
                    "contrasena": "Mala"
                }
                """;

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
    }

    // --- 2. REGISTRO PÚBLICO ---

    @Test
    void registrarCliente_DatosValidos_DebeRetornar201Created() throws Exception {
        Usuario cliente = new Usuario();
        cliente.setId(3L);
        cliente.setRut("21054321K");
        cliente.setNombreCompleto("Juan Pérez");
        cliente.setCorreo("juan@ejemplo.cl");
        cliente.setRolEntidad(new Rol(3L, "cliente"));

        when(service.guardar(any(Usuario.class), eq("cliente"))).thenReturn(cliente);

        String json = """
                {
                    "rut": "21054321K",
                    "nombre": "Juan",
                    "apellidos": "Pérez",
                    "correo": "juan@ejemplo.cl",
                    "contrasena": "12345"
                }
                """;

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.rol").value("cliente"));
    }

    @Test
    void registrarCliente_CuandoCorreoDuplicado_DebeRetornar400BadRequest() throws Exception {
        when(service.guardar(any(Usuario.class), eq("cliente")))
                .thenThrow(new IllegalArgumentException("Este correo electrónico ya está registrado."));

        String json = """
                {
                    "rut": "21054321K",
                    "correo": "admin@ejemplo.cl",
                    "contrasena": "12345"
                }
                """;

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Este correo electrónico ya está registrado."));
    }

    // --- 3. LISTAR ---

    @Test
    void listar_ConHeaderAdmin_DebeRetornar200Ok() throws Exception {
        when(service.listar()).thenReturn(List.of(usuarioBase));

        mockMvc.perform(get("/api/usuarios")
                        .header("X-Usuario-Rol", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("admin@ejemplo.cl"));

        verify(service).listar();
    }

    @Test
    void listar_ConHeaderCliente_DebeRetornar403Forbidden() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("X-Usuario-Rol", "cliente"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").exists());

        verify(service, never()).listar();
    }

    @Test
    void listar_SinHeader_DebeRetornar403Forbidden() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());

        verify(service, never()).listar();
    }

    // --- 4. CREAR DESDE ADMIN ---

    @Test
    void crearDesdeAdmin_ConHeaderAdmin_DebeRetornar201Created() throws Exception {
        when(service.guardar(any(Usuario.class), eq("vendedor"))).thenReturn(usuarioBase);

        String json = """
                {
                    "rut": "998887776",
                    "nombreCompleto": "Vendedor Nuevo",
                    "correo": "vendedor2@ejemplo.cl",
                    "rol": "vendedor"
                }
                """;

        mockMvc.perform(post("/api/usuarios")
                        .header("X-Usuario-Rol", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void crearDesdeAdmin_SinHeaderAdmin_DebeRetornar403Forbidden() throws Exception {
        String json = """
                {
                    "rut": "998887776",
                    "correo": "vendedor2@ejemplo.cl"
                }
                """;

        mockMvc.perform(post("/api/usuarios")
                        .header("X-Usuario-Rol", "vendedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());

        verify(service, never()).guardar(any(), any());
    }

    @Test
    void crearDesdeAdmin_ConDatosInvalidos_DebeRetornar400BadRequest() throws Exception {
        when(service.guardar(any(Usuario.class), eq("vendedor")))
                .thenThrow(new IllegalArgumentException("Este RUT/RUN ya está registrado."));

        String json = """
                {
                    "rut": "112223334",
                    "correo": "otro@ejemplo.cl"
                }
                """;

        mockMvc.perform(post("/api/usuarios")
                        .header("X-Usuario-Rol", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Este RUT/RUN ya está registrado."));
    }

    // --- 5. ACTUALIZAR ---

    @Test
    void actualizar_ConHeaderAdminYUsuarioExiste_DebeRetornar200Ok() throws Exception {
        when(service.actualizar(eq(1L), any(Usuario.class))).thenReturn(usuarioBase);

        String json = """
                {
                    "nombreCompleto": "Admin Editado",
                    "correo": "admin@ejemplo.cl",
                    "rol": "admin"
                }
                """;

        mockMvc.perform(put("/api/usuarios/1")
                        .header("X-Usuario-Rol", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void actualizar_ConHeaderAdminYUsuarioNoExiste_DebeRetornar404NotFound() throws Exception {
        when(service.actualizar(eq(99L), any(Usuario.class))).thenReturn(null);

        String json = """
                {
                    "correo": "noexiste@ejemplo.cl"
                }
                """;

        mockMvc.perform(put("/api/usuarios/99")
                        .header("X-Usuario-Rol", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_SinHeaderAdmin_DebeRetornar403Forbidden() throws Exception {
        String json = """
                {
                    "correo": "admin@ejemplo.cl"
                }
                """;

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());

        verify(service, never()).actualizar(any(), any());
    }

    // --- 6. ELIMINAR ---

    @Test
    void eliminar_ConHeaderAdminYUsuarioExiste_DebeRetornar204NoContent() throws Exception {
        when(service.eliminar(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/usuarios/1")
                        .header("X-Usuario-Rol", "admin"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(1L);
    }

    @Test
    void eliminar_ConHeaderAdminYUsuarioNoExiste_DebeRetornar404NotFound() throws Exception {
        when(service.eliminar(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/usuarios/99")
                        .header("X-Usuario-Rol", "admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_SinHeaderAdmin_DebeRetornar403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/usuarios/1")
                        .header("X-Usuario-Rol", "cliente"))
                .andExpect(status().isForbidden());

        verify(service, never()).eliminar(any());
    }
}