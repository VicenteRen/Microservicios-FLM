
package cl.ferreterialosmaestros.ventas.dto;

import java.util.List;

// Datos que React enviará al registrar una compra.
public record PedidoRequest(
        String nombreCliente,
        String apellidosCliente,
        String correoCliente,
        String direccionEnvio,
        String region,
        String comuna,
        List<Item> productos
) {
    public record Item(
            Long productoId,
            Integer cantidad
    ) {}
}
