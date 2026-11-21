package cl.ferreterialosmaestros.productos.repository;

import cl.ferreterialosmaestros.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Modifying
    @Query(value = """
        UPDATE productos
        SET stock = stock + :variacion
        WHERE id = :id
        AND stock + :variacion >= 0
        """, nativeQuery = true)
    int modificarStock(@Param("id") Long id,
                       @Param("variacion") int variacion);
}