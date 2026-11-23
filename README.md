# Ferretería Los Maestros - Microservicios

Backend de la aplicación web **Ferretería Los Maestros**, desarrollado en Java y Spring Boot. Permite gestionar usuarios, mensajes de contacto, catálogo de productos y ventas mediante APIs REST, utilizando una arquitectura de microservicios con comunicación entre servicios vía OpenFeign.

## Integrantes

- Vicente Céspedes
- Vicente Ponce
- Emilia Gallardo

## Tecnologías utilizadas

- Java 21 y Spring Boot 4.0.9
- Spring Cloud OpenFeign
- Spring Data JPA, MySQL y Flyway
- Spring Security Crypto (BCrypt)
- Maven y Lombok
- JUnit 5, Mockito y JaCoCo
- Swagger / OpenAPI (SpringDoc)
- Git y GitHub

## Arquitectura y funcionalidades

| Microservicio | Puerto | Base de datos | Funcionalidad |
|---|---|---|---|
| Usuarios | 8081 | `usuarios` | Gestión de usuarios y roles, registro, autenticación con BCrypt y control de acceso por header (`X-Usuario-Rol`) |
| Mensajes | 8082 | `mensajeria` | Registro, consulta, marcado como atendido y eliminación de mensajes de contacto |
| Productos | 8083 | `productos` | Gestión CRUD del catálogo e inventario y movimientos de stock |
| Ventas | 8084 | `ventas` | Registro, consulta, actualización y anulación de pedidos con validación y descuento de stock vía OpenFeign |

## Principales endpoints

Cada microservicio expone su API REST con CORS habilitado para el frontend web (`http://localhost:5173`).

| Microservicio | Método | Endpoint | Descripción |
|---|---|---|---|
| Usuarios (`8081`) | POST | `/api/usuarios/login` | Autenticación con correo y contraseña cifrada (BCrypt) |
| Usuarios (`8081`) | POST | `/api/usuarios/registro` | Registro público de clientes |
| Usuarios (`8081`) | GET | `/api/usuarios` | Listar usuarios *(requiere header `X-Usuario-Rol: admin`)* |
| Usuarios (`8081`) | POST | `/api/usuarios` | Crear usuario desde panel admin *(requiere header `X-Usuario-Rol: admin`)* |
| Usuarios (`8081`) | PUT | `/api/usuarios/{id}` | Actualizar usuario *(requiere header `X-Usuario-Rol: admin`)* |
| Usuarios (`8081`) | DELETE | `/api/usuarios/{id}` | Eliminar usuario *(requiere header `X-Usuario-Rol: admin`)* |
| Mensajes (`8082`) | POST | `/api/contactos` | Registrar nuevo mensaje de contacto |
| Mensajes (`8082`) | GET | `/api/contactos` | Listar todos los mensajes |
| Mensajes (`8082`) | PUT | `/api/contactos/{id}` | Marcar mensaje como `ATENDIDO` |
| Mensajes (`8082`) | DELETE | `/api/contactos/{id}` | Eliminar mensaje |
| Productos (`8083`) | GET | `/api/productos` | Listar catálogo completo de productos |
| Productos (`8083`) | GET | `/api/productos/{id}` | Obtener producto por ID |
| Productos (`8083`) | POST | `/api/productos` | Crear nuevo producto |
| Productos (`8083`) | PUT | `/api/productos/{id}` | Actualizar producto existente |
| Productos (`8083`) | DELETE | `/api/productos/{id}` | Eliminar producto |
| Productos (`8083`) | POST | `/api/productos/{id}/movimiento-stock` | Descontar o restituir unidades (`?variacion=`) |
| Ventas (`8084`) | POST | `/api/ventas` | Registrar pedido y descontar stock en Productos |
| Ventas (`8084`) | GET | `/api/ventas` | Listar todos los pedidos |
| Ventas (`8084`) | GET | `/api/ventas/{id}` | Consultar pedido por ID |
| Ventas (`8084`) | PUT | `/api/ventas/{id}` | Actualizar venta confirmada y ajustar inventario |
| Ventas (`8084`) | DELETE | `/api/ventas/{id}` | Anular pedido y restituir stock |

La documentación completa e interactiva de los endpoints está disponible mediante Swagger UI (`/swagger-ui/index.html`) en los puertos **8081**, **8082**, **8083** y **8084**.

## Instalación y ejecución

### 1. Clonar el repositorio

```bash
git clone [https://github.com/VicenteRen/Microservicios-FLM.git](https://github.com/VicenteRen/Microservicios-FLM.git)
```

### 2. Configurar MySQL

Iniciar MySQL en el puerto `3306` y verificar las credenciales en el archivo `application.properties` de cada módulo (por defecto usuario `root` sin contraseña).

Las bases de datos utilizadas son:

- `usuarios`
- `mensajeria`
- `productos`
- `ventas`

Todos los microservicios incluyen `createDatabaseIfNotExist=true` para permitir la creación automática de sus bases de datos y utilizan **Flyway** para ejecutar las migraciones de tablas al iniciar.

### 3. Ejecutar los microservicios

Se requiere **JDK 21**. Abrir una terminal independiente para cada módulo e iniciarlos (iniciando `productos` antes de operar con `ventas`):

1. `usuarios` (puerto `8081`)
2. `mensajes` (puerto `8082`)
3. `productos` (puerto `8083`)
4. `ventas` (puerto `8084`)

Desde la carpeta de cada módulo ejecutar:

```bat
mvnw.cmd spring-boot:run
```

## Pruebas unitarias y cobertura

Se implementaron pruebas con **JUnit 5 y Mockito**, utilizando **JaCoCo** para medir la cobertura del código.

| Microservicio | Cobertura de instrucciones |
|---|---|
| Usuarios | 97 % |
| Mensajes | 92 % |
| Productos | 97 % |
| Ventas | 98 % |

Los cuatro microservicios superan el **80 % mínimo de cobertura**.

Para ejecutar las pruebas y generar los informes:

```bat
mvnw.cmd clean verify
```

Los resultados se consultan en `target/site/jacoco/index.html` dentro de cada microservicio.

## Aplicación web

El backend se integra con la aplicación web de **Ferretería Los Maestros**, desarrollada en React, TypeScript y Vite.

**Repositorio Web:**  
https://github.com/VicenteRen/Ferreteria-Los-Maestros-React

### Conexión con el frontend local

La aplicación web corre en `http://localhost:5173` y consume directamente los cuatro microservicios locales mediante `fetch`:

- **Usuarios:** `http://localhost:8081/api/usuarios`
- **Mensajes:** `http://localhost:8082/api/contactos`
- **Productos:** `http://localhost:8083/api/productos`
- **Ventas:** `http://localhost:8084/api/ventas`

## Control de versiones

El proyecto utiliza GitHub para registrar los avances mediante commits semánticos, ramas de trabajo y Pull Requests, permitiendo organizar el desarrollo colaborativo.
