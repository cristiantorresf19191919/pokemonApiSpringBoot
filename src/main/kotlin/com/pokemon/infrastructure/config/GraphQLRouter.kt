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
                    </style>
                </head>
                <body>
                    <div id="graphiql">Loading GraphiQL...</div>
                    <script crossorigin src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
                    <script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
                    <script crossorigin src="https://unpkg.com/graphiql@3/graphiql.min.js"></script>
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
                            
                            // Default query
                            const defaultQuery = '# Welcome to Pokemon GraphQL API\\n#\\n# Headers Example (click Headers tab):\\n# {\\n#   "Authorization": "Bearer paste_your_token_here"\\n# }\\n\\nquery {\\n  # Start typing to see available queries\\n}';
                            
                            // Default headers with token example
                            const defaultHeaders = {
                                'Authorization': 'Bearer paste_your_token_here'
                            };
                            
                            // Create fetcher with header support
                            const fetcherWithHeaders = async (graphQLParams, opts) => {
                                let requestHeaders = {
                                    'Content-Type': 'application/json'
                                };
                                
                                // Get headers from GraphiQL's header editor if available
                                if (opts && opts.headers) {
                                    Object.assign(requestHeaders, opts.headers);
                                    // Remove placeholder token if it hasn't been replaced
                                    if (requestHeaders.Authorization && requestHeaders.Authorization.includes('paste_your_token_here')) {
                                        delete requestHeaders.Authorization;
                                    }
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
                            
                            // Simple GraphiQL component
                            const root = ReactDOM.createRoot(graphiqlElement);
                            root.render(React.createElement(GraphiQLComponent, {
                                fetcher: fetcherWithHeaders,
                                defaultQuery: defaultQuery,
                                defaultHeaders: defaultHeaders
                            }));
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
