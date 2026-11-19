
package cl.ferreterialosmaestros.ventas.controller;

import cl.ferreterialosmaestros.ventas.model.Venta;
import cl.ferreterialosmaestros.ventas.service.VentaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Gestión de Ventas",
     description = "Operaciones para registrar, consultar, actualizar y anular ventas")
public class VentaController {

    private final VentaService service;

    public VentaController(VentaService service) {
        this.service = service;
    }

    // 1. Listar todas las ventas (GET)
    @Operation(
        summary = "Listar ventas",
        description = "Obtiene todas las ventas registradas, incluyendo las anuladas."
    )
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<Venta>> listarVentas() {
        return ResponseEntity.ok(service.obtenerTodas());
    }

    // 2. Buscar venta por ID (GET)
    @Operation(
        summary = "Buscar venta por ID",
        description = "Consulta los detalles de una venta mediante su identificador."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venta encontrada"),
        @ApiResponse(responseCode = "404", description = "Venta no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Venta> obtenerVenta(@PathVariable Long id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Registrar una venta (POST)
    @Operation(
        summary = "Registrar venta",
        description = "Registra una venta, valida los datos del cliente, consulta el precio oficial del producto mediante OpenFeign y descuenta las unidades compradas del inventario."
    )

    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Venta registrada correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de venta inválidos"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
        @ApiResponse(responseCode = "409", description = "Stock insuficiente o datos del producto inválidos"),
        @ApiResponse(responseCode = "503", description = "Microservicio de productos no disponible")
    })
    @PostMapping
    public ResponseEntity<Venta> crearVenta(@RequestBody Venta venta) {
        Venta guardada = service.guardarVenta(venta);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    // 4. Actualizar una venta (PUT)
    @Operation(
        summary = "Actualizar venta",
        description = "Actualiza los datos de una venta activa. No permite modificar ventas anuladas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venta actualizada correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Venta o producto no encontrado"),
        @ApiResponse(responseCode = "409", description = "Venta anulada, stock insuficiente o datos del producto inválidos"),
        @ApiResponse(responseCode = "503", description = "Microservicio de productos no disponible")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Venta> actualizarVenta(
            @PathVariable Long id, @RequestBody Venta venta) {
        return ResponseEntity.ok(service.actualizarVenta(id, venta));
    }

    // 5. Anular una venta (DELETE lógico)
    @Operation(
        summary = "Anular venta",
        description = "Cambia el estado de una venta a ANULADA sin eliminarla de MySQL y restituye las unidades al inventario. Una venta anulada no puede modificarse ni devolver stock nuevamente."
    )

    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Venta anulada correctamente"),
        @ApiResponse(responseCode = "404", description = "Venta no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anularVenta(@PathVariable Long id) {
        service.anularVenta(id);
        return ResponseEntity.noContent().build();
    }
}
