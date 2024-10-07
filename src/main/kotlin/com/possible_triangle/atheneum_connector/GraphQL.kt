package com.possible_triangle.atheneum_connector

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.possible_triangle.atheneum_connector.AtheneumConnector.configure
import java.net.URL

object GraphQL {

    private val client = GraphQLKtorClient(
        URL("http://localhost:8080/api/graphql"),
        serializer = GraphQLClientKotlinxSerializer { configure() }
    )

    suspend fun <T : Any> query(query: GraphQLClientRequest<T>): T {
        val response = client.execute(query)
        return response.data ?: throw GraphQLException(response.errors ?: emptyList())
    }

}