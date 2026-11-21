package cl.ferreterialosmaestros.productos.controller;

import cl.ferreterialosmaestros.productos.model.Producto;
import cl.ferreterialosmaestros.productos.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Productos", description = "Operaciones CRUD para la gestión del catálogo e inventario de productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todos los productos", description = "Devuelve el catálogo completo de productos registrados en la base de datos.")
    @ApiResponse(responseCode = "200", description = "Listado de productos obtenido correctamente")
    public ResponseEntity<List<Producto>> obtenerListaProductos() {
        return new ResponseEntity<>(service.listarProductos(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID", description = "Busca y devuelve el detalle de un producto específico según su identificador.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Producto encontrado exitosamente"),
        @ApiResponse(responseCode = "404", description = "No se encontró un producto con el ID especificado")
    })
    public ResponseEntity<Producto> obtenerProductoPorId(
            @Parameter(description = "ID del producto a consultar", example = "1")
            @PathVariable Long id) {
        Producto producto = service.obtenerProductoPorId(id);
        if (producto != null) {
            return new ResponseEntity<>(producto, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo producto", description = "Registra un producto en el inventario. Si el precio de compra viene vacío o en 0, se iguala al precio de venta.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Producto creado exitosamente en la base de datos"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o campos obligatorios faltantes")
    })
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody Producto producto) {
        Producto guardado = service.guardarProducto(producto);
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un producto existente", description = "Modifica los datos o el stock de un producto buscándolo por su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Producto actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "No se encontró un producto con el ID especificado")
    })
    public ResponseEntity<Producto> actualizarProducto(
            @Parameter(description = "ID del producto que se desea actualizar", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody Producto producto) {
        Producto existente = service.obtenerProductoPorId(id);
        if (existente != null) {
            producto.setId(id);
            Producto actualizado = service.guardarProducto(producto);
            return new ResponseEntity<>(actualizado, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un producto", description = "Elimina un producto del inventario según su ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Producto eliminado correctamente"),
        @ApiResponse(responseCode = "404", description = "No se encontró un producto con el ID especificado")
    })
    public ResponseEntity<Void> eliminarProductoPorId(
            @Parameter(description = "ID del producto que se desea eliminar", example = "1")
            @PathVariable Long id) {
        Producto existente = service.obtenerProductoPorId(id);
        if (existente != null) {
            service.eliminarProductoPorId(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}