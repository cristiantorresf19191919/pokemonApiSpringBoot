# Pokemon Backend

A Kotlin Spring Boot backend application with WebFlux and GraphQL for managing Pokemon data.

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
- MongoDB (Reactive)
- PokeAPI integration
- **Gradle 8.11.1** (Latest stable)

## Features

- **Authentication**: Simple username/password authentication (admin/admin) via REST
- **Pokemon Operations**: All Pokemon operations via GraphQL
  - Get Pokemon by ID
  - Get paginated Pokemon list with cursor-based pagination
  - Sort by name or number
- **GraphQL API**: Full GraphQL support with schema for all Pokemon business logic
- **REST API**: RESTful endpoint for authentication only

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

```graphql
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

mutation {
  login(username: "admin", password: "admin") {
    success
    token
    message
  }
}
```

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

2. Edit `application-local.yml` and add your local MongoDB connection string and JWT secret:
   ```yaml
   spring:
     data:
       mongodb:
         uri: mongodb+srv://username:password@cluster.mongodb.net/database
   jwt:
     secret: YourLocalJWTSecretKeyForDevelopmentMustBeAtLeast256BitsLongForSecurity
   ```

**Note:** `application-local.yml` is gitignored and will not be committed to the repository.

### Production (Azure Deployment)

The application uses environment variables for production configuration. Set the following environment variables in your Azure App Service:

**Required Environment Variables:**
- `MONGODB_URI` - Your MongoDB connection string (e.g., `mongodb+srv://username:password@cluster.mongodb.net/database?retryWrites=true&w=majority`)
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
   az webapp config appsettings set --resource-group <resource-group-name> --name <app-name> --settings MONGODB_URI="mongodb+srv://..." JWT_SECRET="your-secret-key"
   ```

3. **Azure DevOps Pipeline:**
   ```yaml
   - task: AzureWebApp@1
     inputs:
       appSettings: |
         [
           {
             "name": "MONGODB_URI",
             "value": "$(MONGODB_URI)",
             "slotSetting": false
           },
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
- MongoDB Reactive Driver
- MockK for testing
- Reactor Test for reactive testing

