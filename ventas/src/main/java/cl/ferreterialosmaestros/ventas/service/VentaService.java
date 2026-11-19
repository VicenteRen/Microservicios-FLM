
package cl.ferreterialosmaestros.ventas.service;

import cl.ferreterialosmaestros.ventas.client.ProductoClient;
import cl.ferreterialosmaestros.ventas.dto.ProductoDTO;
import cl.ferreterialosmaestros.ventas.model.Venta;
import cl.ferreterialosmaestros.ventas.repository.VentaRepository;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class VentaService {

    private final VentaRepository repository;
    private final ProductoClient productoClient;

    public VentaService(VentaRepository repository, ProductoClient productoClient) {
        this.repository = repository;
        this.productoClient = productoClient;
    }

    // 1. Listar ventas
    public List<Venta> obtenerTodas() {
        return repository.findAll();
    }

    // 2. Buscar venta por ID
    public Optional<Venta> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    // 3. Registrar venta y descontar stock
    @Transactional
    public Venta guardarVenta(Venta venta) {
        validarVenta(venta);
        completarDatosProducto(venta);

        venta.setId(null);
        venta.setFechaVenta(null);
        venta.setEstado("ACTIVA");

        // Descontar stock del producto
        modificarStock(venta.getProductoId(), -venta.getCantidad());

        try {
            return repository.saveAndFlush(venta);
        } catch (RuntimeException e) {
            // Compensar el descuento si falla el guardado
            compensarStock(venta.getProductoId(), venta.getCantidad());
            throw e;
        }
    }

    // 4. Actualizar datos del cliente de una venta
    @Transactional
    public Venta actualizarVenta(Long id, Venta datosNuevos) {
        Venta existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venta no encontrada"));

        if ("ANULADA".equals(existente.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede modificar una venta anulada");
        }

        validarVenta(datosNuevos);

        // Evitar cambios que alteren el inventario de una venta registrada
        if (!Objects.equals(existente.getProductoId(), datosNuevos.getProductoId())
                || !Objects.equals(existente.getCantidad(), datosNuevos.getCantidad())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede cambiar el producto ni la cantidad de una venta registrada");
        }

        existente.setNombreCliente(datosNuevos.getNombreCliente());
        existente.setApellidosCliente(datosNuevos.getApellidosCliente());
        existente.setCorreoCliente(datosNuevos.getCorreoCliente());
        existente.setDireccionEnvio(datosNuevos.getDireccionEnvio());
        existente.setRegion(datosNuevos.getRegion());
        existente.setComuna(datosNuevos.getComuna());

        // Conservamos el precio original de la compra
        return repository.save(existente);
    }

    // 5. Anular venta y restituir stock
    @Transactional
    public void anularVenta(Long id) {
        Venta venta = repository.buscarParaAnular(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Venta no encontrada"));

        if ("ANULADA".equals(venta.getEstado())) {
            return;
        }

        // Devolver existencias al inventario
        modificarStock(venta.getProductoId(), venta.getCantidad());

        try {
            venta.setEstado("ANULADA");
            repository.saveAndFlush(venta);
        } catch (RuntimeException e) {
            // Compensar si falla el guardado de la anulación
            compensarStock(venta.getProductoId(), -venta.getCantidad());
            throw e;
        }
    }

    // 6. Validar datos del cliente y producto
    private void validarVenta(Venta venta) {
        if (venta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Debe ingresar una venta");
        }

        if (estaVacio(venta.getNombreCliente())
                || estaVacio(venta.getApellidosCliente())
                || estaVacio(venta.getCorreoCliente())
                || estaVacio(venta.getDireccionEnvio())
                || estaVacio(venta.getRegion())
                || estaVacio(venta.getComuna())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Complete los datos del cliente");
        }

        if (!venta.getCorreoCliente().matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Correo electrónico inválido");
        }

        if (venta.getProductoId() == null || venta.getProductoId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Producto inválido");
        }

        if (venta.getCantidad() == null || venta.getCantidad() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cantidad inválida");
        }
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    // 7. Obtener datos oficiales desde productos
    private void completarDatosProducto(Venta venta) {
        ProductoDTO producto;

        try {
            producto = productoClient.obtenerProductoPorId(
                    venta.getProductoId());
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Producto no encontrado");
        } catch (FeignException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Microservicio de productos no disponible");
        }

        if (producto == null || producto.getPrecio() == null
                || !Double.isFinite(producto.getPrecio())
                || producto.getPrecio() < 0
                || producto.getStock() == null
                || producto.getStock() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Datos de producto inválidos");
        }

        if (producto.getStock() < venta.getCantidad()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Stock insuficiente");
        }

        venta.setNombreProducto(producto.getNombre());
        venta.setTotal(producto.getPrecio() * venta.getCantidad());
    }

    // 8. Solicitar movimientos de inventario a productos
    private void modificarStock(Long productoId, int variacion) {
        try {
            productoClient.modificarStock(productoId, variacion);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado");
            }

            if (e.status() == 409) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Stock insuficiente");
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo confirmar el movimiento de stock", e);
        }
    }

    // 9. Intentar revertir un movimiento si falla la venta
    private void compensarStock(Long productoId, int variacion) {
        try {
            modificarStock(productoId, variacion);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo compensar el inventario. Requiere revisión manual",
                    e);
        }
    }
}
