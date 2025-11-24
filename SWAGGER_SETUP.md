# Swagger/OpenAPI Documentation Setup

## ✅ What Was Added

### 1. Dependency
Added SpringDoc OpenAPI (Swagger) for WebFlux:
```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")
```

### 2. Configuration
- **OpenApiConfig.kt**: API metadata and documentation info
- **application.yml**: Swagger UI path and settings

### 3. Annotations
Added Swagger annotations to REST controllers:
- `@Tag`: Groups endpoints (Authentication only)
- `@Operation`: Describes each endpoint
- `@ApiResponses`: Documents response codes
- `@Parameter`: Describes parameters

**Note:** Pokemon operations are exclusively available via GraphQL. REST is only used for authentication.

---

## 🚀 How to Access Swagger UI

### Step 1: Restart the Server

**Important:** The server must be restarted after adding Swagger dependency.

```bash
# Stop any running server
pkill -f "PokemonApplication"

# Start the server
./gradlew bootRun
```

### Step 2: Open Swagger UI

Once the server is running, access Swagger at:

**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

**Alternative paths:**
- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/swagger-ui`

**OpenAPI JSON Spec:** `http://localhost:8080/api-docs`

---

## 📋 Available Endpoints in Swagger

### Authentication Tag
- **POST /api/login** - User authentication

**Note:** All Pokemon operations (list, details, pagination) are available via GraphQL at `/graphql` and `/graphql-playground`. See [GRAPHQL_EXAMPLES.md](./GRAPHQL_EXAMPLES.md) for Pokemon query examples.

---

## 🎯 Using Swagger UI

1. **Open Swagger UI** in your browser
2. **Expand a tag** (e.g., "Authentication")
3. **Click on an endpoint** (e.g., "POST /api/login")
4. **Click "Try it out"**
5. **Enter request data**
6. **Click "Execute"**
7. **View the response**

---

## 📝 Example: Testing Login in Swagger

1. Open: `http://localhost:8080/swagger-ui/index.html`
2. Find: **POST /api/login** under "Authentication"
3. Click: **"Try it out"**
4. Enter request body:
   ```json
   {
     "username": "admin",
     "password": "admin"
   }
   ```
5. Click: **"Execute"**
6. See response with token

---

## 🔧 Configuration Details

### OpenApiConfig.kt
- API title: "Pokemon Backend API"
- Version: "1.0.0"
- Description: "REST API for authentication. Pokemon operations are available via GraphQL."

### application.yml
- Swagger UI path: `/swagger-ui`
- API docs path: `/api-docs`
- Operations sorted by: method
- Tags sorted: alphabetically

---

## 🎨 Swagger Features

✅ **Interactive Testing** - Test endpoints directly
✅ **Request/Response Examples** - See example payloads
✅ **Schema Documentation** - View data models
✅ **Parameter Descriptions** - Understand each parameter
✅ **Response Codes** - See all possible responses
✅ **Try It Out** - Execute requests without cURL

---

## 🔍 Troubleshooting

### Swagger UI Not Loading

1. **Restart the server:**
   ```bash
   pkill -f "PokemonApplication"
   ./gradlew bootRun
   ```

2. **Check the path:**
   - Try: `http://localhost:8080/swagger-ui/index.html`
   - Try: `http://localhost:8080/swagger-ui.html`
   - Try: `http://localhost:8080/swagger-ui`

3. **Check logs** for errors

4. **Verify dependency** is in build.gradle.kts:
   ```kotlin
   implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")
   ```

### Endpoints Not Showing

- Ensure `@RestController` annotation is present
- Check that controllers are in the correct package
- Verify server started without errors

---

## 📚 Additional Resources

- **SpringDoc OpenAPI Docs**: https://springdoc.org/
- **OpenAPI Specification**: https://swagger.io/specification/
- **Swagger UI Guide**: See SWAGGER_GUIDE.md

---

**Access Swagger:** `http://localhost:8080/swagger-ui/index.html`

