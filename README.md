# Pokemon Backend

A Kotlin Spring Boot backend application with WebFlux and GraphQL for managing Pokemon data from PokeAPI.

## Architecture

This project follows Clean Architecture principles with the following layers:

- **Domain Layer**: Core business entities and interfaces
- **Application Layer**: Use cases, services, and DTOs
- **Infrastructure Layer**: External API clients, repositories, and implementations
- **Presentation Layer**: GraphQL resolvers and REST controllers

## Technology Stack

- **Java 21** (LTS)
- **Kotlin 2.0.21** (Latest stable)
- **Spring Boot 3.3.5** (Latest stable)
- Spring WebFlux (Reactive)
- GraphQL
- PokeAPI integration
- **Gradle 8.11.1** (Latest stable)

## Features

- **Authentication**: JWT-based authentication (admin/admin) via REST
- **Pokemon Operations**: All Pokemon operations via GraphQL
  - Get Pokemon by ID
  - Get paginated Pokemon list with cursor-based pagination
  - **Global sorting** by name or number (works across all Pokemon, not just current page)
- **GraphQL API**: Full GraphQL support with schema for all Pokemon business logic
- **REST API**: RESTful endpoint for authentication only
- **Security**: JWT token protection for all GraphQL Pokemon queries
- **Performance**: In-memory caching and optimized data fetching to prevent N+1 queries

## API Endpoints

### REST Endpoints

- `POST /api/login` - Authenticate user

**📚 Swagger Documentation:** `http://localhost:8080/swagger-ui.html` (Authentication endpoints only)
**📖 OpenAPI Spec:** `http://localhost:8080/api-docs`

### GraphQL Endpoint

- `POST /graphql` - GraphQL endpoint
- `GET /graphql-playground` - GraphQL Playground UI

**📚 Complete GraphQL Examples:** See [GRAPHQL_EXAMPLES.md](./GRAPHQL_EXAMPLES.md) for all query examples
**⚡ Quick Reference:** See [GRAPHQL_QUICK_REFERENCE.md](./GRAPHQL_QUICK_REFERENCE.md) for copy-paste queries

### GraphQL Queries

**Note:** All Pokemon queries require JWT authentication. See [Security](#-security-jwt-token-protection) section.

```graphql
# Get single Pokemon (requires token)
query {
  pokemon(id: 1) {
    id
    name
    number
    imageUrl
    abilities {
      name
      isHidden
    }
    moves {
      name
      levelLearnedAt
    }
    forms {
      name
      url
    }
  }
}

# Get paginated list with global sorting (requires token)
query {
  pokemons(first: 20, after: "cursor", sortBy: "name") {
    edges {
      node {
        id
        name
        number
        imageUrl
      }
      cursor
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
    totalCount
  }
}

# Login (no token required)
mutation {
  login(username: "admin", password: "admin") {
    success
    token
    message
  }
}
```

**📚 Complete GraphQL Examples:** See [GRAPHQL_EXAMPLES.md](./GRAPHQL_EXAMPLES.md)  
**⚡ Quick Reference:** See [GRAPHQL_QUICK_REFERENCE.md](./GRAPHQL_QUICK_REFERENCE.md)

## Key Architectural Solutions

### 🔐 Security: JWT Token Protection

**Problem:** GraphQL endpoints need protection from unauthorized access.

**Solution:** All Pokemon GraphQL queries require a valid JWT token in the `Authorization: Bearer <token>` header. The `login` mutation is the only unprotected endpoint.

**Implementation:**
- JWT tokens are generated on successful login via REST endpoint
- `JwtAuthenticationFilter` intercepts `/graphql` requests and validates tokens
- Invalid or missing tokens return `401 UNAUTHORIZED` with GraphQL error format
- Token can be passed via header or query parameter (for GraphQL Playground)

**Usage:**
```bash
# 1. Login to get token
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# 2. Use token for GraphQL queries
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"query":"query { pokemon(id: 1) { id name } }"}'
```

### ⚡ Performance: N+1 Problem Solution

**Problem:** Fetching a page of 20 Pokemon required 21 HTTP requests (1 for list + 20 for details), causing slow performance.

**Solution:** In-memory indexing with smart caching strategy.

**Implementation:**
- `PokemonCacheService` loads all Pokemon IDs and names on startup (lightweight index)
- When fetching a page, we first get the slice from in-memory index (instant)
- Then fetch details only for the 20 items on that page (parallel requests)
- Previously fetched Pokemon are cached in `ConcurrentHashMap` for instant subsequent access

**Result:** 
- First page load: 20 API calls (only for current page)
- Subsequent page loads: 0-20 calls (cached items return instantly)
- Sorting/pagination: 0ms (in-memory operations)

### 🎯 Global Sorting Fix

**Problem:** Sorting only worked within the current page. "Sort by Name" would sort Bulbasaur, Ivysaur, etc., but wouldn't bring "Abra" to page 1 (since Abra is on page 3 of the API's ID-based list).

