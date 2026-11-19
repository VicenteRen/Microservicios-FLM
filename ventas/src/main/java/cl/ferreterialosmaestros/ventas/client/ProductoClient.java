
package cl.ferreterialosmaestros.ventas.client;

import cl.ferreterialosmaestros.ventas.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "ms-productos", url = "http://localhost:8083/api/productos")
public interface ProductoClient {

    // Consultar producto por ID
    @GetMapping("/{id}")
    ProductoDTO obtenerProductoPorId(@PathVariable("id") Long id);

    // Actualizar producto
    @PutMapping("/{id}")
    ProductoDTO actualizarProducto(
            @PathVariable("id") Long id,
            @RequestBody ProductoDTO producto);

    // Descontar o restituir stock
    @PostMapping("/{id}/movimiento-stock")
    void modificarStock(
            @PathVariable("id") Long id,
            @RequestParam("variacion") int variacion);
}
