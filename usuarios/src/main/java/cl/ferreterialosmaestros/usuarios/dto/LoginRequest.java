package cl.ferreterialosmaestros.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "LoginRequest", description = "Credenciales para iniciar sesión")
public class LoginRequest {

    @Schema(description = "Correo del usuario", example = "admin@ejemplo.cl")
    private String correo;

    @Schema(description = "Contraseña en texto plano", example = "Admin123")
    private String contrasena;
}