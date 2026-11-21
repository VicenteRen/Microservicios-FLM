package cl.ferreterialosmaestros.productos.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.ferreterialosmaestros.productos.model.Producto;
import cl.ferreterialosmaestros.productos.repository.ProductoRepository;

@Service 
public class ProductoService {

    @Autowired 
    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository){
        this.repository = repository;
    }

    public List<Producto> listarProductos(){
        return repository.findAll();
    }

    public Producto obtenerProductoPorId(Long id){
        return repository.findById(id).orElse(null);
    }

    public Producto guardarProducto(Producto productoGuardado){
        if (productoGuardado.getPrecioCompra() == null || productoGuardado.getPrecioCompra() == 0){
            productoGuardado.setPrecioCompra(productoGuardado.getPrecio());
        }
        return repository.save(productoGuardado);
    }

    public void eliminarProductoPorId(long id){
        repository.deleteById(id);
    }
    
}
