# GraphQL Playground - Complete Examples Guide

This guide contains all GraphQL queries and mutations for testing your Pokemon backend.

**Access GraphQL Playground:** `http://localhost:8080/graphql-playground`

---

## 🔐 Authentication Flow

### 1. Login (Success)

**Mutation:**
```graphql
mutation {
  login(username: "admin", password: "admin") {
    success
    token
    message
  }
}
```

**Expected Response:**
```json
{
  "data": {
    "login": {
      "success": true,
      "token": "YWRtaW46YWRtaW4=",
      "message": "Login successful"
    }
  }
}
```

### 2. Login (Failure - Wrong Credentials)

**Mutation:**
```graphql
mutation {
  login(username: "wrong", password: "wrong") {
    success
    token
    message
  }
}
```

**Expected Response:**
```json
{
  "data": {
    "login": {
      "success": false,
      "token": null,
      "message": "Invalid credentials"
    }
  }
}
```

### 3. Login (Failure - Wrong Username)

**Mutation:**
```graphql
mutation {
  login(username: "user", password: "admin") {
    success
    token
    message
  }
}
```

### 4. Login (Failure - Wrong Password)

**Mutation:**
```graphql
mutation {
  login(username: "admin", password: "password") {
    success
    token
    message
  }
}
```

---

## 🎮 Pokemon Queries

### 1. Get Pokemon by ID (Minimal Fields)

**Query:**
```graphql
query {
  pokemon(id: 1) {
    id
    name
    number
  }
}
```

**Expected Response:**
```json
{
  "data": {
    "pokemon": {
      "id": 1,
      "name": "bulbasaur",
      "number": 1
    }
  }
}
```

### 2. Get Pokemon by ID (All Fields)

