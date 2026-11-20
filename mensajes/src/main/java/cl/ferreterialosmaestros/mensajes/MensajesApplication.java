package cl.ferreterialosmaestros.mensajes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "API Microservicio de Mensajes - Ferretería Los Maestros",
        version = "1.0",
        description = "Documentación de endpoints para el registro, consulta, atención y eliminación de mensajes de contacto."
    )
)
public class MensajesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MensajesApplication.class, args);
	}

}
