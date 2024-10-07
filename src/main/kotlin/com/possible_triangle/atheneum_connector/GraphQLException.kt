package com.possible_triangle.atheneum_connector

import com.expediagroup.graphql.client.types.GraphQLClientError

class GraphQLException(val errors: List<GraphQLClientError>) : RuntimeException(errors.joinToString(", ") {
    it.message
})