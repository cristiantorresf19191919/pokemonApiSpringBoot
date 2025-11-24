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

@Configuration
class GraphQLRouter(
        private val queryResolvers: List<graphql.kickstart.tools.GraphQLQueryResolver>,
        private val mutationResolvers: List<graphql.kickstart.tools.GraphQLMutationResolver>
) {

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
        return GraphQL.newGraphQL(graphQLSchema).build()
    }

    @Bean
    fun graphQLRoutes(graphQL: GraphQL): RouterFunction<ServerResponse> {
        return router {
            POST("/graphql") { request ->
                request.bodyToMono(GraphQLRequest::class.java).flatMap { graphQLRequest ->
                    val executionInput =
                            ExecutionInput.newExecutionInput()
                                    .query(graphQLRequest.query)
                                    .variables(graphQLRequest.variables ?: emptyMap())
                                    .operationName(graphQLRequest.operationName)
                                    .build()

                    val executionResult = graphQL.execute(executionInput)

                    val response =
                            mapOf(
                                    "data" to executionResult.getData(),
                                    "errors" to
                                            (executionResult.errors.takeIf { it.isNotEmpty() }
                                                    ?: emptyList())
                            )

                    ServerResponse.ok()
                            .header("Content-Type", "application/json")
                            .bodyValue(response)
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
                    <script src="https://unpkg.com/graphiql@3/graphiql.min.js"></script>
                    <script>
                        const root = ReactDOM.createRoot(document.getElementById('graphiql'));
                        const fetcher = GraphiQL.createFetcher({
                            url: '/graphql',
                            headers: () => {
                                const token = localStorage.getItem('authToken');
                                return token ? { 'Authorization': 'Bearer ' + token } : {};
                            }
                        });
                        root.render(React.createElement(GraphiQL, { fetcher }));
                        
                        // Helper function to set token (can be called from browser console)
                        window.setAuthToken = function(token) {
                            localStorage.setItem('authToken', token);
                            location.reload();
                        };
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
