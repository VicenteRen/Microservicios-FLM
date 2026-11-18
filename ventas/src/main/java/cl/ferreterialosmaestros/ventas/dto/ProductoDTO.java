package cl.ferreterialosmaestros.ventas.dto;

import lombok.Data;

@Data
public class ProductoDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String categoria;
    private String subcategoria;
    private String marca;
    private String unidad;
    private Double precioCompra;
    private Double precio;
    private Integer stock;
    private Integer stockMinimo;
    private String imagen;
    private String descripcion;
}