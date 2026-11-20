package cl.ferreterialosmaestros.mensajes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.ferreterialosmaestros.mensajes.model.Mensaje;


public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

}
