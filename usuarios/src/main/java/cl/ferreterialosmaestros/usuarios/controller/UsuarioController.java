package cl.ferreterialosmaestros.usuarios.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.ferreterialosmaestros.usuarios.dto.LoginRequest;
import cl.ferreterialosmaestros.usuarios.model.Usuario;
import cl.ferreterialosmaestros.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Usuarios y Autenticación", description = "Operaciones CRUD de usuarios, roles y login con contraseña cifrada")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    private boolean esAdmin(String rolHeader) {
        return rolHeader != null && rolHeader.equalsIgnoreCase("admin");
    }

    // 1. POST LOGIN (Público)
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida correo y contraseña cifrada con BCrypt.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso"),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Usuario usuario = service.login(request.getCorreo(), request.getContrasena());
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Correo o contraseña incorrectos."));
        }
        return ResponseEntity.ok(usuario);
    }

    // 2. POST REGISTRO DE CLIENTES (Público desde FormularioRegistro.tsx)
    @PostMapping("/registro")
    @Operation(summary = "Registrar nuevo cliente", description = "Crea una cuenta con rol 'cliente' y cifra su contraseña.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cliente registrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "RUT o correo ya existente")
    })
    public ResponseEntity<?> registrarCliente(@RequestBody Usuario usuario) {
        try {
            usuario.setRol("cliente");
            Usuario guardado = service.guardar(usuario, "cliente");
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // 3. GET LISTAR TODOS (Protegido con Header X-Usuario-Rol: admin)
    @GetMapping
    @Operation(summary = "Listar todos los usuarios", description = "Requiere enviar el Header X-Usuario-Rol con valor 'admin'.")
    public ResponseEntity<?> listar(
            @Parameter(description = "Rol del usuario que realiza la petición (debe ser 'admin')", example = "admin")
            @RequestHeader(value = "X-Usuario-Rol", required = false) String rolHeader) {
        if (!esAdmin(rolHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Acceso denegado: Se requiere Header X-Usuario-Rol: admin"));
        }
        return ResponseEntity.ok(service.listar());
    }

    // 4. POST CREAR DESDE PANEL ADMIN (Protegido con Header X-Usuario-Rol: admin)
    @PostMapping
    @Operation(summary = "Crear usuario (Admin/Vendedor)", description = "Crea un usuario desde el panel administrativo. Requiere Header X-Usuario-Rol: admin.")
    public ResponseEntity<?> crearDesdeAdmin(
            @Parameter(description = "Rol del usuario autenticado (debe ser 'admin')", example = "admin")
            @RequestHeader(value = "X-Usuario-Rol", required = false) String rolHeader,
            @RequestBody Usuario usuario) {
        if (!esAdmin(rolHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Acceso denegado: Se requiere Header X-Usuario-Rol: admin"));
        }
        try {
            Usuario guardado = service.guardar(usuario, "vendedor");
            return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // 5. PUT EDITAR USUARIO (Protegido con Header X-Usuario-Rol: admin)
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario por ID", description = "Actualiza los datos de un usuario existente. Requiere Header X-Usuario-Rol: admin.")
    public ResponseEntity<?> actualizar(
            @Parameter(description = "Rol del usuario autenticado (debe ser 'admin')", example = "admin")
            @RequestHeader(value = "X-Usuario-Rol", required = false) String rolHeader,
            @PathVariable Long id,
            @RequestBody Usuario usuario) {
        if (!esAdmin(rolHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Acceso denegado: Se requiere Header X-Usuario-Rol: admin"));
        }
        Usuario actualizado = service.actualizar(id, usuario);
        if (actualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(actualizado);
    }

    // 6. DELETE ELIMINAR USUARIO (Protegido con Header X-Usuario-Rol: admin)
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un usuario por ID", description = "Elimina permanentemente un usuario. Requiere Header X-Usuario-Rol: admin.")
    public ResponseEntity<?> eliminar(
            @Parameter(description = "Rol del usuario autenticado (debe ser 'admin')", example = "admin")
            @RequestHeader(value = "X-Usuario-Rol", required = false) String rolHeader,
            @PathVariable Long id) {
        if (!esAdmin(rolHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Acceso denegado: Se requiere Header X-Usuario-Rol: admin"));
        }
        boolean eliminado = service.eliminar(id);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}