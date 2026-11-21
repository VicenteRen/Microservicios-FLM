package cl.ferreterialosmaestros.productos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Column(nullable = false, length = 50)
    private String codigo;

    @NotEmpty
    @Column(nullable = false, length = 100)
    private String nombre;
 
    @Column(length = 255)
    private String categoria;

    @Column(length = 100)
    private String subcategoria;

    @Column(length = 100)
    private String marca;

    @NotEmpty 
    @Column(length = 50)
    private String unidad = "Unidad";

    @NotNull
    @Column(name = "precio_compra")
    private Double precioCompra = 0.0;

    @NotNull
    @Column(nullable = false)
    private Double precio;

    @NotNull
    @Column(nullable = false)
    private Integer stock;

    @NotNull
    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 5;

    @NotEmpty
    @Column(nullable = false, length = 255)
    private String imagen;

    @NotEmpty
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;
}