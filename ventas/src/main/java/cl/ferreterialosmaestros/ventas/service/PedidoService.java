
package cl.ferreterialosmaestros.ventas.service;

import cl.ferreterialosmaestros.ventas.client.ProductoClient;
import cl.ferreterialosmaestros.ventas.dto.PedidoRequest;
import cl.ferreterialosmaestros.ventas.dto.PedidoResponse;
import cl.ferreterialosmaestros.ventas.dto.ProductoDTO;
import cl.ferreterialosmaestros.ventas.model.DetallePedido;
import cl.ferreterialosmaestros.ventas.model.Pedido;
import cl.ferreterialosmaestros.ventas.repository.PedidoRepository;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PedidoService {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoService.class);

    private static final BigDecimal MONTO_MAXIMO =
            new BigDecimal("9999999999999.99");

    private final PedidoRepository repository;
    private final ProductoClient productoClient;
    private final TransactionTemplate transacciones;

    public PedidoService(
            PedidoRepository repository,
            ProductoClient productoClient,
            TransactionTemplate transacciones) {

        this.repository = repository;
        this.productoClient = productoClient;
        this.transacciones = transacciones;
    }

    // 1. Crear un pedido con varios productos
    public PedidoResponse crear(PedidoRequest solicitud) {

        validarSolicitud(solicitud);

        Pedido pedido = prepararPedido(solicitud);

        List<DetallePedido> descuentosConfirmados =
                new ArrayList<>();

        Pedido guardado;

        try {
            // Descontar stock desde MS-Productos
            for (DetallePedido detalle : pedido.getDetalles()) {

                moverStock(
                        detalle.getProductoId(),
                        -detalle.getCantidad()
                );

                descuentosConfirmados.add(detalle);
            }

            // Guardar pedido y detalles en una transaccion MySQL
            guardado = transacciones.execute(
                    estado -> repository.saveAndFlush(pedido)
            );

            if (guardado == null) {
                throw new IllegalStateException(
                        "No se pudo confirmar el pedido"
                );
            }

        } catch (RuntimeException error) {

            // Intentar devolver el stock descontado
            compensar(descuentosConfirmados, true, error);

            throw error;
        }

        return aRespuesta(guardado);
    }

    // 2. Listar todos los pedidos
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {

        return repository.findAll(
                Sort.by(Sort.Direction.DESC, "id")
        ).stream().map(this::aRespuesta).toList();
    }

    // 3. Obtener un pedido por ID
    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {

        Pedido pedido = repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Pedido no encontrado"
                        )
                );

        return aRespuesta(pedido);
    }

    // 4. Anular un pedido y devolver el inventario
    public void anular(Long id) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID de pedido invalido"
            );
        }

        List<DetallePedido> devolucionesConfirmadas =
                new ArrayList<>();

        try {
            transacciones.executeWithoutResult(estado -> {

                Pedido pedido = repository.buscarParaAnular(id)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Pedido no encontrado"
                                )
                        );

                // Evitar devolver el stock dos veces
                if ("ANULADO".equals(pedido.getEstado())) {
                    return;
                }

                if (!"CONFIRMADO".equals(pedido.getEstado())) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "El pedido no se puede anular"
                    );
                }

                for (DetallePedido detalle : pedido.getDetalles()) {

                    moverStock(
                            detalle.getProductoId(),
                            detalle.getCantidad()
                    );

                    devolucionesConfirmadas.add(detalle);
                }

                pedido.setEstado("ANULADO");

                repository.saveAndFlush(pedido);
            });

        } catch (RuntimeException error) {

            // Intentar revertir las devoluciones parciales
            compensar(devolucionesConfirmadas, false, error);

            throw error;
        }
    }

    // 5. Validar datos del cliente y del carrito
    private void validarSolicitud(PedidoRequest solicitud) {

        if (solicitud == null
                || vacio(solicitud.nombreCliente())
                || vacio(solicitud.apellidosCliente())
                || vacio(solicitud.correoCliente())
                || vacio(solicitud.direccionEnvio())
                || vacio(solicitud.region())
                || vacio(solicitud.comuna())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Complete todos los datos del cliente"
            );
        }

        if (!solicitud.correoCliente().matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Correo invalido"
            );
        }

        if (solicitud.productos() == null
                || solicitud.productos().isEmpty()
                || solicitud.productos().size() > 50) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El pedido debe contener entre 1 y 50 productos distintos"
            );
        }

        Set<Long> ids = new HashSet<>();

        for (PedidoRequest.Item item : solicitud.productos()) {

            if (item == null
                    || item.productoId() == null
                    || item.productoId() <= 0
                    || item.cantidad() == null
                    || item.cantidad() <= 0
                    || item.cantidad() > 10000) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "ID o cantidad de producto invalida"
                );
            }

            // Impedir repetir el mismo producto
            if (!ids.add(item.productoId())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No repita un mismo producto en el pedido"
                );
            }
        }
    }

    // 6. Preparar el pedido usando los datos reales
    private Pedido prepararPedido(PedidoRequest solicitud) {

        Pedido pedido = new Pedido();

        pedido.setNombreCliente(
                solicitud.nombreCliente().trim()
        );

        pedido.setApellidosCliente(
                solicitud.apellidosCliente().trim()
        );

        pedido.setCorreoCliente(
                solicitud.correoCliente().trim()
        );

        pedido.setDireccionEnvio(
                solicitud.direccionEnvio().trim()
        );

        pedido.setRegion(
                solicitud.region().trim()
        );

        pedido.setComuna(
                solicitud.comuna().trim()
        );

        pedido.setEstado("CONFIRMADO");

        BigDecimal total = BigDecimal.ZERO.setScale(2);

        for (PedidoRequest.Item item : solicitud.productos()) {

            ProductoDTO producto =
                    consultarProducto(item.productoId());

            if (producto == null
                    || producto.getPrecio() == null
                    || !Double.isFinite(producto.getPrecio())
                    || producto.getPrecio() < 0
                    || producto.getStock() == null
                    || producto.getStock() < 0
                    || vacio(producto.getNombre())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Datos de producto invalidos"
                );
            }

            if (producto.getStock() < item.cantidad()) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente del producto "
                                + item.productoId()
                );
            }

            // El precio oficial viene desde MS-Productos
            BigDecimal precio =
                    BigDecimal.valueOf(producto.getPrecio())
                            .setScale(2, RoundingMode.HALF_UP);

            BigDecimal subtotal = precio.multiply(
                    BigDecimal.valueOf(item.cantidad())
            );

            total = total.add(subtotal);

            if (total.compareTo(MONTO_MAXIMO) > 0
                    || subtotal.compareTo(MONTO_MAXIMO) > 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Monto del pedido demasiado alto"
                );
            }

            DetallePedido detalle = new DetallePedido();

            detalle.setProductoId(item.productoId());
            detalle.setNombreProducto(producto.getNombre());
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(subtotal);

            pedido.agregarDetalle(detalle);
        }

        pedido.setTotal(total);

        return pedido;
    }

    // 7. Consultar un producto mediante OpenFeign
    private ProductoDTO consultarProducto(Long id) {

        try {
            return productoClient.obtenerProductoPorId(id);

        } catch (FeignException e) {

            if (e.status() == 404) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto " + id + " no encontrado",
                        e
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar el microservicio de productos",
                    e
            );
        }
    }

    // 8. Descontar o devolver stock
    private void moverStock(Long productoId, int variacion) {

        try {
            productoClient.modificarStock(
                    productoId,
                    variacion
            );

        } catch (FeignException e) {

            if (e.status() == 404) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto " + productoId + " no encontrado",
                        e
                );
            }

            if (e.status() == 409) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente del producto "
                                + productoId,
                        e
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo confirmar el movimiento del producto "
                            + productoId,
                    e
            );
        }
    }

    // 9. Intentar compensar movimientos de inventario
    private void compensar(
            List<DetallePedido> realizados,
            boolean compra,
            RuntimeException errorOriginal) {

        boolean falloCompensacion = false;

        // Recorrer los movimientos en orden inverso
        for (int i = realizados.size() - 1; i >= 0; i--) {

            DetallePedido detalle = realizados.get(i);

            int variacion = compra
                    ? detalle.getCantidad()
                    : -detalle.getCantidad();

            try {
                moverStock(
                        detalle.getProductoId(),
                        variacion
                );

            } catch (RuntimeException errorCompensacion) {

                falloCompensacion = true;

                log.error(
                        "Se requiere revision manual del stock del producto {}",
                        detalle.getProductoId(),
                        errorCompensacion
                );

                errorOriginal.addSuppressed(errorCompensacion);
            }
        }

        if (falloCompensacion) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Fallo la compensacion de inventario; requiere revision manual",
                    errorOriginal
            );
        }
    }

    // 10. Convertir el pedido a una respuesta para React
    private PedidoResponse aRespuesta(Pedido pedido) {

        List<PedidoResponse.Item> productos =
                pedido.getDetalles()
                        .stream()
                        .map(detalle ->
                                new PedidoResponse.Item(
                                        detalle.getProductoId(),
                                        detalle.getNombreProducto(),
                                        detalle.getCantidad(),
                                        detalle.getPrecioUnitario(),
                                        detalle.getSubtotal()
                                )
                        )
                        .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getNombreCliente(),
                pedido.getApellidosCliente(),
                pedido.getCorreoCliente(),
                pedido.getDireccionEnvio(),
                pedido.getRegion(),
                pedido.getComuna(),
                pedido.getTotal(),
                pedido.getFechaPedido(),
                pedido.getEstado(),
                productos
        );
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
