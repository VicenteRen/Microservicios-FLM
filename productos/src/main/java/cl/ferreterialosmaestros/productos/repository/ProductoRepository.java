package cl.ferreterialosmaestros.productos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import cl.ferreterialosmaestros.productos.model.Producto;

@Repository 
public interface ProductoRepository extends JpaRepository <Producto, Long>{
    
}
