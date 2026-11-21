package cl.ferreterialosmaestros.mensajes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

import cl.ferreterialosmaestros.mensajes.model.Mensaje;
import cl.ferreterialosmaestros.mensajes.repository.MensajeRepository;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    @Mock
    private MensajeRepository repository;

    @InjectMocks
    private MensajeService service;

    private Mensaje mensajeBase;

    @BeforeEach
    void setUp() {
        mensajeBase = new Mensaje();
        mensajeBase.setId(1L);
        mensajeBase.setNombreCompleto("Juan Perez");
        mensajeBase.setCorreo("juan@example.com");
        mensajeBase.setMensaje("Consulta por stock de taladro");
        mensajeBase.setEstado("PENDIENTE");
    }

    @Test
    void guardar_DebeLimpiarIdYAsignarEstadoPendiente() {
        Mensaje entrada = new Mensaje();
        entrada.setId(99L);
        entrada.setNombreCompleto("Juan Perez");
        entrada.setCorreo("juan@example.com");
        entrada.setMensaje("Consulta por stock de taladro");
        entrada.setEstado("ATENDIDO");

        when(repository.save(any(Mensaje.class))).thenAnswer(inv -> {
            Mensaje m = inv.getArgument(0);
            assertNull(m.getId());
            assertEquals("PENDIENTE", m.getEstado());
            m.setId(1L);
            return m;
        });

        Mensaje resultado = service.guardar(entrada);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("PENDIENTE", resultado.getEstado());
        verify(repository).save(entrada);
    }

    @Test
    void listar_DebeRetornarTodosLosMensajes() {
        when(repository.findAll()).thenReturn(List.of(mensajeBase));

        List<Mensaje> resultado = service.listar();

        assertEquals(1, resultado.size());
        assertEquals("Juan Perez", resultado.get(0).getNombreCompleto());
        verify(repository).findAll();
    }

    @Test
    void atender_CuandoExiste_DebeCambiarEstadoAAtendido() {
        when(repository.findById(1L)).thenReturn(Optional.of(mensajeBase));
        when(repository.save(any(Mensaje.class))).thenAnswer(inv -> inv.getArgument(0));

        Mensaje resultado = service.atender(1L);

        assertNotNull(resultado);
        assertEquals("ATENDIDO", resultado.getEstado());
        verify(repository).findById(1L);
        verify(repository).save(mensajeBase);
    }

    @Test
    void atender_CuandoNoExiste_DebeRetornarNull() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Mensaje resultado = service.atender(99L);

        assertNull(resultado);
        verify(repository).findById(99L);
        verify(repository, never()).save(any(Mensaje.class));
    }

    @Test
    void eliminar_CuandoExiste_DebeEliminarYRetornarTrue() {
        when(repository.existsById(1L)).thenReturn(true);

        boolean resultado = service.eliminar(1L);

        assertTrue(resultado);
        verify(repository).existsById(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void eliminar_CuandoNoExiste_DebeRetornarFalseSinEliminar() {
        when(repository.existsById(99L)).thenReturn(false);

        boolean resultado = service.eliminar(99L);

        assertFalse(resultado);
        verify(repository).existsById(99L);
        verify(repository, never()).deleteById(any());
    }
}