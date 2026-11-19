
package cl.ferreterialosmaestros.ventas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos del cliente
    @Column(nullable = false)
    private String nombreCliente;

    private String apellidosCliente;

    @Column(nullable = false)
    private String correoCliente;

    @Column(nullable = false)
    private String direccionEnvio;

    private String region;
    private String comuna;

    // Datos del producto
    @Column(nullable = false)
    private Long productoId;

    private String nombreProducto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Double total;

    private LocalDateTime fechaVenta;

    // Estado de la venta
    @Column(length = 20)
    private String estado = "ACTIVA";

    @PrePersist
    public void prePersist() {
        if (fechaVenta == null) {
            fechaVenta = LocalDateTime.now();
        }

        if (estado == null || estado.isBlank()) {
            estado = "ACTIVA";
        }
    }

    @PostLoad
    public void postLoad() {
        if (estado == null || estado.isBlank()) {
            estado = "ACTIVA";
        }
    }
}
