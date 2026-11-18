package cl.ferreterialosmaestros.ventas.repository;

import cl.ferreterialosmaestros.ventas.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
}