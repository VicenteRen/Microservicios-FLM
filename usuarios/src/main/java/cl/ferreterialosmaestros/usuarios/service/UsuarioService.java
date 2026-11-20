package cl.ferreterialosmaestros.usuarios.service;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import cl.ferreterialosmaestros.usuarios.model.Rol;
import cl.ferreterialosmaestros.usuarios.model.Usuario;
import cl.ferreterialosmaestros.usuarios.repository.RolRepository;
import cl.ferreterialosmaestros.usuarios.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // Precarga los usuarios iniciales con contraseña cifrada si la BD está vacía
    @PostConstruct
    public void precargarUsuariosIniciales() {
        if (usuarioRepository.count() == 0) {
            Rol rolAdmin = rolRepository.findByNombreIgnoreCase("admin").orElseThrow();
            Rol rolVendedor = rolRepository.findByNombreIgnoreCase("vendedor").orElseThrow();

            Usuario admin = new Usuario();
            admin.setRut("112223334");
            admin.setNombre("Admin");
            admin.setApellidos("Sistema");
            admin.setNombreCompleto("Admin Sistema");
            admin.setCorreo("admin@ejemplo.cl");
            admin.setContrasena(passwordEncoder.encode("Admin123"));
            admin.setRegion("Región Metropolitana de Santiago");
            admin.setComuna("Santiago");
            admin.setTelefono("+56911112222");
            admin.setRolEntidad(rolAdmin);
            usuarioRepository.save(admin);

            Usuario vendedor = new Usuario();
            vendedor.setRut("998887776");
            vendedor.setNombre("Vendedor");
            vendedor.setApellidos("Base");
            vendedor.setNombreCompleto("Vendedor Base");
            vendedor.setCorreo("vendedor@ejemplo.cl");
            vendedor.setContrasena(passwordEncoder.encode("Vendedor123"));
            vendedor.setRegion("Región Metropolitana de Santiago");
            vendedor.setComuna("Providencia");
            vendedor.setTelefono("+56933334444");
            vendedor.setRolEntidad(rolVendedor);
            usuarioRepository.save(vendedor);
        }
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario guardar(Usuario usuario, String rolPorDefecto) {
        usuario.setId(null);

        // Normalizar RUT y Correo
        usuario.setRut(usuario.getRut().replace(".", "").replace("-", "").trim().toUpperCase());
        usuario.setCorreo(usuario.getCorreo().trim().toLowerCase());

        if (usuarioRepository.existsByCorreoIgnoreCase(usuario.getCorreo())) {
            throw new IllegalArgumentException("Este correo electrónico ya está registrado.");
        }
        if (usuarioRepository.existsByRutIgnoreCase(usuario.getRut())) {
            throw new IllegalArgumentException("Este RUT/RUN ya está registrado.");
        }

        // Sincronizar nombre y nombreCompleto según de qué formulario venga
        sincronizarNombres(usuario);

        // Asignar Rol desde la tabla rol
        String nombreRol = (usuario.getRol() != null && !usuario.getRol().isBlank())
                ? usuario.getRol()
                : rolPorDefecto;
        Rol rol = rolRepository.findByNombreIgnoreCase(nombreRol)
                .orElseThrow(() -> new IllegalArgumentException("El rol especificado no existe: " + nombreRol));
        usuario.setRolEntidad(rol);

        // Si se crea desde el panel Admin sin especificar contraseña, se asigna los primeros 6 dígitos del RUT
        String passPlana = (usuario.getContrasena() != null && !usuario.getContrasena().isBlank())
                ? usuario.getContrasena()
                : usuario.getRut().substring(0, Math.min(6, usuario.getRut().length()));

        // CIFRADO DE CONTRASEÑA CON BCRYPT
        usuario.setContrasena(passwordEncoder.encode(passPlana));

        return usuarioRepository.save(usuario);
    }

    public Usuario actualizar(Long id, Usuario datosNuevos) {
        Usuario existente = usuarioRepository.findById(id).orElse(null);
        if (existente == null) {
            return null;
        }

        existente.setCorreo(datosNuevos.getCorreo().trim().toLowerCase());
        existente.setTelefono(datosNuevos.getTelefono());
        existente.setRegion(datosNuevos.getRegion());
        existente.setComuna(datosNuevos.getComuna());
        existente.setDireccion(datosNuevos.getDireccion());

        if (datosNuevos.getNombreCompleto() != null && !datosNuevos.getNombreCompleto().isBlank()) {
            existente.setNombreCompleto(datosNuevos.getNombreCompleto().trim());
            existente.setNombre(datosNuevos.getNombreCompleto().trim());
        }

        if (datosNuevos.getRol() != null && !datosNuevos.getRol().isBlank()) {
            Rol nuevoRol = rolRepository.findByNombreIgnoreCase(datosNuevos.getRol())
                    .orElseThrow(() -> new IllegalArgumentException("Rol no válido"));
            existente.setRolEntidad(nuevoRol);
        }

        // Solo re-ciframos la contraseña si se envió una nueva distinta de vacío
        if (datosNuevos.getContrasena() != null && !datosNuevos.getContrasena().isBlank()
                && !datosNuevos.getContrasena().startsWith("$2a$")) {
            existente.setContrasena(passwordEncoder.encode(datosNuevos.getContrasena()));
        }

        return usuarioRepository.save(existente);
    }

    public boolean eliminar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            return false;
        }
        usuarioRepository.deleteById(id);
        return true;
    }

    // LOGIN: Verifica el correo y compara la contraseña en texto plano contra el hash BCrypt
    public Usuario login(String correo, String contrasenaPlana) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim()).orElse(null);
        if (usuario == null) {
            return null;
        }
        if (!passwordEncoder.matches(contrasenaPlana, usuario.getContrasena())) {
            return null;
        }
        return usuario;
    }

    private void sincronizarNombres(Usuario usuario) {
        if (usuario.getNombreCompleto() == null || usuario.getNombreCompleto().isBlank()) {
            String n = usuario.getNombre() != null ? usuario.getNombre().trim() : "";
            String a = usuario.getApellidos() != null ? usuario.getApellidos().trim() : "";
            usuario.setNombreCompleto((n + " " + a).trim());
        } else if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            usuario.setNombre(usuario.getNombreCompleto().trim());
        }
    }
}