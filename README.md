# 📝 Development Prompts

This section documents the incremental prompts used to build this project step by step.

---

## Prompt 1: Project Setup and Dependencies

```
I need help setting up a Spring Boot project. I already created a Kotlin Spring Boot app with Gradle 8.11 and Java 21, but I need to configure it properly for a production-ready backend.

I want to use:
- Spring WebFlux for async/concurrent handling (reactive programming)
- Spring Web for REST endpoints  
- GraphQL (graphql-java-kickstart) for GraphQL API
- Spring Security for JWT token authentication
- Jackson Kotlin module for JSON
- Reactor Kotlin extensions
- SpringDoc OpenAPI for Swagger docs
- JWT libraries (jjwt) for tokens
- Bucket4j for rate limiting

Can you help me set up the build.gradle.kts with all the right dependencies? Make sure versions are compatible with Spring Boot 3.3.x and Kotlin 2.0.21. Thanks!
```

---

## Prompt 2: Clean Architecture Scaffolding
```
Now that I have the dependencies, I need help creating the project structure. I want to follow Clean Architecture and DDD principles.

I'm thinking of organizing it like this:

**Domain layer** (com.pokemon.domain):
- model/ - business entities (Pokemon, Ability, Move, Form, etc.)
- repository/ - just interfaces, no implementations
- service/ - domain service interfaces (PaginationService, AuthenticationService, JwtTokenService)

**Application layer** (com.pokemon.application):
- dto/ - DTOs for data transfer
- mapper/ - mappers between domain and DTOs
- service/ - application services that orchestrate business logic

**Infrastructure layer** (com.pokemon.infrastructure):
- client/ - external API clients (PokeAPI using WebClient)
- repository/ - repository implementations
- service/ - infrastructure service implementations
- mapper/ - mappers from API responses to domain
- config/ - configuration classes
- security/ - security config and filters

**Presentation layer** (com.pokemon.presentation):
- graphql/ - GraphQL resolvers
- rest/ - REST controllers (just for auth)
- dto/ - presentation DTOs

Can you help me scaffold this structure and explain how dependency injection connects these layers? I want to make sure domain has no dependencies on other layers, and everything flows correctly.
```

---

## Prompt 3: GraphQL Schema and Resolvers
```
Alright, I have my GraphQL schema ready. I need help implementing the resolvers and setting up the GraphQL endpoint.

Here's my schema:

```graphql
type Query {
  pokemon(id: Int!): Pokemon
  pokemons(first: Int, after: String, sortBy: String): PokemonConnection!
  searchPokemon(query: String!): [PokemonPreview!]!
}

type Mutation {
  login(username: String!, password: String!): AuthPayload!
}

type Pokemon {
  id: Int!
  name: String!
  number: Int!
  imageUrl: String!
  abilities: [Ability!]!
  moves: [Move!]!
  forms: [Form!]!
}

type PokemonConnection {
  edges: [PokemonEdge!]!
  pageInfo: PageInfo!
  totalCount: Int!
}

type PokemonPreview {
  id: Int!
  name: String!
  number: Int!
  imageUrl: String!
}
```

I need:
1. Create the schema.graphqls file in resources
2. Implement PokemonQueryResolver with pokemon(), pokemons(), and searchPokemon() methods
3. Implement AuthMutationResolver with login()
4. Configure GraphQL router to handle POST at /graphql
5. Make sure everything uses reactive programming (Mono/Flux) and returns CompletableFuture for GraphQL compatibility

Can you help me set this up?
```

---

## Prompt 4: Cursor-Based Pagination
```
I need to implement cursor-based pagination for the pokemons query. Here's what I'm thinking:

1. **Cursor encoding/decoding**: Use Base64 to encode offsets into opaque cursors
   - Implement PaginationService interface with encodeCursor() and decodeCursor() methods

2. **Pagination logic**:
   - Accept `first` param (items per page, default 20)
   - Accept `after` param (cursor for pagination)
   - Accept `sortBy` param ("name" or "number", default "number")
   - Return PokemonConnection with edges, pageInfo, and totalCount

3. **Implementation details**:
   - Sort the entire Pokemon list in-memory (not just current page)
   - Calculate offset from cursor: offset = decodeCursor(after) + 1
   - Slice the sorted list based on offset and limit
   - Generate cursors for each edge
   - Calculate hasNextPage and hasPreviousPage

4. **Domain models**:
   - Create Page<Pokemon> domain model
   - Create Edge<Pokemon> with node and cursor
   - Create PageInfo with pagination metadata

Can you help me implement this? I want it to work globally across all Pokemon, not just the current page.
```

