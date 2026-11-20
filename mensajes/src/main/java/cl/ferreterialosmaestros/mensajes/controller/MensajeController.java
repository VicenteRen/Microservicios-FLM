package cl.ferreterialosmaestros.mensajes.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.ferreterialosmaestros.mensajes.model.Mensaje;
import cl.ferreterialosmaestros.mensajes.service.MensajeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/contactos")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Contactos / Mensajes", description = "Operaciones CRUD para los mensajes de contacto de clientes")
public class MensajeController {

    private final MensajeService service;

    public MensajeController(MensajeService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar un nuevo mensaje", description = "Recibe los datos del formulario de contacto y los guarda con estado PENDIENTE.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Mensaje registrado exitosamente en la base de datos"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<Mensaje> guardar(@RequestBody Mensaje contacto) {
        Mensaje guardado = service.guardar(contacto);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @GetMapping
    @Operation(summary = "Listar todos los mensajes", description = "Devuelve la lista completa de mensajes recibidos para el panel de administración.")
    @ApiResponse(responseCode = "200", description = "Listado de mensajes obtenido correctamente")
    public List<Mensaje> listar() {
        return service.listar();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Marcar mensaje como ATENDIDO", description = "Busca un mensaje por su ID y actualiza su estado a ATENDIDO.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado del mensaje actualizado a ATENDIDO"),
        @ApiResponse(responseCode = "404", description = "No se encontró un mensaje con el ID especificado")
    })
    public ResponseEntity<Mensaje> atender(
            @Parameter(description = "ID del mensaje que se desea marcar como atendido", example = "1")
            @PathVariable Long id) {
        Mensaje actualizado = service.atender(id);
        if (actualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un mensaje", description = "Elimina permanentemente un mensaje de la base de datos según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Mensaje eliminado correctamente"),
        @ApiResponse(responseCode = "404", description = "No se encontró un mensaje con el ID especificado")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del mensaje que se desea eliminar", example = "1")
            @PathVariable Long id) {
        boolean eliminado = service.eliminar(id);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}