**Solution:** In-memory global sorting before pagination.

**Implementation:**
- All ~1300 Pokemon are indexed in memory on startup
- When sorting is requested, the entire list is sorted globally
- Pagination happens after sorting, ensuring correct results
- Example: "Sort by Name" now shows "Abomasnow" on page 1, not just items from API page 1

**Result:** True global sorting that works across all Pokemon, not just the current page.

### 💾 Caching Strategy

**Two-Level Caching:**

1. **Index Cache (Startup):** Lightweight list of all Pokemon (ID, name, URL) loaded once
2. **Details Cache (Runtime):** `ConcurrentHashMap` storing full Pokemon details after first fetch

**Benefits:**
- Sorting and pagination are instant (in-memory operations)
- Previously viewed Pokemon load instantly from cache
- Reduces external API calls significantly
- Thread-safe with `ConcurrentHashMap`

## Cursor-Based Pagination

The application implements a cursor-based pagination system using Base64-encoded offsets. This provides:

- Efficient pagination for large datasets
- Stable pagination even when data changes
- GraphQL-friendly pagination pattern

## Configuration

### Local Development

1. Copy the example local configuration file:
   ```bash
   cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
   ```

2. Edit `application-local.yml` and add your JWT secret:
   ```yaml
   jwt:
     secret: YourLocalJWTSecretKeyForDevelopmentMustBeAtLeast256BitsLongForSecurity
   ```

**Note:** `application-local.yml` is gitignored and will not be committed to the repository.

### Production (Azure Deployment)

The application uses environment variables for production configuration. Set the following environment variables in your Azure App Service:

**Required Environment Variables:**
- `JWT_SECRET` - Your JWT secret key (must be at least 256 bits long for security)

**Optional Environment Variables:**
- `SERVER_PORT` - Server port (default: 8080)
- `POKEAPI_BASE_URL` - PokeAPI base URL (default: https://pokeapi.co/api/v2)
- `LOG_LEVEL_ROOT` - Root log level (default: INFO)
- `LOG_LEVEL_POKEMON` - Application log level (default: DEBUG)
- `JWT_EXPIRATION` - JWT token expiration in milliseconds (default: 86400000 = 24 hours)

**Setting Environment Variables in Azure:**

1. **Azure Portal:**
   - Go to your App Service → Configuration → Application settings
   - Click "+ New application setting"
   - Add each environment variable with its value
   - Click "Save"

2. **Azure CLI:**
   ```bash
   az webapp config appsettings set --resource-group <resource-group-name> --name <app-name> --settings JWT_SECRET="your-secret-key"
   ```

3. **Azure DevOps Pipeline:**
   ```yaml
   - task: AzureWebApp@1
     inputs:
       appSettings: |
         [
           {
             "name": "JWT_SECRET",
             "value": "$(JWT_SECRET)",
             "slotSetting": false
           }
         ]
   ```

## Running the Application

```bash
./gradlew bootRun
```

## Running Tests

```bash
./gradlew test
```

## Project Structure

```
src/
├── main/
│   ├── kotlin/com/pokemon/
│   │   ├── domain/           # Domain layer
│   │   ├── application/      # Application layer
│   │   ├── infrastructure/   # Infrastructure layer
│   │   └── presentation/    # Presentation layer
│   └── resources/
│       ├── application.yml
│       └── graphql/
│           └── schema.graphqls
└── test/                      # Unit tests
```

## Testing

The project includes comprehensive unit tests for:
- Domain services
- Application services
- Infrastructure components
- Presentation layer (resolvers and controllers)

## Dependencies

- Spring Boot WebFlux for reactive programming
- GraphQL Java Tools for GraphQL support
- MockK for testing
- Reactor Test for reactive testing