---

## Prompt 5: JWT Authentication and Security
```
I need to implement JWT authentication and security. Here's what I need:

1. **JWT Token Service**:
   - Implement JwtTokenService interface
   - Methods: generateToken(), validateToken(), getUsernameFromToken()
   - Use HS256 algorithm
   - Configurable expiration (default 24 hours)
   - Secret key from environment variable

2. **Authentication Service**:
   - Implement AuthenticationService interface
   - authenticate() method - validate credentials (hardcoded admin/admin for now)
   - generateToken() - generate JWT after auth

3. **Security Configuration**:
   - Configure Spring Security for WebFlux
   - Create SecurityConfig that allows public access to /api/login, /swagger-ui, /api-docs, /graphql-playground
   - Add JwtAuthenticationFilter as WebFilter to validate tokens
   - Add RateLimitingFilter for rate limiting

4. **REST and GraphQL Auth**:
   - Create AuthController at /api/login (REST)
   - Create AuthMutationResolver for login mutation (GraphQL)
   - Both return AuthPayload with success, token, message

Can you help me implement this with proper error handling?
```

---

## Prompt 6: PokeAPI Integration and Caching
```
I need to integrate with PokeAPI and implement a smart caching strategy. Here's my plan:

1. **PokeAPI Client**:
   - Create PokeApiClient using Spring WebClient (reactive)
   - Methods: getPokemonList(), getPokemonById(), getPokemonByUrl()
   - Configure base URL via application properties
   - Add retry logic (2 retries with exponential backoff for 5xx errors)

2. **Caching Service**:
   - Create PokemonCacheService with:
     - In-memory index (lightweight list loaded on startup)
     - Details cache (ConcurrentHashMap for full Pokemon details)
   - On startup (@PostConstruct):
     - Fetch all Pokemon list (limit=10000) to build index
     - Pre-fetch first 100 Pokemon details in parallel using WebFlux
   - Methods:
     - getPage() - get paginated index items (synchronous, in-memory)
     - getDetails() - get Pokemon details (check cache first, then API with fallback)
     - search() - search Pokemon by name (synchronous, in-memory)

3. **Pre-fetching Strategy**:
   - After loading index, pre-fetch Pokemon details using Flux.flatMap for parallel processing
   - Use fetchPokemonDetailsWithFallback() method that:
     - Tries getPokemonById() first with retry logic
     - Falls back to getPokemonByUrl() if ID fetch fails
   - Pre-fetch runs asynchronously (non-blocking) so server starts quickly
   - Errors during pre-fetch are logged but don't block startup

4. **Fallback Logic**:
   - If getPokemonById fails, check if Pokemon exists in index
   - If found, use URL from index to fetch via getPokemonByUrl
   - Cache successful fetches

Can you help me implement this with proper error handling and reactive patterns?
```

---

## Prompt 7: DTOs and Mappers
```
I need to create all the DTOs and mappers. Here's what I need:

1. **Application DTOs** (com.pokemon.application.dto):
   - PokemonDTO - full Pokemon with abilities, moves, forms
   - PokemonPreviewDTO - lightweight preview (id, name, number, imageUrl)
   - PageDTO<T> - generic pagination DTO
   - EdgeDTO<T> - generic edge DTO
   - PageInfoDTO - pagination metadata
   - AbilityDTO, MoveDTO, FormDTO - nested DTOs

2. **Presentation DTOs** (com.pokemon.presentation.dto):
   - GraphQLRequest - GraphQL request wrapper
   - AuthPayload - auth response

3. **Mappers**:
   - PokemonMapper (Application layer):
     - toDTO(pokemon: Pokemon): PokemonDTO
     - toDTO(page: Page<Pokemon>): PageDTO<PokemonDTO>
   - PokemonDomainMapper (Infrastructure layer):
     - PokeApiPokemonResponse.toDomain(): Pokemon

4. **Mapping Strategy**:
   - Use extension functions for clean mapping
   - Handle null values properly
   - Map nested objects correctly

Can you help me create all of these with proper null safety?
```

---

