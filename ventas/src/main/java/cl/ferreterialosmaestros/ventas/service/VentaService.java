package cl.ferreterialosmaestros.ventas.service;

import cl.ferreterialosmaestros.ventas.client.ProductoClient;
import cl.ferreterialosmaestros.ventas.dto.ProductoDTO;
import cl.ferreterialosmaestros.ventas.model.Venta;
import cl.ferreterialosmaestros.ventas.repository.VentaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VentaService {

    private final VentaRepository repository;
    private final ProductoClient productoClient;

    public VentaService(VentaRepository repository, ProductoClient productoClient) {
        this.repository = repository;
        this.productoClient = productoClient;
    }

    // 1. Listar todas las ventas (GET)
    public List<Venta> obtenerTodas() {
        return repository.findAll();
    }

    // 2. Buscar venta por ID (GET)
    public Optional<Venta> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    // 3. Crear una venta comunicándose con ms-productos vía OpenFeign (POST)
    public Venta guardarVenta(Venta venta) {
        if (venta.getProductoId() != null) {
            // Consulta el producto al microservicio ms-productos (puerto 8083)
            ProductoDTO producto = productoClient.obtenerProductoPorId(venta.getProductoId());

            if (producto != null) {
                // Autocompleta el nombre del producto
                if (venta.getNombreProducto() == null || venta.getNombreProducto().isEmpty()) {
                    venta.setNombreProducto(producto.getNombre());
                }
                // Si el total viene vacío o en 0, lo calcula con el precio oficial del MS de productos
                if (venta.getTotal() == null || venta.getTotal() == 0) {
                    venta.setTotal(producto.getPrecio() * venta.getCantidad());
                }
            }
        }
        return repository.save(venta);
    }

    // 4. Actualizar una venta existente (PUT)
    public Venta actualizarVenta(Long id, Venta datosNuevos) {
        return repository.findById(id).map(ventaExistente -> {
            ventaExistente.setNombreCliente(datosNuevos.getNombreCliente());
            ventaExistente.setApellidosCliente(datosNuevos.getApellidosCliente());
            ventaExistente.setCorreoCliente(datosNuevos.getCorreoCliente());
            ventaExistente.setDireccionEnvio(datosNuevos.getDireccionEnvio());
            ventaExistente.setRegion(datosNuevos.getRegion());
            ventaExistente.setComuna(datosNuevos.getComuna());
            ventaExistente.setProductoId(datosNuevos.getProductoId());
            ventaExistente.setNombreProducto(datosNuevos.getNombreProducto());
            ventaExistente.setCantidad(datosNuevos.getCantidad());
            ventaExistente.setTotal(datosNuevos.getTotal());
            return repository.save(ventaExistente);
        }).orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
    }
    // 5. Eliminar una venta (DELETE)
    public void eliminarVenta(Long id) {
        repository.deleteById(id);
    }
}