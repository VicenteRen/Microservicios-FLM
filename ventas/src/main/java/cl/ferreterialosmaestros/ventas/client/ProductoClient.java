package cl.ferreterialosmaestros.ventas.client;

import cl.ferreterialosmaestros.ventas.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-productos", url = "http://localhost:8083/api/productos")
public interface ProductoClient {

    @GetMapping("/{id}")
    ProductoDTO obtenerProductoPorId(@PathVariable("id") Long id);

    @PutMapping("/{id}")
    ProductoDTO actualizarProducto(@PathVariable("id") Long id, @RequestBody ProductoDTO producto);
}