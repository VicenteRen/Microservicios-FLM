package cl.ferreterialosmaestros.ventas.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "API Microservicio de Ventas - Ferretería Los Maestros",
        version = "1.0.0",
        description = "API REST para la gestión de ventas de Ferretería Los Maestros. Permite registrar, consultar, actualizar y anular ventas, utilizando MySQL, Flyway y comunicación con productos mediante OpenFeign."
    )
)
public class OpenApiConfig {
}
