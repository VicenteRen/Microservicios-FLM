package cl.ferreterialosmaestros.productos.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.ferreterialosmaestros.productos.model.Producto;
import cl.ferreterialosmaestros.productos.repository.ProductoRepository;

@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<Producto> listarProductos() {
        return repository.findAll();
    }

    public Producto obtenerProductoPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Producto guardarProducto(Producto productoGuardado) {
        if (productoGuardado.getPrecioCompra() == null || productoGuardado.getPrecioCompra() == 0) {
            productoGuardado.setPrecioCompra(productoGuardado.getPrecio());
        }
        return repository.save(productoGuardado);
    }

    public void eliminarProductoPorId(long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void modificarStock(Long id, int variacion) {
        if (variacion == 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La variacion no puede ser cero"
            );
        }

        int filas = repository.modificarStock(id, variacion);

        if (filas == 0) {
            if (!repository.existsById(id)) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Producto no encontrado"
                );
            }

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Stock insuficiente"
            );
        }
    }
}