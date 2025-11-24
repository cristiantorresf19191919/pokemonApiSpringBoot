# GraphQL Quick Reference - Copy & Paste Ready

Quick copy-paste queries for GraphQL Playground.

---

## 🔐 Login

```graphql
mutation {
  login(username: "admin", password: "admin") {
    success
    token
    message
  }
}
```

---

## 🎮 Get Single Pokemon

### Minimal
```graphql
query {
  pokemon(id: 1) {
    id
    name
    number
  }
}
```

### Full Details
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

## 📄 Pagination

### First Page (Sorted by Name)
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

### Next Page (Replace cursor)
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

### Sorted by Number
```graphql
query {
  pokemons(first: 10, sortBy: "number") {
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

## 🔄 Complete Flow Example

### Step 1: Login
```graphql
mutation {
  login(username: "admin", password: "admin") {
    success
    token
  }
}
```

### Step 2: Browse Pokemons
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
      endCursor
    }
    totalCount
  }
}
```

### Step 3: Get Specific Pokemon Details
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

---

## 🎯 Popular Pokemon IDs to Test

- **1** - Bulbasaur
- **4** - Charmander
- **7** - Squirtle
- **25** - Pikachu
- **150** - Mewtwo
- **151** - Mew

---

**Access Playground:** `http://localhost:8080/graphql-playground`

