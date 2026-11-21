
package cl.ferreterialosmaestros.ventas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Información que el backend devuelve a React.
public record PedidoResponse(
        Long id,
        String nombreCliente,
        String apellidosCliente,
        String correoCliente,
        String direccionEnvio,
        String region,
        String comuna,
        BigDecimal total,
        LocalDateTime fechaPedido,
        String estado,
        List<Item> productos
) {
    public record Item(
            Long productoId,
            String nombreProducto,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}
}
