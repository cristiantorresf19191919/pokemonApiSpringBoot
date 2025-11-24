package com.pokemon.infrastructure.config

import com.pokemon.presentation.dto.GraphQLRequest
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.kickstart.tools.SchemaParser
import graphql.schema.GraphQLSchema
import java.nio.charset.StandardCharsets
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Configuration
class GraphQLRouter(
        private val queryResolvers: List<graphql.kickstart.tools.GraphQLQueryResolver>,
        private val mutationResolvers: List<graphql.kickstart.tools.GraphQLMutationResolver>
) {

    private fun executeGraphQLQuery(
            graphQL: GraphQL,
            query: String,
            variables: Map<String, Any>,
            operationName: String?
    ): Mono<ServerResponse> {
        val executionInput =
                ExecutionInput.newExecutionInput()
                        .query(query)
                        .variables(variables)
                        .operationName(operationName)
                        .build()

        // Wrap blocking GraphQL.execute() call in Mono.fromCallable to avoid blocking
        // the event loop
        return Mono.fromCallable { graphQL.execute(executionInput) }
                .subscribeOn(Schedulers.boundedElastic())
                .map { executionResult ->
                    // Convert errors to serializable format (avoid complex exception
                    // objects)
                    val serializableErrors =
                            executionResult.errors.map { error ->
                                val errorMap =
                                        mutableMapOf<String, Any?>("message" to error.message)

                                // Build extensions map
                                val extensions = mutableMapOf<String, Any?>()

                                // Add error type if available
                                if (error.errorType != null) {
                                    extensions["code"] = error.errorType.toString()
                                }

                                // Add existing extensions if they exist and are
                                // serializable
                                if (error.extensions != null && error.extensions.isNotEmpty()) {
                                    error.extensions.forEach { (key, value) ->
                                        // Only include simple serializable types
                                        if (value is String ||
                                                        value is Number ||
                                                        value is Boolean ||
                                                        value == null
                                        ) {
                                            extensions[key.toString()] = value
                                        }
                                    }
                                }

                                if (extensions.isNotEmpty()) {
                                    errorMap["extensions"] = extensions
                                }

                                errorMap
                            }

                    mapOf(
                            "data" to executionResult.getData(),
                            "errors" to
                                    (serializableErrors.takeIf { it.isNotEmpty() } ?: emptyList())
                    )
                }
                .flatMap { response ->
                    ServerResponse.ok()
                            .header("Content-Type", "application/json")
                            .bodyValue(response)
                }
    }

    @Bean
    fun graphQLSchema(): GraphQLSchema {
        val schemaContent =
                ClassPathResource("graphql/schema.graphqls")
                        .inputStream
                        .bufferedReader(StandardCharsets.UTF_8)
                        .use { it.readText() }

        return SchemaParser.newParser()
                .schemaString(schemaContent)
                .resolvers(queryResolvers + mutationResolvers)
                .build()
                .makeExecutableSchema()
    }

    @Bean
    fun graphQL(graphQLSchema: GraphQLSchema): GraphQL {
        // Enable introspection (required for Apollo Client and other tools to fetch schema)
        // Introspection is enabled by default, but we make it explicit
        return GraphQL.newGraphQL(graphQLSchema).build()
    }

    @Bean
    fun graphQLRoutes(graphQL: GraphQL): RouterFunction<ServerResponse> {
        return router {
            // Support both GET and POST for GraphQL (GET is useful for schema introspection)
            GET("/graphql") { request ->
                // Handle GET requests (typically used for schema introspection by Apollo Client)
                // Query parameters are automatically URL-decoded by Spring
                val queryParam = request.queryParam("query").orElse("")
                val operationName = request.queryParam("operationName").orElse(null)
                val variablesParam = request.queryParam("variables").orElse("{}")

                // Parse variables JSON string (Apollo Client may send variables as JSON string)
                val variables =
                        try {
                            if (variablesParam.isNotEmpty() && variablesParam != "{}") {
                                com.fasterxml.jackson.databind.ObjectMapper()
                                        .readValue(
                                                variablesParam,
                                                object :
                                                        com.fasterxml.jackson.core.type.TypeReference<
                                                                Map<String, Any>>() {}
                                        )
                            } else {
                                emptyMap<String, Any>()
                            }
                        } catch (e: Exception) {
                            // If parsing fails, use empty map (variables are optional)
                            emptyMap<String, Any>()
                        }

                if (queryParam.isEmpty()) {
                    return@GET ServerResponse.badRequest()
                            .header("Content-Type", "application/json")
                            .bodyValue(
                                    mapOf(
                                            "errors" to
                                                    listOf(
                                                            mapOf(
                                                                    "message" to
                                                                            "Query parameter is required"
                                                            )
                                                    )
                                    )
                            )
                }

                executeGraphQLQuery(graphQL, queryParam, variables, operationName)
            }
            POST("/graphql") { request ->
                request.bodyToMono(GraphQLRequest::class.java).flatMap { graphQLRequest ->
                    executeGraphQLQuery(
                            graphQL,
                            graphQLRequest.query,
                            graphQLRequest.variables ?: emptyMap(),
                            graphQLRequest.operationName
                    )
                }
            }
            GET("/graphql-playground") {
                val html =
                        """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>GraphQL Playground - Pokemon API</title>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <link rel="stylesheet" href="https://unpkg.com/graphiql@3/graphiql.min.css" />
                    <link rel="stylesheet" href="https://unpkg.com/@graphiql/plugin-explorer@0.1.0/dist/style.css" />
                    <style>
                        body {
                            margin: 0;
                            height: 100vh;
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
                        }
                        #graphiql {
                            height: 100vh;
                        }
                        .example-queries {
                            position: absolute;
                            top: 10px;
                            right: 10px;
                            z-index: 1000;
                            background: #1e1e1e;
                            border: 1px solid #3d3d3d;
                            border-radius: 4px;
                            padding: 8px;
                            color: #fff;
                            font-size: 12px;
                        }
                        .example-queries select {
                            background: #2d2d2d;
                            color: #fff;
                            border: 1px solid #3d3d3d;
                            border-radius: 4px;
                            padding: 6px 10px;
                            font-size: 12px;
                            cursor: pointer;
                            min-width: 200px;
                        }
                        .example-queries select:hover {
                            background: #3d3d3d;
                        }
                        .example-queries select option {
                            background: #2d2d2d;
                            color: #fff;
                        }
                        .example-queries label {
                            display: block;
                            margin-bottom: 4px;
                            font-weight: 500;
                        }
                    </style>
                </head>
                <body>
                    <div class="example-queries">
                        <label for="query-select">📚 Example Queries:</label>
                        <select id="query-select">
                            <option value="">Select an example query...</option>
                            <option value="pokemonById">🎮 Get Pokemon by ID (Full)</option>
                            <option value="pokemonByIdMinimal">🎮 Get Pokemon by ID (Minimal)</option>
                            <option value="pokemonsFirstPage">📋 Get Pokemons - First Page (15)</option>
                            <option value="pokemonsSortedByName">🔤 Get Pokemons - Sorted by Name</option>
                            <option value="pokemonsSortedByNumber">🔢 Get Pokemons - Sorted by Number</option>
                        </select>
                    </div>
                    <div id="graphiql">Loading GraphiQL...</div>
                    <script crossorigin src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
                    <script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
                    <script crossorigin src="https://unpkg.com/graphiql@3/graphiql.min.js"></script>
                    <script>
                        // Define example queries globally with header instructions
                        window.exampleQueries = {
                            pokemonById: '# Headers Example:\\n# {\\n#   "Authorization": "Bearer paste_your_token_here"\\n# }\\n\\nquery { pokemon(id: 1) { id name number imageUrl abilities { name isHidden } moves { name levelLearnedAt } forms { name url } } }',
                            pokemonByIdMinimal: '# Headers Example:\\n# {\\n#   "Authorization": "Bearer paste_your_token_here"\\n# }\\n\\nquery { pokemon(id: 1) { id name number } }',
                            pokemonsFirstPage: '# Headers Example:\\n# {\\n#   "Authorization": "Bearer paste_your_token_here"\\n# }\\n\\nquery { pokemons(first: 15) { edges { node { id name number imageUrl } cursor } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } totalCount } }',
                            pokemonsSortedByName: '# Headers Example:\\n# {\\n#   "Authorization": "Bearer paste_your_token_here"\\n# }\\n\\nquery { pokemons(first: 10, sortBy: "name") { edges { node { id name number imageUrl } cursor } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } totalCount } }',
                            pokemonsSortedByNumber: '# Headers Example:\\n# {\\n#   "Authorization": "Bearer paste_your_token_here"\\n# }\\n\\nquery { pokemons(first: 20, sortBy: "number") { edges { node { id name number imageUrl } cursor } pageInfo { hasNextPage hasPreviousPage startCursor endCursor } totalCount } }'
                        };
                        
                        // Function to load example query into GraphiQL
                        window.loadExampleQuery = function(queryName) {
                            if (!queryName || !window.exampleQueries || !window.exampleQueries[queryName]) {
                                return;
                            }
                            const query = window.exampleQueries[queryName];
                            
                            // Use the updateQuery function if available
                            if (window.updateGraphiQLQuery) {
                                window.updateGraphiQLQuery(query);
                            } else {
                                // Fallback: Try to access the editor directly
                                setTimeout(() => {
                                const cmEditor = document.querySelector('.graphiql-query-editor .cm-editor');
                                    if (cmEditor && cmEditor.__cm) {
                                        const view = cmEditor.__cm;
                                        if (view.dispatch) {
                                        view.dispatch({
                                            changes: {
                                                from: 0,
                                                to: view.state.doc.length,
                                                insert: query
                                            }
                                        });
                                        }
                                    }
                                }, 100);
                            }
                        };
                        
                        // Attach event listener to dropdown
                        document.addEventListener('DOMContentLoaded', function() {
                            const select = document.getElementById('query-select');
                            if (select) {
                                select.addEventListener('change', function(e) {
                                    e.preventDefault();
                                    const queryName = this.value;
                                    if (queryName) {
                                        window.loadExampleQuery(queryName);
                                    }
                                    // Reset dropdown after selection
                                    setTimeout(() => {
                                        this.value = '';
                                    }, 100);
                                });
                            }
                        });
                    </script>
                    <script>
                        // Wait for all scripts to load before initializing
                        function waitForGraphiQL() {
                            if (typeof React === 'undefined' || typeof ReactDOM === 'undefined' || typeof GraphiQL === 'undefined') {
                                // Retry after a short delay
                                setTimeout(waitForGraphiQL, 100);
                                return;
                            }
                            
                            try {
                                initializeGraphiQL();
                            } catch (error) {
                                console.error('Error initializing GraphiQL:', error);
                                const errorDiv = document.getElementById('graphiql');
                                if (errorDiv) {
                                    errorDiv.innerHTML = '<div style="padding: 20px; color: red;">Error initializing GraphiQL: ' + error.message + '<br><br>Check browser console for details.</div>';
                                }
                            }
                        }
                        
                        // Start waiting when DOM is ready
                        if (document.readyState === 'loading') {
                            document.addEventListener('DOMContentLoaded', waitForGraphiQL);
                        } else {
                            // DOM already loaded, wait a bit for scripts
                            setTimeout(waitForGraphiQL, 100);
                        }
                        
                        function initializeGraphiQL() {
                            const graphiqlElement = document.getElementById('graphiql');
                            if (!graphiqlElement) {
                                throw new Error('GraphiQL container element not found');
                            }
                            
                            // Check if GraphiQL is available
                            if (!GraphiQL) {
                                throw new Error('GraphiQL library not loaded correctly.');
                            }
                            
                            // Create a simple fetcher function (no authentication needed)
                            const fetcher = async (graphQLParams) => {
                                try {
                                    const response = await fetch('/graphql', {
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/json'
                                        },
                                        body: JSON.stringify(graphQLParams)
                                    });
                                    
                                    if (!response.ok) {
                                        throw new Error('HTTP error! status: ' + response.status);
                                    }
                                    
                                    return await response.json();
                                } catch (error) {
                                    return {
                                        data: null,
                                        errors: [{
                                            message: error.message || 'An error occurred while executing the query'
                                        }]
                                    };
                                }
                            };
                            
                            // Find GraphiQL component
                            let GraphiQLComponent = null;
                            if (GraphiQL.GraphiQL) {
                                GraphiQLComponent = GraphiQL.GraphiQL;
                            } else if (typeof GraphiQL === 'function') {
                                GraphiQLComponent = GraphiQL;
                            } else if (GraphiQL.default) {
                                GraphiQLComponent = GraphiQL.default;
                            } else {
                                throw new Error('Could not find GraphiQL component. Available keys: ' + Object.keys(GraphiQL).join(', '));
                            }
                            
                            // Default query with header instructions
                            const defaultQuery = '# Welcome to Pokemon GraphQL API\\n# Select an example query from the dropdown above, or write your own.\\n#\\n# Headers Example (click Headers tab above):\\n# {\\n#   "Authorization": "Bearer paste_your_token_here"\\n# }\\n\\nquery {\\n  # Start typing to see available queries\\n}';
                            
                            // Default headers with token example
                            const defaultHeaders = JSON.stringify({
                                'Authorization': 'Bearer paste_your_token_here'
                            }, null, 2);
                            
                            // Create a wrapper component with state to manage query and headers
                            function GraphiQLWrapper() {
                                const [query, setQuery] = React.useState(defaultQuery);
                                const [headers, setHeaders] = React.useState(defaultHeaders);
                                
                                // Expose update function globally when component mounts
                                React.useEffect(() => {
                                    window.updateGraphiQLQuery = function(newQuery) {
                                        setQuery(newQuery);
                                    };
                                    return function() {
                                        delete window.updateGraphiQLQuery;
                                    };
                                }, []);
                                
                                // Create fetcher that includes headers from the editor
                                const fetcherWithHeaders = async (graphQLParams, opts) => {
                                    let requestHeaders = {
                                        'Content-Type': 'application/json'
                                    };
                                    
                                    // Get headers from GraphiQL's header editor if available
                                    let headersToUse = headers;
                                    if (opts && opts.headers) {
                                        headersToUse = JSON.stringify(opts.headers, null, 2);
                                    }
                                    
                                    // Parse headers from the editor
                                    try {
                                        const parsedHeaders = JSON.parse(headersToUse);
                                        Object.assign(requestHeaders, parsedHeaders);
                                        // Remove placeholder token if it hasn't been replaced
                                        if (requestHeaders.Authorization && requestHeaders.Authorization.includes('paste_your_token_here')) {
                                            delete requestHeaders.Authorization;
                                        }
                                    } catch (e) {
                                        // If headers aren't valid JSON, just use default
                                    }
                                    
                                    try {
                                        const response = await fetch('/graphql', {
                                            method: 'POST',
                                            headers: requestHeaders,
                                            body: JSON.stringify(graphQLParams)
                                        });
                                        
                                        if (!response.ok) {
                                            throw new Error('HTTP error! status: ' + response.status);
                                        }
                                        
                                        return await response.json();
                                    } catch (error) {
                                        return {
                                            data: null,
                                            errors: [{
                                                message: error.message || 'An error occurred while executing the query'
                                            }]
                                        };
                                    }
                                };
                                
                                // Parse default headers object for GraphiQL
                                let defaultHeadersObj = {};
                                try {
                                    defaultHeadersObj = JSON.parse(headers);
                                } catch (e) {
                                    // Use empty object if parsing fails
                                }
                                
                                return React.createElement(GraphiQLComponent, {
                                    fetcher: fetcherWithHeaders,
                                    query: query,
                                    onEditQuery: setQuery,
                                    headers: defaultHeadersObj,
                                    onEditHeaders: function(newHeaders) {
                                        setHeaders(JSON.stringify(newHeaders, null, 2));
                                    },
                                    shouldPersistHeaders: false
                                });
                            }
                            
                            const root = ReactDOM.createRoot(graphiqlElement);
                            root.render(React.createElement(GraphiQLWrapper));
                            
                            // Store references
                            window.graphiqlRoot = root;
                            window.graphiqlElement = graphiqlElement;
                        }
                    </script>
                </body>
                </html>
                """.trimIndent()
                ServerResponse.ok()
                        .header("Content-Type", "text/html; charset=utf-8")
                        .bodyValue(html)
            }
        }
    }
}
