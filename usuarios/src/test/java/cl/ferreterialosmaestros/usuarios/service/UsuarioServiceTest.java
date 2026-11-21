package cl.ferreterialosmaestros.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import cl.ferreterialosmaestros.usuarios.model.Rol;
import cl.ferreterialosmaestros.usuarios.model.Usuario;
import cl.ferreterialosmaestros.usuarios.repository.RolRepository;
import cl.ferreterialosmaestros.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private UsuarioService service;

    private BCryptPasswordEncoder encoder;
    private Rol rolAdmin;
    private Rol rolVendedor;
    private Rol rolCliente;
    private Usuario usuarioBase;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();

        rolAdmin = new Rol(1L, "admin");
        rolVendedor = new Rol(2L, "vendedor");
        rolCliente = new Rol(3L, "cliente");

        usuarioBase = new Usuario();
        usuarioBase.setId(1L);
        usuarioBase.setRut("112223334");
        usuarioBase.setNombre("Admin");
        usuarioBase.setApellidos("Sistema");
        usuarioBase.setNombreCompleto("Admin Sistema");
        usuarioBase.setCorreo("admin@ejemplo.cl");
        usuarioBase.setContrasena(encoder.encode("Admin123"));
        usuarioBase.setRegion("Región Metropolitana de Santiago");
        usuarioBase.setComuna("Santiago");
        usuarioBase.setTelefono("+56911112222");
        usuarioBase.setRolEntidad(rolAdmin);
    }

    @Test
    void precargarUsuariosIniciales_CuandoBdEstaVacia_DebeGuardarAdminYVendedor() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(rolRepository.findByNombreIgnoreCase("admin")).thenReturn(Optional.of(rolAdmin));
        when(rolRepository.findByNombreIgnoreCase("vendedor")).thenReturn(Optional.of(rolVendedor));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.precargarUsuariosIniciales();

        verify(usuarioRepository, times(2)).save(any(Usuario.class));
    }

    @Test
    void precargarUsuariosIniciales_CuandoYaHayUsuarios_NoDebeGuardarNada() {
        when(usuarioRepository.count()).thenReturn(2L);

        service.precargarUsuariosIniciales();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void listar_DebeRetornarTodosLosUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuarioBase));

        List<Usuario> resultado = service.listar();

        assertEquals(1, resultado.size());
        assertEquals("admin@ejemplo.cl", resultado.get(0).getCorreo());
        verify(usuarioRepository).findAll();
    }

    @Test
    void guardar_ConNombreApellidosYContrasena_DebeNormalizarCifrarYGuardar() {
        Usuario nuevo = new Usuario();
        nuevo.setId(50L);
        nuevo.setRut(" 21.054.321-k ");
        nuevo.setNombre("Juan");
        nuevo.setApellidos("Pérez");
        nuevo.setCorreo("  JUAN@EJEMPLO.CL ");
        nuevo.setContrasena("Clave123");
        nuevo.setRol("cliente");

        when(usuarioRepository.existsByCorreoIgnoreCase("juan@ejemplo.cl")).thenReturn(false);
        when(usuarioRepository.existsByRutIgnoreCase("21054321K")).thenReturn(false);
        when(rolRepository.findByNombreIgnoreCase("cliente")).thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario guardado = service.guardar(nuevo, "cliente");

        assertNull(guardado.getId());
        assertEquals("21054321K", guardado.getRut());
        assertEquals("juan@ejemplo.cl", guardado.getCorreo());
        assertEquals("Juan Pérez", guardado.getNombreCompleto());
        assertEquals(rolCliente, guardado.getRolEntidad());
        assertNotEquals("Clave123", guardado.getContrasena());
        assertTrue(encoder.matches("Clave123", guardado.getContrasena()));
    }

    @Test
    void guardar_DesdeAdminSinContrasenaYConNombreCompleto_DebeUsarRolPorDefectoYRutComoClave() {
        Usuario nuevo = new Usuario();
        nuevo.setRut("123456789");
        nuevo.setNombreCompleto("Carlos Soto");
        nuevo.setCorreo("carlos@ejemplo.cl");

        when(usuarioRepository.existsByCorreoIgnoreCase("carlos@ejemplo.cl")).thenReturn(false);
        when(usuarioRepository.existsByRutIgnoreCase("123456789")).thenReturn(false);
        when(rolRepository.findByNombreIgnoreCase("vendedor")).thenReturn(Optional.of(rolVendedor));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario guardado = service.guardar(nuevo, "vendedor");

        assertEquals("Carlos Soto", guardado.getNombre());
        assertEquals(rolVendedor, guardado.getRolEntidad());
        assertTrue(encoder.matches("123456", guardado.getContrasena()));
    }

    @Test
    void guardar_CuandoCorreoYaExiste_DebeLanzarExcepcion() {
        Usuario nuevo = new Usuario();
        nuevo.setRut("123456789");
        nuevo.setCorreo("admin@ejemplo.cl");

        when(usuarioRepository.existsByCorreoIgnoreCase("admin@ejemplo.cl")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.guardar(nuevo, "cliente"));

        assertTrue(ex.getMessage().contains("correo"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void guardar_CuandoRutYaExiste_DebeLanzarExcepcion() {
        Usuario nuevo = new Usuario();
        nuevo.setRut("112223334");
        nuevo.setCorreo("nuevo@ejemplo.cl");

        when(usuarioRepository.existsByCorreoIgnoreCase("nuevo@ejemplo.cl")).thenReturn(false);
        when(usuarioRepository.existsByRutIgnoreCase("112223334")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.guardar(nuevo, "cliente"));

        assertTrue(ex.getMessage().contains("RUT"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void guardar_CuandoRolNoExiste_DebeLanzarExcepcion() {
        Usuario nuevo = new Usuario();
        nuevo.setRut("123456789");
        nuevo.setNombre("Ana");
        nuevo.setCorreo("ana@ejemplo.cl");
        nuevo.setRol("superadmin");

        when(usuarioRepository.existsByCorreoIgnoreCase("ana@ejemplo.cl")).thenReturn(false);
        when(usuarioRepository.existsByRutIgnoreCase("123456789")).thenReturn(false);
        when(rolRepository.findByNombreIgnoreCase("superadmin")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.guardar(nuevo, "vendedor"));

        assertTrue(ex.getMessage().contains("rol"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizar_CuandoExisteConDatosCompletos_DebeActualizarCamposRolYContrasena() {
        Usuario datosNuevos = new Usuario();
        datosNuevos.setCorreo("  NUEVO@EJEMPLO.CL ");
        datosNuevos.setTelefono("+56987654321");
        datosNuevos.setRegion("Biobío");
        datosNuevos.setComuna("Concepción");
        datosNuevos.setDireccion("Av. Siempre Viva 742");
        datosNuevos.setNombreCompleto("Admin Editado");
        datosNuevos.setRol("vendedor");
        datosNuevos.setContrasena("Nueva123");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioBase));
        when(rolRepository.findByNombreIgnoreCase("vendedor")).thenReturn(Optional.of(rolVendedor));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario actualizado = service.actualizar(1L, datosNuevos);

        assertNotNull(actualizado);
        assertEquals("nuevo@ejemplo.cl", actualizado.getCorreo());
        assertEquals("Admin Editado", actualizado.getNombreCompleto());
        assertEquals("Admin Editado", actualizado.getNombre());
        assertEquals(rolVendedor, actualizado.getRolEntidad());
        assertTrue(encoder.matches("Nueva123", actualizado.getContrasena()));
    }

    @Test
    void actualizar_CuandoContrasenaYaEsHashOSinNombreNiRol_NoDebeRecifrar() {
        String hashOriginal = usuarioBase.getContrasena();

        Usuario datosNuevos = new Usuario();
        datosNuevos.setCorreo("admin@ejemplo.cl");
        datosNuevos.setContrasena(hashOriginal);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioBase));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario actualizado = service.actualizar(1L, datosNuevos);

        assertNotNull(actualizado);
        assertEquals(hashOriginal, actualizado.getContrasena());
        assertEquals("Admin Sistema", actualizado.getNombreCompleto());
        verify(rolRepository, never()).findByNombreIgnoreCase(any());
    }

    @Test
    void actualizar_CuandoRolNoEsValido_DebeLanzarExcepcion() {
        Usuario datosNuevos = new Usuario();
        datosNuevos.setCorreo("admin@ejemplo.cl");
        datosNuevos.setRol("rolInexistente");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioBase));
        when(rolRepository.findByNombreIgnoreCase("rolInexistente")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.actualizar(1L, datosNuevos));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizar_CuandoUsuarioNoExiste_DebeRetornarNull() {
        Usuario datosNuevos = new Usuario();
        datosNuevos.setCorreo("noexiste@ejemplo.cl");

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Usuario resultado = service.actualizar(99L, datosNuevos);

        assertNull(resultado);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void eliminar_CuandoExiste_DebeEliminarYRetornarTrue() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        boolean eliminado = service.eliminar(1L);

        assertTrue(eliminado);
        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    void eliminar_CuandoNoExiste_DebeRetornarFalse() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        boolean eliminado = service.eliminar(99L);

        assertFalse(eliminado);
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    void login_ConCredencialesValidas_DebeRetornarUsuario() {
        when(usuarioRepository.findByCorreoIgnoreCase("admin@ejemplo.cl"))
                .thenReturn(Optional.of(usuarioBase));

        Usuario resultado = service.login(" admin@ejemplo.cl ", "Admin123");

        assertNotNull(resultado);
        assertEquals("admin@ejemplo.cl", resultado.getCorreo());
    }

    @Test
    void login_CuandoCorreoNoExiste_DebeRetornarNull() {
        when(usuarioRepository.findByCorreoIgnoreCase("desconocido@ejemplo.cl"))
                .thenReturn(Optional.empty());

        Usuario resultado = service.login("desconocido@ejemplo.cl", "Admin123");

        assertNull(resultado);
    }

    @Test
    void login_ConContrasenaIncorrecta_DebeRetornarNull() {
        when(usuarioRepository.findByCorreoIgnoreCase("admin@ejemplo.cl"))
                .thenReturn(Optional.of(usuarioBase));

        Usuario resultado = service.login("admin@ejemplo.cl", "ClaveMala");

        assertNull(resultado);
    }
}