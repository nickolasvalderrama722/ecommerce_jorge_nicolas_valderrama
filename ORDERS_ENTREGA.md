# Módulo de pedidos

Esta rama `feature/order-module` añade el módulo Orders al backend existente. Usa la estructura
`entity → repository → service → controller`, sin modificar el contrato de productos o categorías.

## Archivos y requisitos

| Requisito de la guía | Implementación |
| --- | --- |
| Entidad `orders` y columnas obligatorias | `OrderEntity` |
| Consultas derivadas y JPQL | `OrderRepository` |
| Validaciones, transacciones, estado inicial y cancelación segura | `OrderService` |
| Seis rutas solicitadas y Swagger con JWT | `OrderController` |
| Pruebas unitarias sin base de datos | `OrderServiceTest` con JUnit Jupiter y Mockito |

La guía enumera `PENDING`, `PAID` y `CANCELLED` como estados admitidos, pero también exige
impedir cancelar órdenes que ya figuren como `SHIPPED`. Por eso el servicio rechaza `SHIPPED`
como **nuevo** estado y conserva la comprobación de seguridad para registros que ya tengan ese valor.
Se añadió `PUT /api/orders/{id}/cancel` para poder ejercer la regla de cancelación desde Swagger.

## Ejecutar localmente

1. Abrir el proyecto Maven con JDK 25 y PostgreSQL disponible.
2. Crear la base de datos `ecommerce`, o definir `SPRING_DATASOURCE_URL`,
   `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` para otra base.
3. Ejecutar `mvnw.cmd spring-boot:run` en Windows o `bash mvnw spring-boot:run` en Linux/macOS.
4. Abrir `http://localhost:8081/swagger-ui/index.html`. Registrar un usuario con
   `POST /api/auth/register` o iniciar sesión con `POST /api/auth/login`, copiar el token
   y pulsar **Authorize** en Swagger (esquema `BearerAuth`).
5. Probar `POST /api/orders` con el siguiente JSON:

```json
{
  "customerId": 1,
  "totalAmount": 185000.50
}
```

El cliente con ID `1` debería existir para representar un pedido real. La guía usa un identificador
simple y no exige una relación JPA ni verificación de existencia del cliente al crear el pedido.
La respuesta contiene el ID generado, la fecha y el estado `PENDING`.

Rutas: `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}`,
`GET /api/orders/customer/{customerId}`, `GET /api/orders/status/{status}`,
`PUT /api/orders/{id}/status?status=PAID`, `PUT /api/orders/{id}/cancel`.

Para ejecutar solo las pruebas aisladas: `mvnw.cmd -Dtest=OrderServiceTest test` en Windows,
o `bash mvnw -Dtest=OrderServiceTest test` en Linux/macOS. El resto de pruebas del repositorio
puede necesitar PostgreSQL porque ya existían pruebas con `@SpringBootTest`.

## Preparar Render

Crear una base PostgreSQL y un Web Service con runtime **Docker** conectado a la rama publicada
en GitHub. `Dockerfile` ya está en la raíz y la aplicación usa el puerto indicado por `PORT`.
Definir estas variables de entorno en el servicio web:

| Variable | Valor |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL JDBC, por ejemplo `jdbc:postgresql://host:5432/ecommerce` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de PostgreSQL |
| `APP_JWT_SECRET` | Clave privada aleatoria de al menos 32 caracteres |
| `APP_CORS_ALLOWED_ORIGINS` | Origen exacto del frontend publicado, por ejemplo `https://tu-frontend.onrender.com` |

La URL estándar de Render para PostgreSQL comienza por `postgresql://`; para esta aplicación
se debe usar el formato **JDBC** `jdbc:postgresql://...` y configurar usuario y contraseña
en variables separadas. Para el frontend publicado, configurar `VITE_API_BASE_URL` con
`https://tu-backend.onrender.com/api` y construirlo de nuevo.

## Alcance de la integración con el frontend actual

El frontend del repositorio enlazado envía al checkout un objeto con `buyer`, `items`, `total`
y un `id` alfanumérico. La actividad pide `{ "customerId": 1, "totalAmount": 185000.50 }`.
El frontend tampoco recibe actualmente `customerId` en el contrato de autenticación.
Por eso el módulo se puede demostrar en Swagger/Postman, pero **el checkout existente aún no
registra pedidos compatibles en PostgreSQL**. La integración de checkout requiere acordar
cómo vincular la sesión con un registro de `customers` y transformar el payload antes de enviar.

La guía nombra Spring Boot 3 y JUnit 5, mientras que el repositorio base usa Spring Boot 4.1.2,
que gestiona JUnit Jupiter 6.0.3. Las pruebas usan las mismas anotaciones `org.junit.jupiter`
y Mockito que muestra la guía. Se conservó la versión real del proyecto. No se incluye un
despliegue ni un push remoto.
