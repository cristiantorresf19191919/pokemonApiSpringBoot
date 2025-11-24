# Security Implementation

## Overview

The Pokemon Backend implements JWT-based authentication and rate limiting to protect GraphQL Pokemon endpoints.

## Authentication Flow

### 1. Login (REST Endpoint)

**Endpoint:** `POST /api/login`

**Request:**
```json
{
  "username": "admin",
  "password": "admin"
}
```

**Response (Success):**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Login successful"
}
```

**Response (Failure):**
```json
{
  "success": false,
  "token": null,
  "message": "Invalid credentials"
}
```

**Status Codes:**
- `200 OK` - Login successful
- `401 UNAUTHORIZED` - Invalid credentials

### 2. Using the Token

After successful login, include the JWT token in the `Authorization` header for GraphQL requests:

```
Authorization: Bearer <your-jwt-token>
```

### 3. GraphQL Authentication

All Pokemon GraphQL queries and mutations require a valid JWT token:

**Protected Operations:**
- `pokemon(id: Int!)` - Get Pokemon by ID
- `pokemons(first: Int, after: String, sortBy: String)` - Get paginated Pokemon list

**Unprotected Operations:**
- `login(username: String!, password: String!)` - Authentication mutation (no token required)

**Example with cURL:**
```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "query": "query { pokemon(id: 1) { id name number } }"
  }'
```

**Example with GraphQL Playground:**

1. Login via REST endpoint to get token
2. Open browser console in GraphQL Playground
3. Run: `setAuthToken('YOUR_JWT_TOKEN')`
4. Refresh the page
5. Token will be automatically included in requests

## Rate Limiting

### Configuration

- **Limit:** 100 requests per minute per IP address
- **Scope:** GraphQL endpoint only (`/graphql`)
- **Headers:**
  - `X-RateLimit-Remaining` - Number of requests remaining
  - `X-RateLimit-Retry-After` - Seconds to wait before retry (when limit exceeded)

### Rate Limit Exceeded Response

**Status Code:** `429 TOO MANY REQUESTS`

**Response:**
```json
{
  "errors": [{
    "message": "Rate limit exceeded. Please try again later.",
    "extensions": {
      "code": "RATE_LIMIT_EXCEEDED",
      "httpStatus": 429
    }
  }]
}
```

## Invalid Token Response

**Status Code:** `401 UNAUTHORIZED`

**Response:**
```json
{
  "errors": [{
    "message": "Unauthorized: Invalid or missing token",
    "extensions": {
      "code": "UNAUTHENTICATED",
      "httpStatus": 401
    }
  }]
}
```

## Credentials

**Valid Credentials:**
- Username: `admin`
- Password: `admin`

**Important:** Any other combination is considered invalid.

## JWT Token Configuration

- **Algorithm:** HS256 (HMAC SHA-256)
- **Expiration:** 24 hours (configurable via `jwt.expiration` in `application.yml`)
- **Secret Key:** Configurable via `jwt.secret` in `application.yml`

### Configuration in `application.yml`:

```yaml
jwt:
  secret: YourSecretKeyMustBeAtLeast256BitsLongForSecurity
  expiration: 86400000 # 24 hours in milliseconds
```

## Security Features

✅ **JWT Token Authentication** - Secure token-based authentication
✅ **Token Validation** - Automatic validation on protected endpoints
✅ **Rate Limiting** - Protection against abuse (100 req/min per IP)
✅ **Proper HTTP Status Codes** - 401 for unauthorized, 429 for rate limit
✅ **GraphQL Error Format** - Standard GraphQL error responses

## Testing Authentication

### 1. Test Login

```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }'
```

### 2. Test Protected Query (Without Token)

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { pokemon(id: 1) { id name } }"
  }'
```

**Expected:** `401 UNAUTHORIZED`

### 3. Test Protected Query (With Token)

```bash
# First, get token from login
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

# Then use token for GraphQL query
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "query": "query { pokemon(id: 1) { id name } }"
  }'
```

**Expected:** `200 OK` with Pokemon data

## Security Best Practices

1. **Never commit JWT secret to version control** - Use environment variables or secrets management
2. **Use HTTPS in production** - JWT tokens should only be transmitted over encrypted connections
3. **Rotate secrets regularly** - Change JWT secret periodically
4. **Monitor rate limits** - Adjust limits based on usage patterns
5. **Implement token refresh** - Consider refresh tokens for long-lived sessions
6. **Log authentication failures** - Monitor for suspicious activity

## Troubleshooting

### Token Not Working

1. **Check token expiration** - Tokens expire after 24 hours
2. **Verify Authorization header format** - Must be `Bearer <token>`
3. **Check token validity** - Token must be generated by the same secret key

### Rate Limit Issues

1. **Check X-RateLimit-Remaining header** - See how many requests remain
2. **Wait for reset** - Rate limit resets every minute
3. **Use different IP** - Rate limiting is per IP address

### 401 Unauthorized

1. **Verify credentials** - Only `admin/admin` is valid
2. **Check token format** - Must be valid JWT
3. **Ensure token is included** - Check Authorization header

