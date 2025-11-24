# Swagger/OpenAPI Documentation Guide

## 📚 Access Swagger UI

After starting the server, access Swagger documentation at:

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

**OpenAPI JSON:** `http://localhost:8080/api-docs`

---

## 🚀 Quick Start

1. **Start the server:**
   ```bash
   ./gradlew bootRun
   ```

2. **Open Swagger UI in browser:**
   ```
   http://localhost:8080/swagger-ui.html
   ```

3. **Explore the API:**
   - Click on "Authentication" to see login endpoint
   - Click "Try it out" to test endpoints directly
   - **Note:** All Pokemon operations are available via GraphQL at `/graphql` and `/graphql-playground`

---

## 📋 Available Endpoints in Swagger

### Authentication Endpoints

#### POST /api/login
- **Description:** User login with username and password
- **Request Body:**
  ```json
  {
    "username": "admin",
    "password": "admin"
  }
  ```
- **Responses:**
  - `200 OK`: Login successful
  - `401 Unauthorized`: Invalid credentials

### Pokemon Operations (GraphQL Only)

All Pokemon operations are exclusively available via GraphQL:

- **GraphQL Endpoint:** `POST /graphql`
- **GraphQL Playground:** `GET /graphql-playground`

**Available GraphQL Operations:**
- `pokemon(id: Int!)` - Get Pokemon by ID
- `pokemons(first: Int, after: String, sortBy: String)` - Get paginated Pokemon list

**📚 See [GRAPHQL_EXAMPLES.md](./GRAPHQL_EXAMPLES.md) for complete GraphQL query examples**

---

## 🎯 Using Swagger UI

### 1. Test Login Endpoint

1. Open Swagger UI: `http://localhost:8080/swagger-ui.html`
2. Expand "Authentication" section
3. Click on `POST /api/login`
4. Click "Try it out"
5. Enter request body:
   ```json
   {
     "username": "admin",
     "password": "admin"
   }
   ```
6. Click "Execute"
7. See the response with token

### 2. Test Pokemon Operations (GraphQL)

Pokemon operations are available via GraphQL, not REST:

1. **Open GraphQL Playground:** `http://localhost:8080/graphql-playground`
2. **Or use GraphQL endpoint:** `POST http://localhost:8080/graphql`

**Example: Get Pokemon by ID**
```graphql
query {
  pokemon(id: 1) {
    id
    name
    number
    imageUrl
  }
}
```

**Example: Get Paginated Pokemons**
```graphql
query {
  pokemons(first: 10, sortBy: "name") {
    edges {
      node {
        id
        name
        number
      }
      cursor
    }
    pageInfo {
      hasNextPage
    }
  }
}
```

**📚 See [GRAPHQL_EXAMPLES.md](./GRAPHQL_EXAMPLES.md) for complete examples**

---

## 🔧 Configuration

Swagger is configured in:
- **OpenApiConfig.kt**: API metadata and info
- **application.yml**: Swagger UI path and settings

### Customization

Edit `OpenApiConfig.kt` to customize:
- API title
- Description
- Version
- Contact information
- License

---

## 📖 API Documentation Features

✅ **Interactive Testing**: Test endpoints directly from browser
✅ **Request/Response Examples**: See example payloads
✅ **Schema Documentation**: View data models
✅ **Parameter Descriptions**: Understand each parameter
✅ **Response Codes**: See all possible responses
✅ **Try It Out**: Execute requests without external tools

---

## 🎨 Swagger UI Features

- **Filter**: Search for endpoints
- **Try it out**: Execute requests
- **Schema**: View request/response models
- **Examples**: See example values
- **Download**: Export OpenAPI spec

---

## 🔗 Alternative Access Points

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Swagger UI (alternative)**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/api-docs.yaml`

---

## 💡 Tips

1. **Use "Try it out"** to test endpoints without cURL
2. **Check "Schema"** to see request/response structures
3. **View "Example Value"** for sample payloads
4. **Use filters** to quickly find endpoints
5. **Download OpenAPI spec** for API clients

---

**Happy Documenting! 📚**

