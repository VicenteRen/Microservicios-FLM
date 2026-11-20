package cl.ferreterialosmaestros.mensajes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.ferreterialosmaestros.mensajes.model.Mensaje;
import cl.ferreterialosmaestros.mensajes.repository.MensajeRepository;

@Service 
public class MensajeService {
    private final MensajeRepository repository;

    public MensajeService(MensajeRepository repository) {
        this.repository = repository;
    }

    public Mensaje guardar(Mensaje contacto) {
        contacto.setId(null);
        contacto.setEstado("PENDIENTE");
        return repository.save(contacto);
    }

    public List<Mensaje> listar() {
        return repository.findAll();
    }

    // PUT: Actualizar el estado a ATENDIDO mediante su ID
    public Mensaje atender(Long id) {
        Mensaje contacto = repository.findById(id).orElse(null);
        if (contacto == null) {
            return null;
        }
        contacto.setEstado("ATENDIDO");
        return repository.save(contacto);
    }

    // DELETE: Eliminar mediante su ID
    public boolean eliminar(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }
}
