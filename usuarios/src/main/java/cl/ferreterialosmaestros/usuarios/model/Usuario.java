package cl.ferreterialosmaestros.usuarios.model;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@Entity
@Table(name = "usuario")
@Schema(name = "Usuario", description = "Entidad que almacena clientes, vendedores y administradores")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID autogenerado del usuario", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    @Schema(description = "RUT/RUN sin puntos ni guion", example = "112223334")
    private String rut;

    @Column(nullable = false, length = 100)
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;

    @Column(length = 100)
    @Schema(description = "Apellidos del usuario", example = "Pérez")
    private String apellidos;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    @Schema(description = "Nombre completo para tablas administrativas", example = "Juan Pérez")
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 150)
    @Schema(description = "Correo electrónico único", example = "juan@ejemplo.cl")
    private String correo;

    @Column(nullable = false, length = 255)
    @Schema(description = "Contraseña (se almacena cifrada con BCrypt en la BD)", example = "Admin123")
    private String contrasena;

    @Column(length = 20)
    @Schema(description = "Teléfono de contacto formato chileno", example = "+56911112222")
    private String telefono;

    @Column(name = "fecha_nacimiento", length = 20)
    @Schema(description = "Fecha de nacimiento (YYYY-MM-DD)", example = "1998-05-14")
    private String fechaNacimiento;

    @Column(length = 100)
    @Schema(description = "Región de residencia", example = "Región Metropolitana de Santiago")
    private String region;

    @Column(length = 100)
    @Schema(description = "Comuna de residencia", example = "Santiago")
    private String comuna;

    @Column(length = 300)
    @Schema(description = "Dirección física", example = "Av. Los Maestros 123")
    private String direccion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    @Schema(description = "Objeto Rol asignado en la base de datos", accessMode = Schema.AccessMode.READ_ONLY)
    private Rol rolEntidad;

    // Campo auxiliar para que desde React y Postman puedas enviar y recibir simplemente "rol": "admin"
    @Transient
    @Schema(description = "Nombre del rol ('admin', 'vendedor' o 'cliente')", example = "admin")
    private String rol;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void asignarFechaCreacion() {
        this.fechaCreacion = LocalDateTime.now();
    }

    // Sincroniza automáticamente el String "rol" al convertir a JSON para React
    public String getRol() {
        if (this.rol != null && !this.rol.isBlank()) {
            return this.rol;
        }
        return this.rolEntidad != null ? this.rolEntidad.getNombre() : null;
    }
}