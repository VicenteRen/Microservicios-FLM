package cl.ferreterialosmaestros.mensajes.model;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "contacto")
@Schema(name = "Mensaje", description = "Entidad que representa un mensaje de contacto enviado por un usuario")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
        description = "Identificador único autogenerado del mensaje",
        example = "1",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    @Schema(
        description = "Nombre completo de la persona que envía el mensaje",
        example = "Juan Perez",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String nombreCompleto;

    @Column(nullable = false, length = 150)
    @Schema(
        description = "Correo electrónico de contacto",
        example = "juan@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String correo;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Schema(
        description = "Contenido de la consulta o comentario del cliente",
        example = "Quisiera consultar por el stock de un taladro percutor.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String mensaje;

    @Column(name = "estado", nullable = false, length = 20)
    @Schema(
        description = "Estado actual de gestión del mensaje (PENDIENTE o ATENDIDO)",
        example = "PENDIENTE",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private String estado = "PENDIENTE";

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Schema(
        description = "Fecha y hora exacta en que se registró el mensaje en el servidor",
        example = "2026-11-15T18:30:00",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void asignarFechaCreacion() {
        this.fechaCreacion = LocalDateTime.now();
    }
}