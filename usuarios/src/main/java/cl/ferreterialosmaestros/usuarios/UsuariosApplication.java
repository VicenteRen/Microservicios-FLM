package cl.ferreterialosmaestros.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "API Microservicio de Usuarios - Ferretería Los Maestros",
        version = "1.0",
        description = "Gestión de usuarios, roles precargados, contraseñas cifradas con BCrypt y validación por RequestHeader."
    )
)
public class UsuariosApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsuariosApplication.class, args);
    }
}