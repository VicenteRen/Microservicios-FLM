
package cl.ferreterialosmaestros.ventas.controller;

import cl.ferreterialosmaestros.ventas.dto.PedidoRequest;
import cl.ferreterialosmaestros.ventas.dto.PedidoResponse;
import cl.ferreterialosmaestros.ventas.service.PedidoService;

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
@Tag(
    name = "Gestión de Ventas",
    description = "Registro, consulta y anulación de ventas con uno o varios productos"
)
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    // 1. Registrar una compra completa
    @Operation(
        summary = "Registrar pedido",
        description = "Registra una compra con varios productos, calcula el total con los precios oficiales y descuenta el inventario."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pedido registrado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos del pedido inválidos"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
        @ApiResponse(responseCode = "409", description = "Stock insuficiente"),
        @ApiResponse(responseCode = "503", description = "Microservicio de productos no disponible")
    })
    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(
            @RequestBody PedidoRequest pedido) {

        PedidoResponse guardado = service.crear(pedido);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(guardado);
    }

    // 2. Listar todos los pedidos
    @Operation(
        summary = "Listar pedidos",
        description = "Obtiene todos los pedidos registrados, incluyendo los anulados."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Listado obtenido correctamente"
    )
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listarPedidos() {

        return ResponseEntity.ok(service.listar());
    }

    // 3. Consultar pedido por ID
    @Operation(
        summary = "Buscar pedido por ID",
        description = "Consulta los datos de un pedido y todos sus productos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtenerPedido(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.obtener(id));
    }


    // Actualizar una venta confirmada
    @Operation(
        summary = "Actualizar venta",
        description = "Permite corregir datos del cliente y modificar productos o cantidades de una venta confirmada. Ajusta automaticamente el inventario y recalcula el total."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venta actualizada"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos"),
        @ApiResponse(responseCode = "404", description = "Venta no encontrada"),
        @ApiResponse(responseCode = "409", description = "Stock insuficiente o venta anulada"),
        @ApiResponse(responseCode = "503", description = "Productos no disponible")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponse> actualizarVenta(
            @PathVariable Long id,
            @RequestBody PedidoRequest datos) {

        return ResponseEntity.ok(service.actualizar(id, datos));
    }


    // 4. Anular un pedido completo
    @Operation(
        summary = "Anular pedido",
        description = "Cambia el estado del pedido a ANULADO e intenta restituir al inventario todos los productos comprados. Evita devolver stock dos veces."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pedido anulado correctamente"),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
        @ApiResponse(responseCode = "409", description = "El pedido no puede anularse"),
        @ApiResponse(responseCode = "503", description = "Microservicio de productos no disponible")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anularPedido(
            @PathVariable Long id) {

        service.anular(id);

        return ResponseEntity.noContent().build();
    }
}