## Prompt 8: Service Layer Orchestration
```
I need to implement the service layer that orchestrates everything. Here's what I need:

1. **PokemonService** (Application layer):
   - getPokemonById(id: Int): Mono<PokemonDTO> - get single Pokemon, map to DTO
   - getPokemons(first, after, sortBy): Mono<PageDTO<PokemonDTO>>:
     - Get page slice from cache service (synchronous)
     - Fetch details for items in parallel using Flux
     - Handle errors gracefully (log and continue with other items)
     - Map to DTO and construct pagination response
   - searchPokemon(query: String): Flux<PokemonPreviewDTO>:
     - Use in-memory search (no API calls)
     - Map index items to preview DTOs
     - Generate image URLs statically

2. **Error Handling**:
   - Use retry logic for transient errors (5xx server errors)
   - Use onErrorResume to handle individual item failures in pagination
   - Log errors but don't fail entire requests
   - Return empty results gracefully when needed

3. **Performance Optimization**:
   - Use parallel processing with Flux.flatMap for fetching multiple Pokemon
   - Cache results to avoid redundant API calls
   - Use in-memory operations for search and pagination

Can you help me implement this with reactive programming patterns?
```

---

## Prompt 9: Fallback Mechanism
```
Quick question - I noticed that when I search for Pokemon I can get results, but when I try to fetch by ID it sometimes fails with a 500 error from PokeAPI. Can you help me add a fallback mechanism? 

If getPokemonById fails, I want to:
- Check if the Pokemon exists in the index
- If found, use the URL from the index to fetch via getPokemonByUrl as a fallback
- No need for retries on the fallback, just try once
- Cache the result if successful

This should help handle cases where the direct ID endpoint fails but the URL endpoint works.
```

---

## Prompt 10: Adding Number Field to Search
```
One more thing - I want to make sure the searchPokemon query returns the number field. Currently it only returns id, name, and imageUrl. Can you help me add the number field to:
1. PokemonPreviewDTO
2. The GraphQL schema (PokemonPreview type)
3. The searchPokemon service method

The number should be the same as the id in this case (based on how the domain model maps it).
```

---

# 🚀 Pokemon Backend

A Kotlin Spring Boot backend application with WebFlux and GraphQL for managing Pokemon data from PokeAPI.

---

## 📑 Table of Contents

- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [Features](#-features)
- [API Endpoints](#-api-endpoints)
- [Key Architectural Solutions](#-key-architectural-solutions)
- [Configuration](#-configuration)
- [Running the Application](#-running-the-application)
- [Project Structure](#-project-structure)
- [Testing](#-testing)
- [Deployment](#-azure-deployment)

---

## 🏗️ Architecture

This project follows Clean Architecture principles with the following layers:

- **Domain Layer**: Core business entities and interfaces
- **Application Layer**: Use cases, services, and DTOs
- **Infrastructure Layer**: External API clients, repositories, and implementations
- **Presentation Layer**: GraphQL resolvers and REST controllers

## 🛠️ Technology Stack

- **Java 21** (LTS)
- **Kotlin 2.0.21** (Latest stable)
- **Spring Boot 3.3.5** (Latest stable)
- Spring WebFlux (Reactive)
- GraphQL
- PokeAPI integration
- **Gradle 8.11.1** (Latest stable)

## ✨ Features

- **🔐 Authentication**: JWT-based authentication (admin/admin) via REST
- **🎮 Pokemon Operations**: All Pokemon operations via GraphQL
  - Get Pokemon by ID
  - Get paginated Pokemon list with cursor-based pagination
  - **Global sorting** by name or number (works across all Pokemon, not just current page)
- **📊 GraphQL API**: Full GraphQL support with schema for all Pokemon business logic
- **🌐 REST API**: RESTful endpoint for authentication only
- **🛡️ Security**: JWT token protection for all GraphQL Pokemon queries
- **⚡ Performance**: In-memory caching and optimized data fetching to prevent N+1 queries

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

## 📄 Cursor-Based Pagination

The application implements a cursor-based pagination system using Base64-encoded offsets. This provides:

- Efficient pagination for large datasets
- Stable pagination even when data changes
- GraphQL-friendly pagination pattern

## ⚙️ Configuration

### 🏠 Local Development

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

### ☁️ Production (Azure Deployment)

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

## ▶️ Running the Application

```bash
./gradlew bootRun
```

## 🧪 Running Tests

```bash
./gradlew test
```

## 📁 Project Structure

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

## 🧪 Testing

The project includes comprehensive unit tests for:
- Domain services
- Application services
- Infrastructure components
- Presentation layer (resolvers and controllers)

## 📦 Dependencies

- Spring Boot WebFlux for reactive programming
- GraphQL Java Tools for GraphQL support
- MockK for testing
- Reactor Test for reactive testing

---

## 🚢 Azure Deployment

```bash
# Create buildx builder
docker buildx create --use

# Build and push multi-platform image
docker buildx build --platform linux/amd64 -t cristiantorres19/pokemon-backend:latest --push .
```