**Query:**
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
```

**Expected Response:**
```json
{
  "data": {
    "pokemon": {
      "id": 1,
      "name": "bulbasaur",
      "number": 1,
      "imageUrl": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png",
      "abilities": [
        {
          "name": "overgrow",
          "isHidden": false
        },
        {
          "name": "chlorophyll",
          "isHidden": true
        }
      ],
      "moves": [
        {
          "name": "razor-wind",
          "levelLearnedAt": null
        },
        {
          "name": "swords-dance",
          "levelLearnedAt": null
        }
      ],
      "forms": [
        {
          "name": "bulbasaur",
          "url": "https://pokeapi.co/api/v2/pokemon-form/1/"
        }
      ]
    }
  }
}
```

### 3. Get Pokemon by ID (Only Abilities)

**Query:**
```graphql
query {
  pokemon(id: 25) {
    id
    name
    abilities {
      name
      isHidden
    }
  }
}
```

### 4. Get Pokemon by ID (Only Moves)

**Query:**
```graphql
query {
  pokemon(id: 25) {
    id
    name
    moves {
      name
      levelLearnedAt
    }
  }
}
```

### 5. Get Multiple Pokemons (Different IDs)

**Query:**
```graphql
query {
  pikachu: pokemon(id: 25) {
    id
    name
    number
    imageUrl
  }
  charizard: pokemon(id: 6) {
    id
    name
    number
    imageUrl
  }
  blastoise: pokemon(id: 9) {
    id
    name
    number
    imageUrl
  }
}
```

---

## 📄 Pagination Queries

### 1. Get First Page (Default - Sorted by Number)

**Query:**
```graphql
query {
  pokemons(first: 10) {
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
```

**Expected Response:**
```json
{
  "data": {
    "pokemons": {
      "edges": [
        {
          "node": {
            "id": 1,
            "name": "bulbasaur",
            "number": 1,
            "imageUrl": "..."
          },
          "cursor": "Y3Vyc29yMA=="
        }
      ],
      "pageInfo": {
        "hasNextPage": true,
        "hasPreviousPage": false,
        "startCursor": "Y3Vyc29yMA==",
        "endCursor": "Y3Vyc29yOQ=="
      },
      "totalCount": 1000
    }
  }
}
```

### 2. Get First Page (Sorted by Name)

**Query:**
```graphql
query {
  pokemons(first: 10, sortBy: "name") {
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
```

### 3. Get First Page (Sorted by Number)

**Query:**
```graphql
query {
  pokemons(first: 10, sortBy: "number") {
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
```

### 4. Get Next Page (Using Cursor)

**Query:**
```graphql
query {
  pokemons(first: 10, after: "Y3Vyc29yOQ==", sortBy: "name") {
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
```

**Note:** Replace `"Y3Vyc29yOQ=="` with the `endCursor` from the previous query.

### 5. Get Large Page (20 Items)

**Query:**
```graphql
query {
  pokemons(first: 20, sortBy: "name") {
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
```

### 6. Get Paginated Pokemons with Full Details

**Query:**
```graphql
query {
  pokemons(first: 5, sortBy: "name") {
    edges {
      node {
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
```

---

## 🔄 Complete Business Flow Examples

### Flow 1: Authentication → Get Pokemon

**Step 1: Login**
```graphql
mutation {
  login(username: "admin", password: "admin") {
    success
    token
    message
  }
}
```

**Step 2: Get Pokemon (using token in headers if needed)**
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

### Flow 2: Browse Pokemons with Pagination

**Step 1: Get First Page**
```graphql
query {
  pokemons(first: 10, sortBy: "name") {
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
      endCursor
    }
    totalCount
  }
}
```

**Step 2: Get Next Page (use endCursor from Step 1)**
```graphql
query {
  pokemons(first: 10, after: "Y3Vyc29yOQ==", sortBy: "name") {
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
      endCursor
    }
    totalCount
  }
}
```

**Step 3: Get Specific Pokemon from Results**
```graphql
query {
  pokemon(id: 25) {
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
  }
}
```

### Flow 3: Search and Explore

**Step 1: Get Pokemons Sorted by Number**
```graphql
query {
  pokemons(first: 20, sortBy: "number") {
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
      endCursor
    }
  }
}
```

**Step 2: Get Pokemons Sorted by Name**
```graphql
query {
  pokemons(first: 20, sortBy: "name") {
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
      endCursor
    }
  }
}
```

**Step 3: Get Detailed Info for Specific Pokemon**
```graphql
query {
  pokemon(id: 25) {
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
```

---

## 🎯 Advanced Examples

### 1. Multiple Queries in One Request

**Query:**
```graphql
query {
  firstPokemon: pokemon(id: 1) {
    id
    name
    number
  }
  secondPokemon: pokemon(id: 25) {
    id
    name
    number
  }
  pokemonsList: pokemons(first: 5, sortBy: "name") {
    edges {
      node {
        id
        name
      }
    }
    totalCount
  }
}
```

### 2. Query with Variables

**Query:**
```graphql
query GetPokemon($pokemonId: Int!) {
  pokemon(id: $pokemonId) {
    id
    name
    number
    imageUrl
  }
}
```

**Variables:**
```json
{
  "pokemonId": 25
}
```

### 3. Pagination with Variables

**Query:**
```graphql
query GetPokemons($first: Int, $after: String, $sortBy: String) {
  pokemons(first: $first, after: $after, sortBy: $sortBy) {
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
```

**Variables:**
```json
{
  "first": 10,
  "after": null,
  "sortBy": "name"
}
```

### 4. Login Mutation with Variables

**Mutation:**
```graphql
mutation Login($username: String!, $password: String!) {
  login(username: $username, password: $password) {
    success
    token
    message
  }
}
```

**Variables:**
```json
{
  "username": "admin",
  "password": "admin"
}
```

---

## 📊 Testing Different Pokemon IDs

### Popular Pokemons to Test

```graphql
# Pikachu
query {
  pokemon(id: 25) {
    id
    name
    number
    imageUrl
  }
}

# Charizard
query {
  pokemon(id: 6) {
    id
    name
    number
    imageUrl
  }
}

# Blastoise
query {
  pokemon(id: 9) {
    id
    name
    number
    imageUrl
  }
}

# Mewtwo
query {
  pokemon(id: 150) {
    id
    name
    number
    imageUrl
  }
}
```

---

## 🔍 Field Selection Examples

### Minimal Selection
```graphql
query {
  pokemon(id: 1) {
    id
    name
  }
}
```

### Medium Selection
```graphql
query {
  pokemon(id: 1) {
    id
    name
    number
    imageUrl
    abilities {
      name
    }
  }
}
```

### Full Selection
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
```

---

## 🚀 Quick Start Testing Sequence

Copy and paste these in order in GraphQL Playground:

### 1. Test Login
```graphql
mutation {
  login(username: "admin", password: "admin") {
    success
    token
    message
  }
}
```

### 2. Test Get Single Pokemon
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

### 3. Test Pagination
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
      endCursor
    }
    totalCount
  }
}
```

### 4. Test Next Page (use endCursor from step 3)
```graphql
query {
  pokemons(first: 10, after: "Y3Vyc29yOQ==", sortBy: "name") {
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
      endCursor
    }
    totalCount
  }
}
```

---

## 💡 Tips for GraphQL Playground

1. **Use Variables:** Click "Query Variables" panel at bottom to use variables
2. **Auto-complete:** Press `Ctrl+Space` (or `Cmd+Space` on Mac) for suggestions
3. **Prettify:** Click the "Prettify" button to format your query
4. **History:** Previous queries are saved in the history panel
5. **Docs:** Click "Docs" in the right sidebar to explore the schema

---

## 🎨 Example Response Structures

### Login Response
```json
{
  "data": {
    "login": {
      "success": true,
      "token": "YWRtaW46YWRtaW4=",
      "message": "Login successful"
    }
  }
}
```

### Pokemon Response
```json
{
  "data": {
    "pokemon": {
      "id": 1,
      "name": "bulbasaur",
      "number": 1,
      "imageUrl": "https://...",
      "abilities": [...],
      "moves": [...],
      "forms": [...]
    }
  }
}
```

### Pagination Response
```json
{
  "data": {
    "pokemons": {
      "edges": [
        {
          "node": {...},
          "cursor": "..."
        }
      ],
      "pageInfo": {
        "hasNextPage": true,
        "hasPreviousPage": false,
        "startCursor": "...",
        "endCursor": "..."
      },
      "totalCount": 1000
    }
  }
}
```

---

**Happy Querying! 🎮**

