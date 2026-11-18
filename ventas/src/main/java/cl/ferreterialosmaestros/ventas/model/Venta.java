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

    // Datos del cliente (Checkout)
    @Column(nullable = false)
    private String nombreCliente;

    private String apellidosCliente;

    @Column(nullable = false)
    private String correoCliente;

    @Column(nullable = false)
    private String direccionEnvio;

    private String region;
    private String comuna;

    // Datos del producto conectado vía OpenFeign con ms-productos
    @Column(nullable = false)
    private Long productoId;

    private String nombreProducto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Double total;

    private LocalDateTime fechaVenta;

    @PrePersist
    public void prePersist() {
        if (this.fechaVenta == null) {
            this.fechaVenta = LocalDateTime.now();
        }
    }
}